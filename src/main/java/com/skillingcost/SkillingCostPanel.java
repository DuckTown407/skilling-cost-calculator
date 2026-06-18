package com.skillingcost;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.PluginPanel;

final class SkillingCostPanel extends PluginPanel
{
    private static final int MAX_SKILL_LEVEL = 99;
    private static final int MAX_XP = 200_000_000;
    private static final Color PANEL_BG = new Color(36, 36, 36);
    private static final Color ROW_BG = new Color(31, 31, 31);
    private static final Color ROW_SELECTED_BG = new Color(82, 82, 82);
    private static final Color BORDER = new Color(82, 82, 82);
    private static final Color DIVIDER = new Color(70, 70, 70);
    private static final Color TEXT = new Color(220, 220, 220);
    private static final Color SUBTEXT = new Color(170, 170, 170);
    private static final Color UNLOCKED = new Color(0, 180, 75);
    private static final Color LOCKED = new Color(190, 55, 55);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        .withZone(ZoneId.systemDefault());

    private final SkillingCostPlugin plugin;
    private final Map<Skill, List<SkillingMethod>> methodsBySkill = SkillingMethod.methodsBySkill();
    private final JComboBox<Skill> skillSelect = new JComboBox<>(SkillingMethod.SUPPORTED_SKILLS.toArray(new Skill[0]));
    private final JComboBox<MethodSort> sortSelect = new JComboBox<>(MethodSort.values());
    private final DefaultListModel<SkillingMethod> methodListModel = new DefaultListModel<>();
    private final JList<SkillingMethod> methodList = new JList<>(methodListModel);
    private final List<SkillingMethod> currentMethods = new ArrayList<>();
    private final Map<SkillingMethod, Long> netEstimates = new HashMap<>();
    private final JSpinner currentLevel = new JSpinner(new SpinnerNumberModel(1, 1, MAX_SKILL_LEVEL, 1));
    private final JSpinner currentXp = new JSpinner(new SpinnerNumberModel(0, 0, MAX_XP, 1));
    private final JSpinner targetLevel = new JSpinner(new SpinnerNumberModel(84, 1, MAX_SKILL_LEVEL, 1));
    private final JSpinner targetXp = new JSpinner(new SpinnerNumberModel(Experience.getXpForLevel(84), 0, MAX_XP, 1));
    private final JButton loadCurrentXpButton = new JButton("Load current XP");
    private final JCheckBox hideLockedMethods = new JCheckBox("Hide locked methods", false);
    private final JCheckBox includeOptionalInputs = new JCheckBox("Include optional consumables", true);
    private final JCheckBox sellOutputs = new JCheckBox("Sell outputs", true);
    private final JCheckBox includeTax = new JCheckBox("Include GE tax", true);
    private final JButton calculateButton = new JButton("Calculate selected method");
    private final JPanel resultPanel = new JPanel();
    private final JScrollPane resultScrollPane = new JScrollPane(resultPanel);

    private boolean syncingFields;
    private boolean updatingSelection;
    private int xpLoadRequestId;
    private int netSortRequestId;

    private enum MethodSort
    {
        DEFAULT("Default"),
        LEAST_ACTIONS("Least actions"),
        MOST_PROFITABLE("Most profitable");

        private final String label;

        MethodSort(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    SkillingCostPanel(SkillingCostPlugin plugin)
    {
        super(false);
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        configureSkillRenderer();
        skillSelect.setAlignmentX(Component.LEFT_ALIGNMENT);
        skillSelect.setMaximumSize(new Dimension(Integer.MAX_VALUE, skillSelect.getPreferredSize().height));
        skillSelect.addActionListener(e -> updateMethodsForSelectedSkill());
        content.add(labeled("Skill", skillSelect));
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        configureSpinner(currentLevel);
        configureSpinner(currentXp);
        configureSpinner(targetLevel);
        configureSpinner(targetXp);
        wireXpLevelSync();

        content.add(twoColumnFields("Current Level", currentLevel, "Current Experience", currentXp));
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(twoColumnFields("Target Level", targetLevel, "Target Experience", targetXp));
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        loadCurrentXpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadCurrentXpButton.addActionListener(e -> loadCurrentXp());
        content.add(loadCurrentXpButton);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(sectionDivider());
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        sortSelect.setAlignmentX(Component.LEFT_ALIGNMENT);
        sortSelect.setMaximumSize(new Dimension(Integer.MAX_VALUE, sortSelect.getPreferredSize().height));
        sortSelect.addActionListener(e -> updateMethodOrdering(true));
        content.add(labeled("Sort", sortSelect));
        content.add(Box.createRigidArea(new Dimension(0, 4)));

        hideLockedMethods.setAlignmentX(Component.LEFT_ALIGNMENT);
        hideLockedMethods.setToolTipText("Hide methods above your current level.");
        hideLockedMethods.addActionListener(e -> {
            updateMethodOrdering(true);
            clearResult("Filter changed. Recalculate when ready.");
        });
        content.add(hideLockedMethods);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        configureMethodList();
        content.add(methodListSection());
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        includeOptionalInputs.setAlignmentX(Component.LEFT_ALIGNMENT);
        sellOutputs.setAlignmentX(Component.LEFT_ALIGNMENT);
        includeTax.setAlignmentX(Component.LEFT_ALIGNMENT);
        includeOptionalInputs.addActionListener(e -> onInputsAffectingMethodListChanged("Options changed. Recalculate when ready."));
        sellOutputs.addActionListener(e -> {
            updateOptionState();
            onInputsAffectingMethodListChanged("Options changed. Recalculate when ready.");
        });
        includeTax.addActionListener(e -> onInputsAffectingMethodListChanged("Options changed. Recalculate when ready."));
        content.add(includeOptionalInputs);
        content.add(sellOutputs);
        content.add(includeTax);
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        calculateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        calculateButton.addActionListener(e -> onCalculate());
        content.add(calculateButton);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        resultPanel.setBackground(ROW_BG);
        resultPanel.setOpaque(true);

        resultScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultScrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        resultScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultScrollPane.getViewport().setBackground(ROW_BG);
        resultScrollPane.getViewport().setOpaque(true);
        resultScrollPane.setBackground(ROW_BG);
        resultScrollPane.setOpaque(true);

        add(content, BorderLayout.NORTH);
        add(resultScrollPane, BorderLayout.CENTER);
        updateMethodsForSelectedSkill();
        showStatus("Select a method, enter XP manually, or load current XP from the client.");
    }

    void showStatus(String status)
    {
        resultPanel.removeAll();
        addPlain(status);
        resultPanel.revalidate();
        resultPanel.repaint();
        resetResultScroll();
    }

    void showResult(CostResult result)
    {
        resultPanel.removeAll();

        addSummaryLine("Net", SkillingCostPlugin.formatGp(result.getNet()), result.getNet() >= 0);
        addLine("Actions", String.format("%,d", result.getActionsNeeded()));
        addLine("XP needed", String.format("%,d", result.getXpNeeded()));
        addLine("Input cost", SkillingCostPlugin.formatGp(result.getInputCost()));
        addLine("Output gross", SkillingCostPlugin.formatGp(result.getOutputGross()));
        addLine("GE tax", SkillingCostPlugin.formatGp(result.getTax()));

        addSpacer();
        addHeader("Method");
        addPlain(result.getMethod().getName());
        addPlain("Required level: " + result.getMethod().getRequiredLevel());
        addPlain("XP/action: " + result.getMethod().getXpPerAction());
        addPlain("Current: Level " + result.getCurrentLevel() + " / " + String.format("%,d", result.getCurrentXp()) + " XP");
        addPlain("Target: Level " + result.getTargetLevel() + " / " + String.format("%,d", result.getTargetXp()) + " XP");

        addSpacer();
        addHeader("Required inputs");
        for (String line : result.getRequiredInputLines())
        {
            addBlock(line);
        }
        if (result.isIncludeOptionalInputs() && !result.getOptionalInputLines().isEmpty())
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

        resultPanel.revalidate();
        resultPanel.repaint();
        resetResultScroll();
    }


    private void resetResultScroll()
    {
        javax.swing.SwingUtilities.invokeLater(() -> resultScrollPane.getVerticalScrollBar().setValue(0));
    }

    private void updateMethodsForSelectedSkill()
    {
        Skill skill = (Skill) skillSelect.getSelectedItem();

        // Kill any pending async XP/price updates from the previous skill before changing fields.
        xpLoadRequestId++;
        netSortRequestId++;

        netEstimates.clear();
        currentMethods.clear();
        includeOptionalInputs.setSelected(true);
        sellOutputs.setSelected(true);
        includeTax.setSelected(true);

        // Skill changes should never carry over a manually typed current XP from another skill.
        resetCurrentXpFields();
        clearResult("Skill changed. Loading current XP if available…");

        if (skill != null)
        {
            currentMethods.addAll(methodsBySkill.get(skill));
            loadCurrentXpButton.setText("Load current " + SkillingCostPlugin.displaySkill(skill) + " XP");
        }

        updateMethodOrdering(false);

        if (skill != null)
        {
            requestCurrentXpLoad(skill, true);
        }
    }

    private void resetCurrentXpFields()
    {
        syncingFields = true;
        setLevelSpinner(currentLevel, 1);
        setXpSpinner(currentXp, 0);
        syncSpinnerEditor(currentLevel);
        syncSpinnerEditor(currentXp);
        syncingFields = false;
    }

    private void updateMethodOrdering(boolean userTriggered)
    {
        MethodSort sort = (MethodSort) sortSelect.getSelectedItem();
        if (sort == null)
        {
            sort = MethodSort.DEFAULT;
        }

        if (sort == MethodSort.MOST_PROFITABLE)
        {
            updateProfitSortedMethods(userTriggered);
            return;
        }

        List<SkillingMethod> sorted = visibleMethods();
        if (sort == MethodSort.LEAST_ACTIONS)
        {
            sorted.sort(Comparator
                .comparingLong(this::actionsFor)
                .thenComparing(SkillingMethod::getRequiredLevel)
                .thenComparing(SkillingMethod::getName));
        }

        applyMethodList(sorted);
    }

    private void updateProfitSortedMethods(boolean userTriggered)
    {
        if (!commitSpinnerEdits())
        {
            showStatus("One of the XP/level fields has an invalid value.");
            return;
        }

        final int requestId = ++netSortRequestId;
        int currentXpValue = ((Number) currentXp.getValue()).intValue();
        int targetXpValue = ((Number) targetXp.getValue()).intValue();
        boolean shouldIncludeOptionalInputs = includeOptionalInputs.isEnabled() && includeOptionalInputs.isSelected();
        boolean shouldSellOutputs = sellOutputs.isEnabled() && sellOutputs.isSelected();
        boolean shouldIncludeTax = includeTax.isEnabled() && includeTax.isSelected();

        showStatus("Fetching prices to sort by estimated profit…");
        plugin.estimateNetForMethods(
            visibleMethods(),
            currentXpValue,
            targetXpValue,
            shouldIncludeOptionalInputs,
            shouldSellOutputs,
            shouldIncludeTax,
            estimates -> {
                if (requestId != netSortRequestId)
                {
                    return;
                }

                netEstimates.clear();
                netEstimates.putAll(estimates);

                List<SkillingMethod> sorted = visibleMethods();
                sorted.sort((left, right) -> {
                    long leftNet = estimates.getOrDefault(left, Long.MIN_VALUE);
                    long rightNet = estimates.getOrDefault(right, Long.MIN_VALUE);
                    int byNet = Long.compare(rightNet, leftNet);
                    if (byNet != 0)
                    {
                        return byNet;
                    }
                    return left.getName().compareTo(right.getName());
                });

                applyMethodList(sorted);
                showStatus("Sorted by estimated net. Recalculate the selected method for the full breakdown.");
            },
            error -> {
                if (requestId != netSortRequestId)
                {
                    return;
                }
                showStatus(error);
            });
    }

    private List<SkillingMethod> visibleMethods()
    {
        commitSpinnerEdits();
        List<SkillingMethod> visible = new ArrayList<>();
        int level = ((Number) currentLevel.getValue()).intValue();
        for (SkillingMethod method : currentMethods)
        {
            if (!hideLockedMethods.isSelected() || level >= method.getRequiredLevel())
            {
                visible.add(method);
            }
        }
        return visible;
    }

    private void applyMethodList(List<SkillingMethod> methods)
    {
        SkillingMethod previous = methodList.getSelectedValue();
        updatingSelection = true;
        methodListModel.clear();
        for (SkillingMethod method : methods)
        {
            methodListModel.addElement(method);
        }

        if (!methodListModel.isEmpty())
        {
            int index = previous == null ? -1 : methodListModel.indexOf(previous);
            methodList.setSelectedIndex(index >= 0 ? index : 0);
        }
        updatingSelection = false;
        updateOptionState();
        refreshMethodListDisplay();
    }

    private void updateOptionState()
    {
        SkillingMethod method = methodList.getSelectedValue();
        boolean hasMethod = method != null;
        boolean hasOptional = hasMethod && !method.getOptionalInputs().isEmpty();
        boolean hasOutputs = hasMethod && method.hasOutputs();

        includeOptionalInputs.setEnabled(hasOptional);
        if (hasOptional)
        {
            includeOptionalInputs.setText("Include optional: " + method.getOptionalInputSummary());
        }
        else
        {
            includeOptionalInputs.setText("No optional consumables");
            includeOptionalInputs.setSelected(false);
        }

        sellOutputs.setEnabled(hasOutputs);
        if (!hasOutputs)
        {
            sellOutputs.setSelected(false);
        }
        else if (!sellOutputs.isSelected())
        {
            // Preserve manual uncheck.
        }

        includeTax.setEnabled(hasOutputs && sellOutputs.isSelected());
        calculateButton.setEnabled(hasMethod);
    }

    private void configureSkillRenderer()
    {
        ListCellRenderer<? super Skill> baseRenderer = skillSelect.getRenderer();
        skillSelect.setRenderer((JList<? extends Skill> list, Skill value, int index, boolean isSelected, boolean cellHasFocus) -> {
            JLabel label = (JLabel) baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null)
            {
                label.setText(SkillingCostPlugin.displaySkill(value));
            }
            return label;
        });
    }

    private void configureMethodList()
    {
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodList.setVisibleRowCount(5);
        methodList.setFixedCellHeight(64);
        methodList.setBackground(PANEL_BG);
        methodList.setCellRenderer(new MethodCellRenderer());
        methodList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingSelection)
            {
                return;
            }
            includeOptionalInputs.setSelected(true);
            sellOutputs.setSelected(true);
            updateOptionState();
            clearResult("Method changed. Recalculate when ready.");
        });
    }

    private JPanel methodListSection()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 315));
        panel.add(new JLabel("Method"), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(methodList);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.setPreferredSize(new Dimension(0, 280));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadCurrentXp()
    {
        Skill skill = (Skill) skillSelect.getSelectedItem();
        if (skill == null)
        {
            showStatus("No skill selected.");
            return;
        }

        requestCurrentXpLoad(skill, false);
    }

    private void requestCurrentXpLoad(Skill skill, boolean automatic)
    {
        final int requestId = ++xpLoadRequestId;
        final String skillName = SkillingCostPlugin.displaySkill(skill);

        loadCurrentXpButton.setEnabled(false);
        if (automatic)
        {
            showStatus("Skill changed. Reading current " + skillName + " XP from client…");
        }
        else
        {
            showStatus("Reading current " + skillName + " XP from client…");
        }

        plugin.loadCurrentSkillXp(skill, xp -> {
            if (requestId != xpLoadRequestId || skill != skillSelect.getSelectedItem())
            {
                return;
            }

            loadCurrentXpButton.setEnabled(true);

            if (xp < 0)
            {
                showStatus("Could not read current " + skillName + " XP. Enter current XP manually.");
                return;
            }

            syncingFields = true;
            setXpSpinner(currentXp, xp);
            setLevelSpinner(currentLevel, levelForXp(xp));
            syncSpinnerEditor(currentXp);
            syncSpinnerEditor(currentLevel);
            syncingFields = false;
            updateMethodOrdering(false);

            showStatus("Loaded current " + skillName + " XP: " + String.format("%,d", xp) + ". You can edit it manually if needed.");
        });
    }

    private void onCalculate()
    {
        if (!commitSpinnerEdits())
        {
            showStatus("One of the XP/level fields has an invalid value.");
            return;
        }

        SkillingMethod method = methodList.getSelectedValue();
        if (method == null)
        {
            showStatus("No method selected.");
            return;
        }

        int currentXpValue = ((Number) currentXp.getValue()).intValue();
        int targetXpValue = ((Number) targetXp.getValue()).intValue();
        int targetLevelValue = ((Number) targetLevel.getValue()).intValue();

        showStatus("Calculating…");
        plugin.calculate(
            method,
            currentXpValue,
            targetLevelValue,
            targetXpValue,
            includeOptionalInputs.isEnabled() && includeOptionalInputs.isSelected(),
            sellOutputs.isEnabled() && sellOutputs.isSelected(),
            includeTax.isEnabled() && includeTax.isSelected());
    }

    private void wireXpLevelSync()
    {
        currentLevel.addChangeListener(e -> {
            if (syncingFields)
            {
                return;
            }
            syncingFields = true;
            setXpSpinner(currentXp, Experience.getXpForLevel(((Number) currentLevel.getValue()).intValue()));
            syncingFields = false;
            onInputsAffectingMethodListChanged("Current XP changed. Recalculate when ready.");
        });

        currentXp.addChangeListener(e -> {
            if (syncingFields)
            {
                return;
            }
            syncingFields = true;
            setLevelSpinner(currentLevel, levelForXp(((Number) currentXp.getValue()).intValue()));
            syncingFields = false;
            onInputsAffectingMethodListChanged("Current XP changed. Recalculate when ready.");
        });

        targetLevel.addChangeListener(e -> {
            if (syncingFields)
            {
                return;
            }
            syncingFields = true;
            setXpSpinner(targetXp, Experience.getXpForLevel(((Number) targetLevel.getValue()).intValue()));
            syncingFields = false;
            onInputsAffectingMethodListChanged("Target XP changed. Recalculate when ready.");
        });

        targetXp.addChangeListener(e -> {
            if (syncingFields)
            {
                return;
            }
            syncingFields = true;
            setLevelSpinner(targetLevel, levelForXp(((Number) targetXp.getValue()).intValue()));
            syncingFields = false;
            onInputsAffectingMethodListChanged("Target XP changed. Recalculate when ready.");
        });
    }

    private boolean commitSpinnerEdits()
    {
        try
        {
            currentLevel.commitEdit();
            currentXp.commitEdit();
            targetLevel.commitEdit();
            targetXp.commitEdit();
            return true;
        }
        catch (ParseException ex)
        {
            return false;
        }
    }

    private void configureSpinner(JSpinner spinner)
    {
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, spinner.getPreferredSize().height));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#,##0"));
    }

    private JPanel twoColumnFields(String leftLabel, Component left, String rightLabel, Component right)
    {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panel.add(labeled(leftLabel, left));
        panel.add(labeled(rightLabel, right));
        return panel;
    }

    private JPanel labeled(String label, Component component)
    {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private Component sectionDivider()
    {
        JPanel divider = new JPanel();
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setMinimumSize(new Dimension(1, 1));
        divider.setBackground(DIVIDER);
        return divider;
    }

    private void setXpSpinner(JSpinner spinner, int xp)
    {
        spinner.setValue(Math.max(0, Math.min(MAX_XP, xp)));
    }

    private void setLevelSpinner(JSpinner spinner, int level)
    {
        spinner.setValue(Math.max(1, Math.min(MAX_SKILL_LEVEL, level)));
    }

    private void syncSpinnerEditor(JSpinner spinner)
    {
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor)
        {
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
            editor.getTextField().setValue(spinner.getValue());
        }
    }

    private static int levelForXp(int xp)
    {
        return Math.min(MAX_SKILL_LEVEL, Experience.getLevelForXp(Math.max(0, xp)));
    }

    private void refreshMethodListDisplay()
    {
        methodList.repaint();
    }

    private void clearResult(String message)
    {
        plugin.invalidatePendingCalculations();
        showStatus(message);
    }

    private void onInputsAffectingMethodListChanged(String message)
    {
        MethodSort sort = (MethodSort) sortSelect.getSelectedItem();
        if (hideLockedMethods.isSelected() || sort == MethodSort.LEAST_ACTIONS || sort == MethodSort.MOST_PROFITABLE)
        {
            updateMethodOrdering(false);
        }
        else
        {
            refreshMethodListDisplay();
            clearResult(message);
        }
    }

    private long actionsFor(SkillingMethod method)
    {
        int currentXpValue = ((Number) currentXp.getValue()).intValue();
        int targetXpValue = ((Number) targetXp.getValue()).intValue();
        int xpNeeded = Math.max(0, targetXpValue - currentXpValue);
        return (long) Math.ceil(xpNeeded / method.getXpPerAction());
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
        valueLabel.setForeground(positive ? UNLOCKED : LOCKED);
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

    private void addBlock(String text)
    {
        String[] lines = text.split("\\n");
        for (String line : lines)
        {
            addPlain(line);
        }
        resultPanel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private void addPlain(String text)
    {
        JTextArea area = new JTextArea(text)
        {
            @Override
            public Dimension getPreferredSize()
            {
                int viewportWidth = resultScrollPane.getViewport().getWidth();
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
        area.setForeground(TEXT);
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

    private static String colorizeGp(String value, boolean positive)
    {
        return "<span style='color:" + (positive ? "#00b44b" : "#be3737") + "'>"
            + escape(value)
            + "</span>";
    }

    private static String escape(String value)
    {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private final class MethodCellRenderer extends JPanel implements ListCellRenderer<SkillingMethod>
    {
        private final JPanel stripe = new JPanel();
        private final JLabel name = new JLabel();
        private final JLabel detail = new JLabel();

        private MethodCellRenderer()
        {
            setLayout(new BorderLayout(8, 0));
            setOpaque(true);

            stripe.setPreferredSize(new Dimension(5, 1));
            stripe.setOpaque(true);
            add(stripe, BorderLayout.WEST);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 4));

            name.setForeground(TEXT);
            name.setFont(name.getFont().deriveFont(Font.PLAIN, name.getFont().getSize2D() + 1.0f));
            detail.setForeground(SUBTEXT);
            detail.setFont(detail.getFont().deriveFont(Font.PLAIN, detail.getFont().getSize2D() - 1.0f));

            textPanel.add(name);
            textPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            textPanel.add(detail);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SkillingMethod> list, SkillingMethod value, int index, boolean isSelected, boolean cellHasFocus)
        {
            int level = ((Number) currentLevel.getValue()).intValue();
            boolean unlocked = level >= value.getRequiredLevel();
            long actions = actionsFor(value);

            setBackground(isSelected ? ROW_SELECTED_BG : ROW_BG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 0, 2, 0),
                BorderFactory.createLineBorder(BORDER)));
            stripe.setBackground(unlocked ? UNLOCKED : LOCKED);
            name.setText(value.getName());

            MethodSort sort = (MethodSort) sortSelect.getSelectedItem();
            String prefix = "Lvl. " + value.getRequiredLevel()
                + " (" + value.getXpPerAction() + " xp) - ";

            if (sort == MethodSort.MOST_PROFITABLE)
            {
                Long estimate = netEstimates.get(value);
                if (estimate == null)
                {
                    detail.setForeground(SUBTEXT);
                    detail.setText(prefix + "net loading…");
                }
                else
                {
                    detail.setForeground(SUBTEXT);
                    detail.setText(html(escape(prefix + "net ")
                        + colorizeGp(SkillingCostPlugin.formatGp(estimate), estimate >= 0)));
                }
            }
            else
            {
                detail.setForeground(SUBTEXT);
                detail.setText(prefix + String.format("%,d", actions) + " actions");
            }

            return this;
        }
    }
}
