package com.skillingcost;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResultRendererTest
{
    private final Instant now = Instant.ofEpochSecond(1000);
    private final CostCalculator.TaxSettings tax = new CostCalculator.TaxSettings(200, 5_000_000, 50);

    private CostResult estimate(WikiPrice quote)
    {
        SkillingMethod method = new SkillingMethod(Skill.MAGIC, "Fixture production", null, null, 1, 1,
            Collections.emptyList(), Collections.emptyList(), Collections.singletonList(new ItemQuantity(1, "output", 1)));
        return CostCalculator.calculate(method, 1, 0, 1, 1, true, true, true, 28,
            new PriceSnapshot(quote == null ? Collections.emptyMap() : Collections.singletonMap(1, quote), now), tax, now);
    }

    private List<Component> descendants(Container parent)
    {
        List<Component> components = new ArrayList<>();
        for (Component component : parent.getComponents())
        {
            components.add(component);
            if (component instanceof Container) { components.addAll(descendants((Container) component)); }
        }
        return components;
    }

    private void layout(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }

    @Test
    public void trillionSummaryFitsAndExpansionSurvivesStatusChanges() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ResultRenderer renderer = new ResultRenderer();
            CostResult result = estimate(new WikiPrice(2_149_631_130_647L, 2_149_631_130_647L, 900L, 900L));
            renderer.showResult(result);
            renderer.getComponent().setSize(new Dimension(210, 380));
            layout(renderer.getComponent());
            List<Component> components = descendants(renderer.getComponent());
            assertTrue(components.stream().anyMatch(component -> component instanceof JLabel
                && ((JLabel) component).getText().contains("GP/XP")));
            JLabel compact = (JLabel) components.stream().filter(component -> component instanceof JLabel
                && ((JLabel) component).getText().contains("2.15T")).findFirst().get();
            assertTrue("Trillion summary clipped", compact.getFontMetrics(compact.getFont()).stringWidth("2.15T gp") <= compact.getWidth());
            JToggleButton expand = (JToggleButton) components.stream().filter(component -> component instanceof JToggleButton).findFirst().get();
            expand.doClick();
            renderer.showStatus("Loading…");
            assertSame(result, renderer.displayedResult());
            assertTrue(expand.isSelected());
            renderer.showFailure("Wiki request failed");
            assertSame(result, renderer.displayedResult());
            assertTrue(renderer.statusText().contains("previous snapshot"));
            renderer.showResult(result);
            assertTrue(expand.isSelected());
            layout(renderer.getComponent());
            // Isolated Swing fixture only: no client, game state or game input.
            BufferedImage preview = new BufferedImage(210, 380, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = preview.createGraphics();
            renderer.getComponent().paint(graphics);
            graphics.dispose();
            try
            {
                File directory = new File("build/reports/tests");
                assertTrue(directory.isDirectory() || directory.mkdirs());
                ImageIO.write(preview, "png", new File(directory, "sidebar-fixture.png"));
            }
            catch (java.io.IOException exception) { throw new AssertionError(exception); }
        });
    }

    @Test
    public void missingFallbackStaleAndUnknownAgeAreReadable() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ResultRenderer renderer = new ResultRenderer();
            renderer.showResult(estimate(null));
            assertEquals(CostResult.State.MISSING_QUOTES, renderer.displayedResult().getState());
            renderer.showResult(estimate(new WikiPrice(99L, null, 100L, null)));
            assertTrue(renderer.displayedResult().getNotices().stream().anyMatch(note -> note.startsWith("Opposite-side")));
            assertTrue(renderer.displayedResult().getNotices().stream().anyMatch(note -> note.startsWith("Quote older")));
            renderer.showResult(estimate(new WikiPrice(99L, 99L, null, null)));
            assertTrue(renderer.displayedResult().getNotices().stream().anyMatch(note -> note.startsWith("Quote age unknown")));
        });
    }
}
