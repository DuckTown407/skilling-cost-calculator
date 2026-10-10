package com.skillingcost;

import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.runelite.api.Skill;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteLAF;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the whole sidebar with isolated Swing components; no client or game input. */
public class PanelLayoutTest
{
    private LookAndFeel previousLookAndFeel;
    private String previousScaleSetting;

    @Before
    public void useRuneLiteTheme() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            previousLookAndFeel = UIManager.getLookAndFeel();
            previousScaleSetting = System.getProperty("flatlaf.uiScale.enabled");
            assertTrue(RuneLiteLAF.setup());
        });
    }

    @After
    public void restoreTheme() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            try { UIManager.setLookAndFeel(previousLookAndFeel); }
            catch (UnsupportedLookAndFeelException exception) { throw new AssertionError(exception); }
            if (previousScaleSetting == null) { System.clearProperty("flatlaf.uiScale.enabled"); }
            else { System.setProperty("flatlaf.uiScale.enabled", previousScaleSetting); }
        });
    }

    private static final CalculatorController CONTROLLER = new CalculatorController()
    {
        public void loadCurrentSkillXp(Skill skill, IntConsumer callback) { callback.accept(-1); }
        public void estimate(CalculationRequest request, Consumer<Map<String, CostResult>> success, Consumer<String> failure) { }
    };

    private static SkillingCostPanel panel()
    {
        return new SkillingCostPanel(CONTROLLER, new MethodRepository(new Gson()), action -> new EstimateScheduler()
        {
            public void restart() { }
            public void stop() { }
        });
    }

    private static void layout(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents())
        {
            if (child instanceof Container) { layout((Container) child); }
        }
    }

    private static void settle(SkillingCostPanel panel, int height)
    {
        panel.getWrappedPanel().setSize(new Dimension(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH, height));
        for (int pass = 0; pass < 5; pass++) { layout(panel); }
    }

    private static void assertViewportWidth(JScrollPane scroll)
    {
        assertEquals("Content must fit the visible sidebar width", scroll.getViewport().getExtentSize().width,
            scroll.getViewport().getView().getWidth());
        assertEquals("Hidden horizontal scrolling must never shift content", 0, scroll.getViewport().getViewPosition().x);
    }

    @Test
    public void controlsStayInsideSidebarAcrossSkillsAndFocusScrolling() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SkillingCostPanel panel = panel();
            try
            {
                JScrollPane scroll = (JScrollPane) panel.getComponent(0);
                for (int index = 0; index < panel.skillSelect.getItemCount(); index++)
                {
                    panel.skillSelect.setSelectedIndex(index);
                    panel.currentXp.setValue(200_000_000);
                    panel.targetXp.setValue(200_000_000);
                    for (int height : new int[]{450, 700, 1400})
                    {
                        settle(panel, height);
                        assertViewportWidth(scroll);
                        assertViewportWidth((JScrollPane) panel.list.getParent().getParent());
                        for (JComponent control : new JComponent[]{panel.skillSelect, panel.currentLevel, panel.currentXp,
                            panel.targetLevel, panel.targetXp, panel.loadXp, panel.search, panel.sortSelect,
                            panel.hideLocked, panel.optional, panel.sell, panel.tax, panel.refresh, panel.variants, panel.batchSize})
                        {
                            if (!control.isVisible() || !control.getParent().isVisible()) { continue; }
                            Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(),
                                scroll.getViewport().getView());
                            assertTrue("Control starts outside sidebar: " + control.getClass().getSimpleName(), bounds.x >= 0);
                            assertTrue("Control extends outside sidebar: " + control.getClass().getSimpleName(),
                                bounds.x + bounds.width <= scroll.getViewport().getExtentSize().width);
                            control.scrollRectToVisible(new Rectangle(0, 0, control.getWidth(), control.getHeight()));
                            assertViewportWidth(scroll);
                        }
                        for (JSpinner spinner : new JSpinner[]{panel.currentXp, panel.targetXp})
                        {
                            javax.swing.JFormattedTextField editor = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                            assertTrue("Full XP must fit its editor", editor.getFontMetrics(editor.getFont()).stringWidth(editor.getText())
                                <= editor.getWidth() - editor.getInsets().left - editor.getInsets().right);
                        }
                    }
                }
            }
            finally { panel.dispose(); }
        });
    }

    @Test
    public void expandedResultsStayInsideSidebarAndScrollVertically() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SkillingCostPanel panel = panel();
            try
            {
                panel.skillSelect.setSelectedItem(Skill.PRAYER);
                panel.currentLevel.setValue(99);
                SkillingMethod method = panel.list.getSelectedValue().getSelected();
                Instant now = Instant.ofEpochSecond(1000);
                panel.renderer.showResult(CostCalculator.calculate(method, 99, 0, 99, 1,
                    true, true, true, 28, new PriceSnapshot(Collections.emptyMap(), now),
                    new CostCalculator.TaxSettings(200, 5_000_000, 50), now));
                JToggleButton expand = findExpand(panel.renderer.getComponent());
                expand.doClick();
                settle(panel, 1400);
                assertViewportWidth((JScrollPane) panel.getComponent(0));
                for (int index = 0; index < panel.rows.size(); index++)
                {
                    Rectangle bounds = panel.list.getCellBounds(index, index);
                    Container row = (Container) panel.list.getCellRenderer().getListCellRendererComponent(panel.list,
                        panel.rows.get(index), index, false, false);
                    row.setSize(bounds.getSize());
                    layout(row);
                    BufferedImage cell = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D cellGraphics = cell.createGraphics();
                    row.paint(cellGraphics);
                    cellGraphics.dispose();
                    for (Component component : row.getComponents())
                    {
                        javax.swing.JTextArea text = (javax.swing.JTextArea) component;
                        assertTrue("Wrapped method row text must fit its allocated height: " + text.getText(),
                            text.getUI().getRootView(text).getPreferredSpan(javax.swing.text.View.Y_AXIS)
                                <= text.getHeight() - text.getInsets().top - text.getInsets().bottom);
                    }
                }
                JScrollPane results = (JScrollPane) panel.renderer.getComponent().getComponent(1);
                assertViewportWidth(results);
                assertTrue("Exact resources need vertical scrolling", results.getVerticalScrollBar().isVisible());
                BufferedImage preview = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = preview.createGraphics();
                panel.paint(graphics);
                graphics.dispose();
                File directory = new File("build/reports/tests");
                assertTrue(directory.isDirectory() || directory.mkdirs());
                try { ImageIO.write(preview, "png", new File(directory, "full-sidebar-fixture.png")); }
                catch (IOException exception) { throw new AssertionError(exception); }
            }
            finally { panel.dispose(); }
        });
    }

    private static JToggleButton findExpand(Container container)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof JToggleButton) { return (JToggleButton) component; }
            if (component instanceof Container)
            {
                JToggleButton found = findExpand((Container) component);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
