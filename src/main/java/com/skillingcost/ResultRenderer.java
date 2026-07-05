package com.skillingcost;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

/**
 * Owns the scrollable result area at the bottom of the panel and knows how to render either a
 * one-line status message or a full {@link CostResult} breakdown into it.
 */
final class ResultRenderer
{
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        .withZone(ZoneId.systemDefault());

    private final JPanel resultPanel = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane(resultPanel);

    ResultRenderer()
    {
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        resultPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        resultPanel.setOpaque(true);

        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setOpaque(true);
    }

    /** The component to embed in the plugin panel. */
    JScrollPane getComponent()
    {
        return scrollPane;
    }

    void showStatus(String status)
    {
        resultPanel.removeAll();
        addPlain(status);
        finishRender();
    }

    /** Like {@link #showStatus} but in red, for notices the user should not miss. */
    void showWarning(String status)
    {
        resultPanel.removeAll();
        addPlain(status, SkillingCostPanel.LOSS);
        finishRender();
    }

    void showResult(CostResult result)
    {
        resultPanel.removeAll();

        addSummaryLine("Net", Formats.gp(result.getNet()), result.getNet() >= 0);
        if (result.getXpNeeded() > 0)
        {
            addLine("GP/XP", String.format("%.2f", (double) result.getNet() / result.getXpNeeded()));
        }
        addLine("Actions", String.format("%,d", result.getActionsNeeded()));
        addLine("XP needed", String.format("%,d", result.getXpNeeded()));
        addLine("Input cost", Formats.gp(result.getInputCost()));
        addLine("Output gross", Formats.gp(result.getOutputGross()));
        addLine("GE tax", Formats.gp(result.getTax()));

        addSpacer();
        addHeader("Method");
        addPlain(result.getMethod().getName());
        addPlain("Required level: " + result.getMethod().getRequiredLevel());
        addPlain("XP/action: " + result.getMethod().getXpPerAction());
        addPlain("Current: Level " + result.getCurrentLevel() + " / " + String.format("%,d", result.getCurrentXp()) + " XP");
        addPlain("Target: Level " + result.getTargetLevel() + " / " + String.format("%,d", result.getTargetXp()) + " XP");

        addSpacer();
        addHeader("Required inputs");
        if (result.getRequiredInputLines().isEmpty())
        {
            addPlain("None.");
        }
        for (String line : result.getRequiredInputLines())
        {
            addBlock(line);
        }
        if (result.isIncludeOptionalInputs())
        {
            for (String line : result.getOptionalInputLines())
            {
                addBlock(line);
            }
        }

        addSpacer();
        addHeader("Outputs");
        if (result.isSellOutputs() && !result.getOutputLines().isEmpty())
        {
            for (String line : result.getOutputLines())
            {
                addBlock(line);
            }
        }
        else if (result.getMethod().hasOutputs())
        {
            addPlain("Not sold.");
        }
        else
        {
            addPlain("No sellable output.");
        }

        if (result.getNewestPriceTimestamp() > 0)
        {
            addSpacer();
            addPlain("Newest price timestamp:");
            addPlain(TIME_FORMATTER.format(Instant.ofEpochSecond(result.getNewestPriceTimestamp())));
        }

        finishRender();
    }

    private void finishRender()
    {
        resultPanel.revalidate();
        resultPanel.repaint();
        // Jump back to the top whenever new content is rendered.
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private void addHeader(String text)
    {
        JLabel label = new JLabel("<html><b>" + escape(text) + "</b></html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.add(label);
    }

    private void addSummaryLine(String name, String value, boolean positive)
    {
        JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new JLabel(html("<b>" + escape(name) + "</b>")));
        JLabel valueLabel = new JLabel(html("<b>" + escape(value) + "</b>"));
        valueLabel.setForeground(positive ? SkillingCostPanel.PROFIT : SkillingCostPanel.LOSS);
        row.add(valueLabel);
        resultPanel.add(row);
    }

    private void addLine(String name, String value)
    {
        JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new JLabel(html("<b>" + escape(name) + "</b>")));
        row.add(new JLabel(html(escape(value))));
        resultPanel.add(row);
    }

    /** Adds a multi-line item block (one JTextArea per line, followed by a small gap). */
    private void addBlock(String text)
    {
        for (String line : text.split("\\n"))
        {
            addPlain(line);
        }
        resultPanel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    /**
     * Adds one wrapping line of plain text. A JTextArea is used instead of a JLabel because it
     * can word-wrap to the current viewport width (recomputed in getPreferredSize so wrapping
     * follows panel resizes).
     */
    private void addPlain(String text)
    {
        addPlain(text, SkillingCostPanel.TEXT);
    }

    private void addPlain(String text, java.awt.Color color)
    {
        JTextArea area = new JTextArea(text)
        {
            @Override
            public Dimension getPreferredSize()
            {
                int viewportWidth = scrollPane.getViewport().getWidth();
                int wrapWidth = viewportWidth > 0 ? viewportWidth - 24 : 180;
                wrapWidth = Math.max(80, wrapWidth);
                setSize(new Dimension(wrapWidth, Short.MAX_VALUE));

                Dimension preferred = super.getPreferredSize();
                preferred.width = wrapWidth;
                return preferred;
            }

            @Override
            public Dimension getMaximumSize()
            {
                Dimension preferred = getPreferredSize();
                preferred.width = Integer.MAX_VALUE;
                return preferred;
            }
        };

        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setForeground(color);
        area.setFont(new JLabel().getFont());
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.add(area);
    }

    private void addSpacer()
    {
        resultPanel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private static String html(String body)
    {
        return "<html>" + body + "</html>";
    }

    static String escape(String value)
    {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
