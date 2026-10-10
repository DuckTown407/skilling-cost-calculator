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
import javax.swing.JToggleButton;
import net.runelite.client.ui.ColorScheme;

/**
 * Keeps loading/failure status separate from main totals and expandable exact resources.
 */
final class ResultRenderer
{
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        .withZone(ZoneId.systemDefault());

    private final JPanel container = new JPanel();
    private final JTextArea status = new JTextArea()
    {
        @Override
        public Dimension getPreferredSize()
        {
            int width = Math.max(80, container.getWidth() > 0 ? container.getWidth() : 200);
            setSize(new Dimension(width, Short.MAX_VALUE));
            Dimension size = super.getPreferredSize();
            size.width = width;
            return size;
        }

        @Override
        public Dimension getMaximumSize()
        {
            Dimension size = getPreferredSize();
            size.width = Integer.MAX_VALUE;
            return size;
        }
    };
    private final JPanel resultPanel = new ScrollablePanel();
    private final JPanel details = new JPanel();
    private final JToggleButton expand = new JToggleButton("<html>Resources and<br>assumptions</html>");
    private CostResult displayed;
    private boolean addingDetails;
    private final JScrollPane scrollPane = new JScrollPane(resultPanel);

    ResultRenderer()
    {
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        status.setEditable(false);
        status.setFocusable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setOpaque(false);
        status.setForeground(SkillingCostPanel.SUBTEXT);
        status.setVisible(false);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(status);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        expand.setAlignmentX(Component.LEFT_ALIGNMENT);
        expand.addActionListener(event -> {
            details.setVisible(expand.isSelected());
            finishRender();
        });
        container.add(scrollPane);
        scrollPane.setPreferredSize(new Dimension(200, 280));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
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
    JPanel getComponent()
    {
        return container;
    }

    void showStatus(String message)
    {
        status.setText(message);
        status.setVisible(!message.isEmpty());
        container.revalidate();
        container.repaint();
    }

    void showFailure(String message)
    {
        showStatus(message + (displayed == null ? "" : "\nDisplayed result uses the previous snapshot and inputs."));
    }

    CostResult displayedResult() { return displayed; }
    String statusText() { return status.getText(); }

    void showResult(CostResult result)
    {
        displayed = result;
        status.setText("");
        status.setVisible(false);
        resultPanel.removeAll();
        details.removeAll();
        addingDetails = false;
        if (result.isAvailable())
        {
            addSummaryLine("Net", Formats.gpCompact(result.getNet()), result.getNet() >= 0);
            if (result.getXpNeeded() > 0)
            {
                addLine("GP/XP", Formats.gpPerXp(result.getNet(), result.getXpNeeded()));
            }
            addLine("Inputs", Formats.gpCompact(result.getInputCost()));
            addLine("Outputs", Formats.gpCompact(result.getOutputGross()));
            addLine("Tax", Formats.gpCompact(result.getTax()));
        }
        else
        {
            addHeader(result.getState() == CostResult.State.OUT_OF_RANGE ? "Estimate out of range" : "Estimate unavailable");
        }
        addLine("Actions", String.format("%,d", result.getActionsNeeded()));
        addLine("XP needed", String.format("%,d", result.getXpNeeded()));
        addPlain("Action: " + result.getMethod().getActionUnit());
        boolean fallback = result.getNotices().stream().anyMatch(note -> note.startsWith("Opposite-side"));
        boolean stale = result.getNotices().stream().anyMatch(note -> note.startsWith("Quote older"));
        boolean unknownAge = result.getNotices().stream().anyMatch(note -> note.startsWith("Quote age unknown"));
        if (fallback) { addPlain("Fallback quote used; see resources."); }
        if (stale) { addPlain("Stale quote: older than ten minutes."); }
        if (unknownAge) { addPlain("Quote age unknown; see resources."); }
        if (result.getState() == CostResult.State.MISSING_QUOTES) { addPlain("Required buy or sell quotes are missing."); }
        if (result.getState() == CostResult.State.INVALID_SETTINGS) { addPlain("Check GE tax settings in plugin config."); }
        if (result.getMethod().hasLevelDependentOutput()) { addPlain("Yield uses the starting level throughout."); }
        resultPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        resultPanel.add(expand);
        resultPanel.add(details);
        details.setVisible(expand.isSelected());
        addingDetails = true;
        if (result.isAvailable())
        {
            addPlain("Exact net: " + Formats.gp(result.getNet()));
            addPlain("Exact inputs: " + Formats.gp(result.getInputCost()));
            addPlain("Exact output gross: " + Formats.gp(result.getOutputGross()));
            addPlain("Exact tax: " + Formats.gp(result.getTax()));
        }
        for (String notice : result.getNotices()) { addPlain(notice); }
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
        for (String line : result.getOutputLines()) { addBlock(line); }
        if (result.getOutputLines().isEmpty()) { addPlain("No output."); }

        addSpacer();
        addHeader("Assumptions");
        addPlain(result.getMethod().getAssumptions().isEmpty() ? "Standard recipe; equipment and travel excluded." : result.getMethod().getAssumptions());
        addPlain("Source: " + result.getMethod().getSource());
        if (!result.getSnapshotTime().equals(Instant.EPOCH))
        {
            addPlain("Snapshot fetched: " + TIME_FORMATTER.format(result.getSnapshotTime()));
        }

        if (result.getOldestPriceTimestamp() > 0)
        {
            addSpacer();
            addPlain("Oldest used quote:");
            addPlain(TIME_FORMATTER.format(Instant.ofEpochSecond(result.getOldestPriceTimestamp())));
        }

        addingDetails = false;
        finishRender();
    }

    private void finishRender()
    {
        resultPanel.revalidate();
        resultPanel.repaint();
        container.revalidate();
        container.repaint();
    }

    private void addHeader(String text)
    {
        JLabel label = new JLabel("<html><b>" + escape(text) + "</b></html>");
        label.setForeground(SkillingCostPanel.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        (addingDetails ? details : resultPanel).add(label);
    }

    private void addSummaryLine(String name, String value, boolean positive)
    {
        JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nameLabel = new JLabel(html("<b>" + escape(name) + "</b>"));
        nameLabel.setForeground(SkillingCostPanel.TEXT);
        row.add(nameLabel);
        JLabel valueLabel = new JLabel(html("<b>" + escape(value) + "</b>"));
        valueLabel.setForeground(positive ? SkillingCostPanel.PROFIT : SkillingCostPanel.LOSS);
        row.add(valueLabel);
        (addingDetails ? details : resultPanel).add(row);
    }

    private void addLine(String name, String value)
    {
        JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nameLabel = new JLabel(html("<b>" + escape(name) + "</b>"));
        JLabel valueLabel = new JLabel(html(escape(value)));
        nameLabel.setForeground(SkillingCostPanel.TEXT);
        valueLabel.setForeground(SkillingCostPanel.TEXT);
        row.add(nameLabel);
        row.add(valueLabel);
        (addingDetails ? details : resultPanel).add(row);
    }

    /** Adds a multi-line item block (one JTextArea per line, followed by a small gap). */
    private void addBlock(String text)
    {
        for (String line : text.split("\\n"))
        {
            addPlain(line);
        }
        (addingDetails ? details : resultPanel).add(Box.createRigidArea(new Dimension(0, 4)));
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
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        area.setForeground(color);
        area.setFont(new JLabel().getFont());
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        (addingDetails ? details : resultPanel).add(area);
    }

    private void addSpacer()
    {
        (addingDetails ? details : resultPanel).add(Box.createRigidArea(new Dimension(0, 8)));
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
