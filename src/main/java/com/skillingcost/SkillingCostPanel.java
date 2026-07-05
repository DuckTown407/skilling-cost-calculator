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
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * The plugin side panel. Layout, top to bottom:
 * skill picker -> current/target level+XP spinners -> sort picker and lock filter ->
 * method list (one row per method or method group) -> variant dropdown (only for groups) ->
 * option checkboxes -> calculate button -> scrollable result area ({@link ResultRenderer}).
 *
 * Level and XP spinners are kept in sync both ways; the {@code syncingFields} flag prevents
 * the change listeners from feeding back into each other.
 */
final class SkillingCostPanel extends PluginPanel
{
    private static final int MAX_SKILL_LEVEL = 99;
    private static final int MAX_XP = 200_000_000;
    private static final int DEFAULT_TARGET_LEVEL = 2;

    static final Color TEXT = new Color(220, 220, 220);
    static final Color SUBTEXT = new Color(170, 170, 170);
    static final Color PROFIT = new Color(0, 180, 75);
    static final Color LOSS = new Color(190, 55, 55);

    private final SkillingCostPlugin plugin;
    private final Map<Skill, List<SkillingMethod>> methodsBySkill;

    private final JComboBox<Skill> skillSelect;
    private final JComboBox<MethodSort> sortSelect = new JComboBox<>(MethodSort.values());
    private final DefaultListModel<MethodListEntry> entryListModel = new DefaultListModel<>();
    private final JList<MethodListEntry> entryList = new JList<>(entryListModel);
    private final List<MethodListEntry> currentEntries = new ArrayList<>();
    private final Map<SkillingMethod, Long> netEstimates = new HashMap<>();
    private final JComboBox<SkillingMethod> variantSelect = new JComboBox<>();
    private final JPanel variantRow;
    private final JSpinner currentLevel = new JSpinner(new SpinnerNumberModel(1, 1, MAX_SKILL_LEVEL, 1));
    private final JSpinner currentXp = new JSpinner(new SpinnerNumberModel(0, 0, MAX_XP, 1));
    private final JSpinner targetLevel = new JSpinner(new SpinnerNumberModel(DEFAULT_TARGET_LEVEL, 1, MAX_SKILL_LEVEL, 1));
    private final JSpinner targetXp = new JSpinner(new SpinnerNumberModel(Experience.getXpForLevel(DEFAULT_TARGET_LEVEL), 0, MAX_XP, 1));
    private final JButton loadCurrentXpButton = new JButton("Load current XP");
    private final JCheckBox hideLockedMethods = new JCheckBox("Hide locked methods", false);
    private final JCheckBox includeOptionalInputs = new JCheckBox("Include optional consumables", true);
    private final JCheckBox sellOutputs = new JCheckBox("Sell outputs", true);
    private final JCheckBox includeTax = new JCheckBox("Include GE tax", true);
    private final JButton calculateButton = new JButton("Calculate selected method");
    private final ResultRenderer resultRenderer = new ResultRenderer();

    private boolean syncingFields;
    private boolean updatingSelection;
    private boolean updatingVariants;
    private int xpLoadRequestId;
    private int netSortRequestId;

    private enum MethodSort
    {
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

    SkillingCostPanel(SkillingCostPlugin plugin, MethodRepository methodRepository)
    {
        super(false);
        this.plugin = plugin;
        this.methodsBySkill = methodRepository.getMethodsBySkill();
        this.skillSelect = new JComboBox<>(methodRepository.getSupportedSkills().toArray(new Skill[0]));

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

        configureEntryList();
        content.add(methodListSection());
        content.add(Box.createRigidArea(new Dimension(0, 4)));

        configureVariantSelect();
        variantRow = labeled("Variant", variantSelect);
        variantRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        variantRow.setVisible(false);
        content.add(variantRow);
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

        add(content, BorderLayout.NORTH);
        add(resultRenderer.getComponent(), BorderLayout.CENTER);
        updateMethodsForSelectedSkill();
        showStatus("Select a method, enter XP manually, or load current XP from the client.");
    }

    void showStatus(String status)
    {
        resultRenderer.showStatus(status);
    }

    void showResult(CostResult result)
    {
        resultRenderer.showResult(result);
    }

    private void showStatusWarning(String status)
    {
        resultRenderer.showWarning(status);
    }

    // --- Skill / method list handling ---

    private void updateMethodsForSelectedSkill()
    {
        Skill skill = (Skill) skillSelect.getSelectedItem();

        // Kill any pending async XP/price updates from the previous skill before changing fields.
        xpLoadRequestId++;
        netSortRequestId++;

        netEstimates.clear();
        currentEntries.clear();
        includeOptionalInputs.setSelected(true);
        sellOutputs.setSelected(true);
        includeTax.setSelected(true);

        // Skill changes should never carry over a manually typed current XP from another skill.
        resetCurrentXpFields();
        clearResult("Skill changed. Loading current XP if available…");

        if (skill != null)
        {
            currentEntries.addAll(MethodListEntry.build(methodsBySkill.get(skill)));
            loadCurrentXpButton.setText("Load current " + Formats.skillName(skill) + " XP");
        }

        updateMethodOrdering(false);

        if (skill != null)
        {
            requestCurrentXpLoad(skill, true);
        }
    }

    private void updateMethodOrdering(boolean userTriggered)
    {
        MethodSort sort = (MethodSort) sortSelect.getSelectedItem();
        if (sort == MethodSort.MOST_PROFITABLE)
        {
            updateProfitSortedEntries(userTriggered);
            return;
        }

        List<MethodListEntry> sorted = visibleEntries();
        sorted.sort(Comparator
            .comparingLong((MethodListEntry entry) -> actionsFor(entry.getSelected()))
            .thenComparingInt(entry -> entry.getSelected().getRequiredLevel())
            .thenComparing(MethodListEntry::getDisplayName));

        applyEntryList(sorted);
    }

    private void updateProfitSortedEntries(boolean userTriggered)
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

        // Estimate every variant, not just the selected ones, so the variant dropdown can show
        // per-bone/per-variant net gp. One price fetch covers everything.
        List<MethodListEntry> visible = visibleEntries();
        List<SkillingMethod> methodsToEstimate = new ArrayList<>();
        for (MethodListEntry entry : visible)
        {
            methodsToEstimate.addAll(entry.getVariants());
        }

        showStatus("Fetching prices to sort by estimated profit…");
        plugin.estimateNetForMethods(
            methodsToEstimate,
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

                List<MethodListEntry> sorted = visibleEntries();
                sorted.sort((left, right) -> {
                    long leftNet = estimates.getOrDefault(left.getSelected(), Long.MIN_VALUE);
                    long rightNet = estimates.getOrDefault(right.getSelected(), Long.MIN_VALUE);
                    int byNet = Long.compare(rightNet, leftNet);
                    if (byNet != 0)
                    {
                        return byNet;
                    }
                    return left.getDisplayName().compareTo(right.getDisplayName());
                });

                applyEntryList(sorted);

                boolean hasGroups = false;
                for (MethodListEntry entry : sorted)
                {
                    if (entry.hasVariants())
                    {
                        hasGroups = true;
                        break;
                    }
                }
                if (hasGroups)
                {
                    // Red so the user notices: grouped rows are ranked by whichever variant is
                    // currently selected, not by their best variant.
                    boolean isPrayer = skillSelect.getSelectedItem() == Skill.PRAYER;
                    showStatusWarning(isPrayer
                        ? "Method list sorted by selected bone variant. Recalculate for the full breakdown."
                        : "Method list sorted by selected variant. Recalculate for the full breakdown.");
                }
                else
                {
                    showStatus("Sorted by estimated net. Recalculate the selected method for the full breakdown.");
                }
            },
            error -> {
                if (requestId != netSortRequestId)
                {
                    return;
                }
                showStatus(error);
            });
    }

    /** Entries that pass the "hide locked" filter (a group counts as unlocked if any variant is). */
    private List<MethodListEntry> visibleEntries()
    {
        commitSpinnerEdits();
        List<MethodListEntry> visible = new ArrayList<>();
        int level = ((Number) currentLevel.getValue()).intValue();
        for (MethodListEntry entry : currentEntries)
        {
            if (!hideLockedMethods.isSelected() || level >= entry.getMinRequiredLevel())
            {
                visible.add(entry);
            }
        }
        return visible;
    }

    private void applyEntryList(List<MethodListEntry> entries)
    {
        MethodListEntry previous = entryList.getSelectedValue();
        updatingSelection = true;
        entryListModel.clear();
        for (MethodListEntry entry : entries)
        {
            entryListModel.addElement(entry);
        }

        if (!entryListModel.isEmpty())
        {
            int index = previous == null ? -1 : entryListModel.indexOf(previous);
            entryList.setSelectedIndex(index >= 0 ? index : 0);
        }
        updatingSelection = false;
        updateVariantSelector();
        updateOptionState();
        entryList.repaint();
    }

    private SkillingMethod selectedMethod()
    {
        MethodListEntry entry = entryList.getSelectedValue();
        return entry == null ? null : entry.getSelected();
    }

    /** Syncs the variant dropdown with the selected list entry; hidden for standalone methods. */
    private void updateVariantSelector()
    {
        MethodListEntry entry = entryList.getSelectedValue();

        updatingVariants = true;
        DefaultComboBoxModel<SkillingMethod> model = new DefaultComboBoxModel<>();
        if (entry != null)
        {
            for (SkillingMethod variant : entry.getVariants())
            {
                model.addElement(variant);
            }
        }
        variantSelect.setModel(model);
        if (entry != null && model.getSize() > 0)
        {
            variantSelect.setSelectedIndex(entry.getSelectedVariantIndex());
        }
        updatingVariants = false;

        boolean showVariants = entry != null && entry.hasVariants();
        if (variantRow != null && variantRow.isVisible() != showVariants)
        {
            variantRow.setVisible(showVariants);
            revalidate();
            repaint();
        }
    }

    private void updateOptionState()
    {
        SkillingMethod method = selectedMethod();
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
                label.setText(Formats.skillName(value));
            }
            return label;
        });
    }

    private void configureEntryList()
    {
        entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryList.setVisibleRowCount(5);
        entryList.setFixedCellHeight(64);
        entryList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        entryList.setCellRenderer(new MethodCellRenderer());
        entryList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingSelection)
            {
                return;
            }
            includeOptionalInputs.setSelected(true);
            sellOutputs.setSelected(true);
            updateVariantSelector();
            updateOptionState();
            clearResult("Method changed. Recalculate when ready.");
        });
    }

    private void configureVariantSelect()
    {
        variantSelect.setAlignmentX(Component.LEFT_ALIGNMENT);
        variantSelect.setMaximumSize(new Dimension(Integer.MAX_VALUE, variantSelect.getPreferredSize().height));
        ListCellRenderer<? super SkillingMethod> baseRenderer = variantSelect.getRenderer();
        variantSelect.setRenderer((JList<? extends SkillingMethod> list, SkillingMethod value, int index, boolean isSelected, boolean cellHasFocus) -> {
            JLabel label = (JLabel) baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null)
            {
                int level = ((Number) currentLevel.getValue()).intValue();
                String text = value.getVariantName() == null ? value.getName() : value.getVariantName();
                // Show speed (actions) and, once a profit sort has fetched prices, cost per
                // variant - so fastest vs cheapest bone is visible at a glance.
                text += " | " + String.format("%,d", actionsFor(value)) + " actions";
                Long estimate = netEstimates.get(value);
                if (estimate != null)
                {
                    text += " | net " + Formats.gpCompact(estimate);
                }
                if (level < value.getRequiredLevel())
                {
                    text += " (needs lvl " + value.getRequiredLevel() + ")";
                }
                label.setText(text);
            }
            return label;
        });
        variantSelect.addActionListener(e -> {
            if (updatingVariants)
            {
                return;
            }
            MethodListEntry entry = entryList.getSelectedValue();
            int index = variantSelect.getSelectedIndex();
            if (entry == null || index < 0 || index == entry.getSelectedVariantIndex())
            {
                return;
            }
            entry.setSelectedVariantIndex(index);
            includeOptionalInputs.setSelected(true);
            sellOutputs.setSelected(true);
            updateOptionState();
            entryList.repaint();
            onInputsAffectingMethodListChanged("Variant changed. Recalculate when ready.");
        });
    }

    private JPanel methodListSection()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 315));
        panel.add(new JLabel("Method"), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(entryList);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        scrollPane.setPreferredSize(new Dimension(0, 280));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // --- XP loading and calculation ---

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
        final String skillName = Formats.skillName(skill);

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
            setTargetToNextLevelFromCurrent();
            syncSpinnerEditor(currentXp);
            syncSpinnerEditor(currentLevel);
            syncSpinnerEditor(targetLevel);
            syncSpinnerEditor(targetXp);
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

        SkillingMethod method = selectedMethod();
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

    // --- Spinner wiring ---

    private void wireXpLevelSync()
    {
        currentLevel.addChangeListener(e -> {
            if (syncingFields)
            {
                return;
            }
            syncingFields = true;
            setXpSpinner(currentXp, Experience.getXpForLevel(((Number) currentLevel.getValue()).intValue()));
            setTargetToNextLevelFromCurrent();
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
            setTargetToNextLevelFromCurrent();
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

    private void resetCurrentXpFields()
    {
        syncingFields = true;
        setLevelSpinner(currentLevel, 1);
        setXpSpinner(currentXp, 0);
        setTargetToNextLevelFromCurrent();
        syncSpinnerEditor(currentLevel);
        syncSpinnerEditor(currentXp);
        syncSpinnerEditor(targetLevel);
        syncSpinnerEditor(targetXp);
        syncingFields = false;
    }

    /** Commits pending text edits in all spinners; returns false if any value is invalid. */
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

    private void setTargetToNextLevelFromCurrent()
    {
        int nextLevel = Math.min(MAX_SKILL_LEVEL, ((Number) currentLevel.getValue()).intValue() + 1);
        setLevelSpinner(targetLevel, nextLevel);
        setXpSpinner(targetXp, Experience.getXpForLevel(nextLevel));
    }

    private static int levelForXp(int xp)
    {
        return Math.min(MAX_SKILL_LEVEL, Experience.getLevelForXp(Math.max(0, xp)));
    }

    // --- Small layout helpers ---

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
        divider.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        return divider;
    }

    private void clearResult(String message)
    {
        plugin.invalidatePendingCalculations();
        showStatus(message);
    }

    /**
     * Called when an input changed that may affect list ordering or the lock filter. Re-sorts
     * when needed; otherwise just repaints rows and asks the user to recalculate.
     */
    private void onInputsAffectingMethodListChanged(String message)
    {
        MethodSort sort = (MethodSort) sortSelect.getSelectedItem();
        if (sort != MethodSort.MOST_PROFITABLE)
        {
            // Profit sorting sets its own status when the fetch finishes.
            clearResult(message);
        }
        updateMethodOrdering(false);
    }

    private long actionsFor(SkillingMethod method)
    {
        int currentXpValue = ((Number) currentXp.getValue()).intValue();
        int targetXpValue = ((Number) targetXp.getValue()).intValue();
        int xpNeeded = Math.max(0, targetXpValue - currentXpValue);
        return (long) Math.ceil(xpNeeded / method.getXpPerAction());
    }

    /**
     * Renders one method row: a lock-state stripe on the left, the method/group name, and a
     * detail line with level, XP/action, and either the action count or the estimated net gp
     * (when profit sorting is active). Group rows describe their currently selected variant.
     */
    private final class MethodCellRenderer extends JPanel implements ListCellRenderer<MethodListEntry>
    {
        private final JPanel stripe = new JPanel();
        private final JLabel name = new JLabel();
        private final JLabel detail = new JLabel();
        // Separate label so only the gp amount is colored, not the whole detail line.
        private final JLabel netValue = new JLabel();

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
            netValue.setFont(detail.getFont());

            JPanel detailRow = new JPanel();
            detailRow.setOpaque(false);
            detailRow.setLayout(new BoxLayout(detailRow, BoxLayout.X_AXIS));
            detailRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailRow.add(detail);
            detailRow.add(netValue);

            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(name);
            textPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            textPanel.add(detailRow);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MethodListEntry> list, MethodListEntry value, int index, boolean isSelected, boolean cellHasFocus)
        {
            SkillingMethod method = value.getSelected();
            int level = ((Number) currentLevel.getValue()).intValue();
            boolean unlocked = level >= method.getRequiredLevel();
            long actions = actionsFor(method);

            setBackground(isSelected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 0, 2, 0),
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR)));
            stripe.setBackground(unlocked ? PROFIT : LOSS);

            String title = value.getDisplayName();
            if (value.hasVariants())
            {
                title += " (" + value.getVariants().size() + " options)";
            }
            name.setText(title);

            MethodSort sort = (MethodSort) sortSelect.getSelectedItem();
            String prefix = "Lvl. " + method.getRequiredLevel()
                + " (" + method.getXpPerAction() + " xp) | ";

            // Plain text only: HTML JLabels word-wrap when slightly too wide, which pushed some
            // net values onto an invisible second line. Compact gp keeps the line short.
            if (sort == MethodSort.MOST_PROFITABLE)
            {
                Long estimate = netEstimates.get(method);
                if (estimate == null)
                {
                    detail.setText(prefix + "net loading…");
                    netValue.setText("");
                }
                else
                {
                    detail.setText(prefix + "net ");
                    netValue.setForeground(estimate >= 0 ? PROFIT : LOSS);
                    netValue.setText(Formats.gpCompact(estimate));
                }
            }
            else
            {
                detail.setText(prefix + String.format("%,d", actions) + " actions");
                netValue.setText("");
            }

            return this;
        }
    }
}
