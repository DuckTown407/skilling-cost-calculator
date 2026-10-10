package com.skillingcost;

import com.google.gson.Gson;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalculatorFlowTest
{
    private static final Instant NOW = Instant.ofEpochSecond(1000);
    private static final CostCalculator.TaxSettings TAX = new CostCalculator.TaxSettings(200, 5_000_000, 50);

    private static final class Schedule implements EstimateScheduler
    {
        Runnable action;
        boolean pending;
        int stops;
        public void restart() { pending = true; }
        public void stop() { pending = false; stops++; }
        void fire() { if (pending) { pending = false; action.run(); } }
    }

    private static final class Controller implements CalculatorController
    {
        final List<IntConsumer> xp = new ArrayList<>();
        final List<CalculationRequest> requests = new ArrayList<>();
        final List<Consumer<Map<String, CostResult>>> successes = new ArrayList<>();
        final List<Consumer<String>> failures = new ArrayList<>();
        public void loadCurrentSkillXp(Skill skill, IntConsumer callback) { xp.add(callback); }
        public void estimate(CalculationRequest request, Consumer<Map<String, CostResult>> success, Consumer<String> failure)
        {
            requests.add(request); successes.add(success); failures.add(failure);
        }
        void succeed(int index)
        {
            CalculationRequest request = requests.get(index);
            Map<Integer, WikiPrice> prices = new HashMap<>();
            for (int id : request.priceIds()) { prices.put(id, new WikiPrice(100L, 99L, 900L, 800L)); }
            PriceSnapshot snapshot = new PriceSnapshot(prices, NOW);
            Map<String, CostResult> results = new HashMap<>();
            for (SkillingMethod method : request.methods)
            {
                results.put(method.getId(), CostCalculator.calculate(method, request.currentLevel, request.currentXp,
                    request.targetLevel, request.targetXp, request.optional, request.sell, request.tax,
                    request.batchSize, snapshot, TAX, NOW));
            }
            successes.get(index).accept(results);
        }
    }

    private SkillingCostPanel panel(Controller controller, Schedule schedule)
    {
        return new SkillingCostPanel(controller, new MethodRepository(new Gson()), action -> {
            schedule.action = action;
            return schedule;
        });
    }

    @Test
    public void goalsPreserveExplicitTargetsAndLevel99()
    {
        CalculatorSession state = new CalculatorSession();
        CalculatorSession.Goal goal = state.goal(Skill.COOKING);
        assertEquals(Experience.getXpForLevel(2), goal.targetXp);
        goal.current(Experience.getXpForLevel(50), true);
        assertEquals(Experience.getXpForLevel(51), goal.targetXp);
        goal.target(1_000_000);
        goal.current(Experience.getXpForLevel(60), true);
        assertEquals(1_000_000, goal.targetXp);
        assertSame(goal, state.goal(Skill.COOKING));
        CalculatorSession.Goal maximum = state.goal(Skill.MAGIC);
        maximum.current(200_000_000, true);
        assertEquals(200_000_000, maximum.targetXp);
        assertFalse(maximum.applyLoaded(0, maximum.xpEditRevision - 1));
        assertFalse(maximum.applyLoaded(-1, maximum.xpEditRevision));
    }

    @Test
    public void coalescingManualTextAndLateXp()
        throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            panel.currentXp.setValue(100);
            panel.currentXp.setValue(200);
            panel.targetXp.setValue(1000);
            assertEquals(0, controller.requests.size());
            schedule.fire();
            assertEquals(1, controller.requests.size());
            assertEquals(200, controller.requests.get(0).currentXp);
            ((JSpinner.DefaultEditor) panel.currentXp.getEditor()).getTextField().setText("invalid");
            controller.xp.get(0).accept(5000);
            assertEquals(200, panel.session.goal(Skill.SMITHING).currentXp);
            assertFalse(schedule.pending);
            ((JSpinner.DefaultEditor) panel.currentXp.getEditor()).getTextField().setText("300");
            schedule.fire();
            assertEquals(300, controller.requests.get(1).currentXp);
            assertEquals(1000, controller.requests.get(1).targetXp);
            panel.dispose();
            assertFalse(schedule.pending);
        });
    }

    @Test
    public void globalOptionsStayEditableAcrossSkillsAndEmptySelections()
        throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            panel.targetXp.setValue(10000);
            panel.sell.doClick();
            panel.search.setText("iron");
            panel.skillSelect.setSelectedItem(Skill.FIREMAKING);
            assertTrue(panel.optional.isEnabled());
            assertTrue(panel.sell.isEnabled());
            assertTrue(panel.tax.isEnabled());
            assertFalse(panel.session.sell);
            // Preferences can be changed even when this recipe has no outputs or optional inputs.
            panel.tax.doClick();
            panel.optional.doClick();
            panel.search.setText("NO MATCH EXISTS");
            assertEquals(0, panel.rows.size());
            assertTrue(panel.optional.isEnabled());
            assertTrue(panel.sell.isEnabled());
            assertTrue(panel.tax.isEnabled());
            panel.sell.doClick();
            panel.skillSelect.setSelectedItem(Skill.SMITHING);
            assertEquals(10000, panel.session.goal(Skill.SMITHING).targetXp);
            assertEquals("iron", panel.search.getText());
            assertFalse(panel.optional.isSelected());
            assertTrue(panel.sell.isSelected());
            assertFalse(panel.tax.isSelected());
            schedule.fire();
            CalculationRequest request = controller.requests.get(0);
            assertFalse(request.optional);
            assertTrue(request.sell);
            assertFalse(request.tax);
            controller.xp.get(0).accept(90000);
            assertEquals(0, panel.session.goal(Skill.SMITHING).currentXp);
            panel.dispose();
        });
    }

    @Test
    public void selectedMethodVariantAndTargetSurviveSkillChangesSortAndXpReload() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            panel.skillSelect.setSelectedItem(Skill.PRAYER);
            panel.currentLevel.setValue(99);
            panel.targetXp.setValue(20_000_000);
            panel.search.setText("dragon");
            for (int index = 0; index < panel.rows.size(); index++)
            {
                if (panel.rows.get(index).hasVariants()) { panel.list.setSelectedIndex(index); break; }
            }
            assertTrue(panel.variants.getItemCount() > 1);
            panel.variants.setSelectedIndex(panel.variants.getItemCount() - 1);
            String selectedId = panel.list.getSelectedValue().getSelected().getId();
            panel.skillSelect.setSelectedItem(Skill.COOKING);
            panel.skillSelect.setSelectedItem(Skill.PRAYER);
            assertEquals("dragon", panel.search.getText());
            assertEquals(20_000_000, panel.targetXp.getValue());
            assertEquals(selectedId, panel.list.getSelectedValue().getSelected().getId());
            assertEquals(selectedId, ((SkillingMethod) panel.variants.getSelectedItem()).getId());
            panel.sortSelect.setSelectedItem(SkillingCostPanel.MethodSort.MOST_PROFITABLE);
            schedule.fire();
            controller.succeed(0);
            assertEquals(selectedId, panel.renderer.displayedResult().getMethod().getId());
            panel.loadXp.doClick();
            controller.xp.get(controller.xp.size() - 1).accept(14_000_000);
            assertEquals(20_000_000, panel.targetXp.getValue());
            assertEquals(selectedId, panel.list.getSelectedValue().getSelected().getId());
            panel.dispose();
        });
    }

    @Test
    public void obsoleteResponsesCannotChangePanelAfterAnyInputOrDisposal()
        throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            schedule.fire();
            panel.sortSelect.setSelectedItem(SkillingCostPanel.MethodSort.MOST_PROFITABLE);
            controller.succeed(0);
            assertNull(panel.renderer.displayedResult());
            schedule.fire();
            controller.succeed(1);
            CostResult baseline = panel.renderer.displayedResult();
            assertNotNull(baseline);
            panel.targetXp.setValue(2000); schedule.fire();
            panel.tax.doClick(); schedule.fire();
            panel.search.setText("steel"); schedule.fire();
            panel.hideLocked.doClick(); schedule.fire();
            controller.succeed(2);
            controller.succeed(3);
            controller.failures.get(4).accept("OLD FAILURE");
            assertSame(baseline, panel.renderer.displayedResult());
            assertFalse(panel.renderer.statusText().contains("OLD FAILURE"));
            panel.dispose();
            controller.succeed(controller.requests.size() - 1);
            assertSame(baseline, panel.renderer.displayedResult());
            SkillingCostPanel replacement = panel(controller, new Schedule());
            assertNull(replacement.renderer.displayedResult());
            replacement.dispose();
        });
    }

    @Test
    public void filteringAndSelectedVariantRankingUseIdenticalResults()
        throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            panel.skillSelect.setSelectedItem(Skill.PRAYER);
            panel.currentLevel.setValue(40);
            panel.search.setText("superior");
            panel.hideLocked.doClick();
            for (int i = 0; i < panel.rows.size(); i++)
            {
                for (SkillingMethod method : panel.rows.get(i).getVariants()) { assertTrue(method.getRequiredLevel() <= 40); }
            }
            assertEquals(0, panel.rows.size());
            panel.search.setText("dragon");
            assertTrue(panel.rows.size() > 0);
            panel.sortSelect.setSelectedItem(SkillingCostPanel.MethodSort.MOST_PROFITABLE);
            schedule.fire();
            CalculationRequest request = controller.requests.get(0);
            controller.succeed(0);
            assertEquals(request.selectedId, panel.list.getSelectedValue().getSelected().getId());
            assertEquals(request.selectedId, panel.renderer.displayedResult().getMethod().getId());
            assertEquals(request.currentLevel, panel.renderer.displayedResult().getCurrentLevel());
            assertEquals(request.methods.size(), request.methods.stream().map(SkillingMethod::getId).distinct().count());
            panel.refresh.doClick();
            assertTrue(controller.requests.get(1).refresh);
            Instant oldFetch = panel.renderer.displayedResult().getSnapshotTime();
            controller.failures.get(1).accept("Wiki request failed");
            assertEquals(oldFetch, panel.renderer.displayedResult().getSnapshotTime());
            assertTrue(panel.renderer.statusText().contains("previous snapshot"));
            panel.dispose();
        });
    }

    @Test
    public void zeroWorkNeedsNoQuotesAndTaxSettingsAreChecked()
    {
        MethodRepository repository = new MethodRepository(new Gson());
        SkillingMethod method = repository.getMethodsBySkill().get(Skill.SMITHING).get(0);
        CalculationRequest request = new CalculationRequest(1, Collections.singletonList(method), method.getId(),
            100, 100, 2, 2, 28, true, true, true, false);
        assertTrue(request.priceIds().isEmpty());
        assertTrue(CostCalculator.calculate(method, 2, 100, 2, 100, true, true, true, 28,
            new PriceSnapshot(Collections.emptyMap(), Instant.EPOCH), TAX, NOW).isAvailable());
        CostResult invalid = CostCalculator.calculate(method, 2, 0, 2, 100, true, true, true, 28,
            new PriceSnapshot(Collections.emptyMap(), NOW), new CostCalculator.TaxSettings(-1, 5, 50), NOW);
        assertEquals(CostResult.State.INVALID_SETTINGS, invalid.getState());
        ItemQuantity exempt = new ItemQuantity(1, "exempt", java.math.BigDecimal.ONE,
            ItemQuantity.Pricing.GE, 0, 1, false, true);
        SkillingMethod exemptMethod = new SkillingMethod(Skill.MAGIC, "exempt", null, null, 1, 1,
            Collections.emptyList(), Collections.emptyList(), Collections.singletonList(exempt));
        CostResult result = CostCalculator.calculate(exemptMethod, 1, 0, 1, 2, true, true, true, 28,
            new PriceSnapshot(Collections.singletonMap(1, new WikiPrice(99L, 99L, 900L, 900L)), NOW), TAX, NOW);
        assertEquals(198, result.getOutputGross());
        assertEquals(0, result.getTax());
    }

    @Test
    public void xpBoundariesLoggedOutLoadsAndPendingBatchEdits() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            controller.xp.get(0).accept(-1);
            assertTrue(panel.renderer.statusText().contains("Log in"));
            panel.currentXp.setValue(200_000_000);
            assertEquals(99, panel.currentLevel.getValue());
            assertEquals(200_000_000, panel.targetXp.getValue());
            panel.targetXp.setValue(12345);
            panel.loadXp.doClick();
            controller.xp.get(1).accept(50000);
            assertEquals(12345, panel.targetXp.getValue());
            assertEquals(CalculatorSession.level(50000), panel.currentLevel.getValue());
            panel.currentLevel.setValue(1);
            assertEquals(0, panel.currentXp.getValue());
            ((JSpinner.DefaultEditor) panel.currentXp.getEditor()).getTextField().setText("200000001");
            assertFalse(schedule.pending);
            ((JSpinner.DefaultEditor) panel.currentXp.getEditor()).getTextField().setText("83");
            assertTrue(schedule.pending);
            assertEquals(2, panel.currentLevel.getValue());
            ((JSpinner.DefaultEditor) panel.batchSize.getEditor()).getTextField().setText("0");
            assertFalse(schedule.pending);
            ((JSpinner.DefaultEditor) panel.batchSize.getEditor()).getTextField().setText("17");
            schedule.fire();
            assertEquals(17, controller.requests.get(0).batchSize);
            panel.search.setText("NO MATCH EXISTS");
            assertEquals(0, panel.rows.size());
            assertTrue(panel.renderer.statusText().contains("No matching"));
            panel.dispose();
        });
    }

    @Test
    public void everyOptionCombinationMatchesTheSelectedComparison() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Controller controller = new Controller(); Schedule schedule = new Schedule();
            SkillingCostPanel panel = panel(controller, schedule);
            panel.skillSelect.setSelectedItem(Skill.CRAFTING);
            panel.currentLevel.setValue(99);
            panel.targetXp.setValue(200_000_000);
            panel.search.setText("green");
            panel.sortSelect.setSelectedItem(SkillingCostPanel.MethodSort.MOST_PROFITABLE);
            for (int mask = 0; mask < 8; mask++)
            {
                if (panel.optional.isSelected() != ((mask & 1) != 0)) { panel.optional.doClick(); }
                if (panel.sell.isSelected() != ((mask & 2) != 0)) { panel.sell.doClick(); }
                if (panel.tax.isSelected() != ((mask & 4) != 0)) { panel.tax.doClick(); }
                schedule.fire();
                int index = controller.requests.size() - 1;
                CalculationRequest request = controller.requests.get(index);
                controller.succeed(index);
                CostResult rendered = panel.renderer.displayedResult();
                assertEquals(request.selectedId, panel.list.getSelectedValue().getSelected().getId());
                assertEquals(request.selectedId, rendered.getMethod().getId());
                assertEquals(request.optional, rendered.isIncludeOptionalInputs());
                assertEquals(request.sell, rendered.isSellOutputs());
                Map<Integer, WikiPrice> prices = new HashMap<>();
                for (int id : request.priceIds()) { prices.put(id, new WikiPrice(100L, 99L, 900L, 800L)); }
                CostResult direct = CostCalculator.calculate(rendered.getMethod(), request.currentLevel, request.currentXp,
                    request.targetLevel, request.targetXp, request.optional, request.sell, request.tax, request.batchSize,
                    new PriceSnapshot(prices, NOW), TAX, NOW);
                assertEquals(direct.getNet(), rendered.getNet());
                assertEquals(direct.getRequiredInputLines(), rendered.getRequiredInputLines());
                assertEquals(direct.getOutputLines(), rendered.getOutputLines());
            }
            panel.dispose();
        });
    }
}
