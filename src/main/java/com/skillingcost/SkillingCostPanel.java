package com.skillingcost;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** All mutable session state and completion checks belong to the Swing EDT. */
final class SkillingCostPanel extends PluginPanel
{
    static final Color TEXT = new Color(220, 220, 220);
    static final Color SUBTEXT = new Color(170, 170, 170);
    static final Color PROFIT = new Color(0, 180, 75);
    static final Color LOSS = new Color(190, 55, 55);

    enum MethodSort
    {
        LEAST_ACTIONS("Least actions"), MOST_PROFITABLE("Most profitable");
        private final String label;
        MethodSort(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final CalculatorController controller;
    private final Map<Skill, List<SkillingMethod>> methods;
    final CalculatorSession session = new CalculatorSession();
    final JComboBox<Skill> skillSelect;
    final JComboBox<MethodSort> sortSelect = new JComboBox<>(MethodSort.values());
    final JTextField search = new JTextField();
    final JCheckBox hideLocked = new JCheckBox("Hide locked methods");
    final JSpinner currentLevel = spinner(1, 1, 99);
    final JSpinner currentXp = spinner(0, 0, CalculatorSession.MAX_XP);
    final JSpinner targetLevel = spinner(2, 1, 99);
    final JSpinner targetXp = spinner(Experience.getXpForLevel(2), 0, CalculatorSession.MAX_XP);
    final JSpinner batchSize = spinner(28, 1, 1000);
    final JCheckBox optional = new JCheckBox("<html>Include optional<br>consumables</html>", true);
    final JCheckBox sell = new JCheckBox("Sell outputs", true);
    final JCheckBox tax = new JCheckBox("Include GE tax", true);
    final JButton loadXp = new JButton("Load current XP");
    final JButton refresh = new JButton("Refresh prices");
    static final class MethodListModel extends DefaultListModel<MethodListEntry>
    {
        void widthChanged()
        {
            if (!isEmpty()) { fireContentsChanged(this, 0, size() - 1); }
        }
    }

    final MethodListModel rows = new MethodListModel();
    final JList<MethodListEntry> list = new JList<MethodListEntry>(rows)
    {
        @Override
        public boolean getScrollableTracksViewportWidth() { return true; }

        @Override
        public void setBounds(int x, int y, int width, int height)
        {
            boolean widthChanged = width != getWidth();
            super.setBounds(x, y, width, height);
            // Wrapped cell heights must be measured again after the viewport width changes.
            if (widthChanged) { rows.widthChanged(); }
        }
    };
    private final JPanel rowRenderer = new JPanel(new BorderLayout(0, 4))
    {
        @Override
        public Dimension getPreferredSize()
        {
            Dimension size = super.getPreferredSize();
            size.height = Math.max(64, size.height);
            return size;
        }
    };
    private final javax.swing.JTextArea rowName = rowText();
    private final javax.swing.JTextArea rowDescription = rowText();
    final JComboBox<SkillingMethod> variants = new JComboBox<>();
    final ResultRenderer renderer = new ResultRenderer();
    private final JLabel empty = new JLabel("No matching methods.");
    private final JLabel ranking = new JLabel("<html>Rows rank their selected<br>variant.</html>");
    private final JPanel variantRow;
    private final JPanel batchRow;
    private final EstimateScheduler scheduler;
    private Map<String, CostResult> estimates = Collections.emptyMap();
    private Skill skill;
    private boolean syncing;
    private boolean disposed;
    private boolean validInputs = true;
    private long revision;
    private long xpLoadRevision;

    SkillingCostPanel(CalculatorController controller, MethodRepository repository)
    {
        this(controller, repository, EstimateScheduler::swing);
    }

    SkillingCostPanel(CalculatorController controller, MethodRepository repository,
        Function<Runnable, EstimateScheduler> scheduling)
    {
        super(false);
        this.controller = controller;
        methods = repository.getMethodsBySkill();
        skillSelect = new JComboBox<>(repository.getSupportedSkills().toArray(new Skill[0]));
        javax.swing.DefaultListCellRenderer skillsRenderer = new javax.swing.DefaultListCellRenderer();
        skillSelect.setRenderer((view, value, index, selected, focus) ->
            skillsRenderer.getListCellRendererComponent(view, value == null ? "" : Formats.skillName(value), index, selected, focus));
        javax.swing.DefaultListCellRenderer variantsRenderer = new javax.swing.DefaultListCellRenderer();
        variants.setRenderer((view, value, index, selected, focus) -> {
            String name = value == null ? "" : value.getVariantName() == null ? value.getName() : value.getVariantName();
            Component component = variantsRenderer.getListCellRendererComponent(view, name, index, selected, focus);
            ((JLabel) component).setToolTipText(value == null ? null : value.getName());
            return component;
        });
        scheduler = scheduling.apply(() -> estimate(false));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(labeled("Skill", skillSelect));
        content.add(gap(8));
        content.add(pair("Current level", currentLevel, "Current XP", currentXp));
        content.add(gap(8));
        content.add(pair("Target level", targetLevel, "Target XP", targetXp));
        content.add(gap(8));
        content.add(loadXp);
        sectionBreak(content);
        content.add(labeled("Search methods / variants", search));
        content.add(gap(8));
        content.add(labeled("Sort", sortSelect));
        content.add(gap(6));
        content.add(hideLocked);
        content.add(gap(4));
        ranking.setForeground(SUBTEXT);
        content.add(ranking);
        content.add(gap(8));
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        rowRenderer.add(rowName, BorderLayout.NORTH);
        rowRenderer.add(rowDescription, BorderLayout.CENTER);
        rowRenderer.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        rowName.setFont(rowName.getFont().deriveFont(rowName.getFont().getSize2D() + 1));
        rowDescription.setFont(rowDescription.getFont().deriveFont(rowDescription.getFont().getSize2D() - 1));
        list.setCellRenderer((view, row, index, selected, focus) -> renderRow(row, selected));
        JScrollPane methodScroll = new JScrollPane(list);
        methodScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        methodScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        methodScroll.setPreferredSize(new Dimension(200, 280));
        methodScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        JPanel methodSection = labeled("Method", methodScroll);
        methodSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, methodSection.getPreferredSize().height));
        content.add(methodSection);
        content.add(empty);
        content.add(gap(8));
        variantRow = labeled("Variant", variants);
        variantRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        content.add(variantRow);
        batchRow = labeled("Essence per batch", batchSize);
        batchRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        content.add(batchRow);
        sectionBreak(content);
        content.add(optional);
        content.add(gap(6));
        content.add(sell);
        content.add(gap(6));
        content.add(tax);
        sectionBreak(content);
        content.add(refresh);
        content.add(gap(10));
        content.add(renderer.getComponent());
        for (Component component : content.getComponents())
        {
            if (component instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        skillSelect.addActionListener(event -> switchSkill());
        sortSelect.addActionListener(event -> changed(true));
        hideLocked.addActionListener(event -> changed(true));
        listen(search, () -> {
            if (syncing || disposed) { return; }
            goal().search = search.getText();
            changed(true);
        });
        for (JSpinner field : new JSpinner[]{currentLevel, currentXp, targetLevel, targetXp})
        {
            boolean current = field == currentLevel || field == currentXp;
            boolean level = field == currentLevel || field == targetLevel;
            field.addChangeListener(event -> edit(field, current, level));
            listen(editor(field), () -> edit(field, current, level));
        }
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !syncing && !disposed)
            {
                rememberSelection();
                updateControls();
                changed(false);
            }
        });
        variants.addActionListener(event -> {
            if (syncing || disposed || list.getSelectedValue() == null) { return; }
            list.getSelectedValue().setSelectedVariantIndex(variants.getSelectedIndex());
            rememberSelection();
            updateControls();
            changed(false);
        });
        optional.addActionListener(event -> { session.optional = optional.isSelected(); changed(false); });
        sell.addActionListener(event -> { session.sell = sell.isSelected(); changed(false); });
        tax.addActionListener(event -> { session.tax = tax.isSelected(); changed(false); });
        batchSize.addChangeListener(event -> editBatch());
        listen(editor(batchSize), this::editBatch);
        loadXp.addActionListener(event -> loadCurrentXp());
        refresh.addActionListener(event -> { ++revision; scheduler.stop(); estimate(true); });
        switchSkill();
    }

    private CalculatorSession.Goal goal() { return session.goal(skill); }

    private void switchSkill()
    {
        if (disposed || syncing) { return; }
        skill = (Skill) skillSelect.getSelectedItem();
        ++xpLoadRevision;
        syncFields();
        changed(true);
        if (!goal().xpLoaded) { loadCurrentXp(); }
    }

    private void syncFields()
    {
        syncing = true;
        currentXp.setValue(goal().currentXp);
        currentLevel.setValue(CalculatorSession.level(goal().currentXp));
        targetXp.setValue(goal().targetXp);
        targetLevel.setValue(CalculatorSession.level(goal().targetXp));
        search.setText(goal().search);
        syncing = false;
        validInputs = true;
    }

    private void edit(JSpinner field, boolean current, boolean level)
    {
        if (syncing || disposed || skill == null) { return; }
        ++revision;
        estimates = Collections.emptyMap();
        scheduler.stop();
        goal().xpEditRevision++;
        try
        {
            int value = parsed(field);
            int xp = level ? Experience.getXpForLevel(value) : value;
            if (current) { goal().current(xp, false); }
            else { goal().target(xp); }
            syncing = true;
            (current ? (level ? currentXp : currentLevel) : (level ? targetXp : targetLevel))
                .setValue(level ? xp : CalculatorSession.level(xp));
            if (current && !goal().explicitTarget)
            {
                targetXp.setValue(goal().targetXp);
                targetLevel.setValue(CalculatorSession.level(goal().targetXp));
            }
            syncing = false;
            validateInputs();
            validInputs = true;
            rebuildRows();
            renderer.showStatus("Updating estimate…");
            scheduler.restart();
        }
        catch (ParseException | IllegalArgumentException exception)
        {
            syncing = false;
            validInputs = false;
            renderer.showStatus("Enter valid levels (1–99) and XP (0–200,000,000). Displayed result uses previous inputs.");
        }
    }

    private static int parsed(JSpinner spinner) throws ParseException
    {
        JFormattedTextField field = editor(spinner);
        Object parsed = field.getFormatter().stringToValue(field.getText());
        if (!(parsed instanceof Number)) { throw new ParseException("Invalid number", 0); }
        double value = ((Number) parsed).doubleValue();
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        int minimum = ((Number) model.getMinimum()).intValue();
        int maximum = ((Number) model.getMaximum()).intValue();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < minimum || value > maximum)
        {
            throw new ParseException("Out of range", 0);
        }
        return (int) value;
    }

    private void validateInputs() throws ParseException
    {
        for (JSpinner candidate : new JSpinner[]{currentLevel, currentXp, targetLevel, targetXp, batchSize}) { parsed(candidate); }
    }

    private void editBatch()
    {
        if (syncing || disposed) { return; }
        ++revision;
        estimates = Collections.emptyMap();
        scheduler.stop();
        try
        {
            session.batchSize = parsed(batchSize);
            validateInputs();
            validInputs = true;
            changed(false);
        }
        catch (ParseException | IllegalArgumentException exception)
        {
            validInputs = false;
            renderer.showStatus("Enter a batch size from 1 to 1,000. Displayed result uses previous inputs.");
        }
        updateControls();
    }

    private void loadCurrentXp()
    {
        Skill requestedSkill = skill;
        CalculatorSession.Goal requestedGoal = goal();
        long edit = requestedGoal.xpEditRevision;
        long loadRevision = ++xpLoadRevision;
        controller.loadCurrentSkillXp(requestedSkill, xp -> {
            if (disposed || loadRevision != xpLoadRevision || skill != requestedSkill || goal() != requestedGoal) { return; }
            if (xp < 0)
            {
                renderer.showStatus("Log in to load XP, or enter it manually.");
                return;
            }
            if (!requestedGoal.applyLoaded(xp, edit)) { return; }
            syncFields();
            changed(true);
        });
    }

    private void changed(boolean rebuild)
    {
        if (syncing || disposed) { return; }
        ++revision;
        estimates = Collections.emptyMap();
        if (rebuild) { rebuildRows(); }
        else { list.repaint(); }
        scheduler.stop();
        if (validInputs)
        {
            renderer.showStatus(rows.isEmpty() ? "No matching methods." : "Updating estimate…");
            scheduler.restart();
        }
    }

    private void rebuildRows()
    {
        String selectedId = goal().selectedId;
        List<MethodListEntry> visible = new ArrayList<>();
        for (MethodListEntry row : MethodListEntry.build(methods.get(skill)))
        {
            MethodListEntry filtered = row.filtered(goal().search, CalculatorSession.level(goal().currentXp),
                hideLocked.isSelected(), goal().variants.get(row.getDisplayName()));
            if (filtered != null) { visible.add(filtered); }
        }
        sortRows(visible);
        syncing = true;
        rows.clear();
        int selection = 0;
        for (int i = 0; i < visible.size(); i++)
        {
            MethodListEntry row = visible.get(i);
            rows.addElement(row);
            if (row.getSelected().getId().equals(selectedId)) { selection = i; }
        }
        if (!rows.isEmpty()) { list.setSelectedIndex(selection); }
        syncing = false;
        empty.setVisible(rows.isEmpty());
        rememberSelection();
        updateControls();
    }

    private void sortRows(List<MethodListEntry> visible)
    {
        Comparator<MethodListEntry> comparator;
        if (sortSelect.getSelectedItem() == MethodSort.MOST_PROFITABLE)
        {
            comparator = Comparator.comparing((MethodListEntry row) -> !available(row))
                .thenComparing((a, b) -> available(a) && available(b)
                    ? Long.compare(estimates.get(b.getSelected().getId()).getNet(), estimates.get(a.getSelected().getId()).getNet()) : 0);
        }
        else
        {
            comparator = Comparator.comparingLong(row -> row.getSelected().actionsFor(Math.max(0, goal().targetXp - goal().currentXp)));
        }
        visible.sort(comparator.thenComparing(MethodListEntry::getDisplayName));
    }

    private boolean available(MethodListEntry row)
    {
        CostResult result = estimates.get(row.getSelected().getId());
        return result != null && result.isAvailable();
    }

    private void rememberSelection()
    {
        MethodListEntry row = list.getSelectedValue();
        goal().selectedId = row == null ? null : row.getSelected().getId();
        if (row != null) { goal().variants.put(row.getDisplayName(), goal().selectedId); }
    }

    private void updateControls()
    {
        MethodListEntry row = list.getSelectedValue();
        SkillingMethod selected = row == null ? null : row.getSelected();
        syncing = true;
        variants.setModel(new DefaultComboBoxModel<>(row == null ? new SkillingMethod[0] : row.getVariants().toArray(new SkillingMethod[0])));
        if (row != null) { variants.setSelectedIndex(row.getSelectedVariantIndex()); }
        syncing = false;
        variantRow.setVisible(row != null && row.hasVariants());
        batchRow.setVisible(selected != null && selected.usesBatches());
        refresh.setEnabled(selected != null && validInputs);
    }

    void estimate(boolean forceRefresh)
    {
        if (disposed || !validInputs || list.getSelectedValue() == null) { return; }
        List<SkillingMethod> requested = new ArrayList<>();
        if (sortSelect.getSelectedItem() == MethodSort.MOST_PROFITABLE)
        {
            for (int i = 0; i < rows.size(); i++) { requested.addAll(rows.get(i).getVariants()); }
        }
        else { requested.add(list.getSelectedValue().getSelected()); }
        CalculationRequest request = new CalculationRequest(revision, requested, goal().selectedId,
            goal().currentXp, goal().targetXp, CalculatorSession.level(goal().currentXp),
            CalculatorSession.level(goal().targetXp), session.batchSize, session.optional, session.sell, session.tax, forceRefresh);
        renderer.showStatus(forceRefresh ? "Refreshing prices…" : "Estimating…");
        controller.estimate(request, results -> {
            if (disposed || revision != request.revision) { return; }
            estimates = results;
            rebuildRows();
            CostResult result = results.get(request.selectedId);
            if (result != null) { renderer.showResult(result); }
        }, message -> {
            if (disposed || revision != request.revision) { return; }
            estimates = Collections.emptyMap();
            list.repaint();
            renderer.showFailure(message);
        });
    }

    void pricesChanged() { changed(true); }
    void dispose() { disposed = true; ++revision; scheduler.stop(); estimates = Collections.emptyMap(); }

    private Component renderRow(MethodListEntry row, boolean selected)
    {
        SkillingMethod method = row.getSelected();
        rowRenderer.setBackground(selected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
        int width = Math.max(80, list.getWidth() > 0
            ? list.getWidth() - rowRenderer.getInsets().left - rowRenderer.getInsets().right : 180);
        rowName.setText(row.getDisplayName());
        rowName.setForeground(TEXT);
        String detail = "Lvl " + method.getRequiredLevel() + " · " + method.getXpPerAction() + " XP";
        CostResult result = estimates.get(method.getId());
        if (sortSelect.getSelectedItem() == MethodSort.MOST_PROFITABLE)
        {
            detail += " · " + (result == null ? "Net pending" : result.isAvailable() ? Formats.gpCompact(result.getNet()) : "Unavailable");
        }
        else { detail += " · " + String.format("%,d", method.actionsFor(Math.max(0, goal().targetXp - goal().currentXp))) + " actions"; }
        rowDescription.setText(detail);
        rowDescription.setForeground(CalculatorSession.level(goal().currentXp) < method.getRequiredLevel() ? LOSS : SUBTEXT);
        sizeRowText(rowName, width);
        sizeRowText(rowDescription, width);
        rowRenderer.setSize(width + rowRenderer.getInsets().left + rowRenderer.getInsets().right,
            rowRenderer.getPreferredSize().height);
        rowRenderer.doLayout();
        return rowRenderer;
    }

    private static javax.swing.JTextArea rowText()
    {
        javax.swing.JTextArea area = new javax.swing.JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(new JLabel().getFont());
        return area;
    }

    private static void sizeRowText(javax.swing.JTextArea area, int width)
    {
        area.setPreferredSize(null);
        area.setSize(new Dimension(width, Short.MAX_VALUE));
        Dimension size = area.getPreferredSize();
        size.width = width;
        area.setPreferredSize(size);
    }

    private static JSpinner spinner(int value, int min, int max)
    {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#,##0"));
        return spinner;
    }
    private static JFormattedTextField editor(JSpinner spinner) { return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField(); }
    private static void listen(JTextField field, Runnable action)
    {
        field.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent event) { action.run(); }
            public void removeUpdate(DocumentEvent event) { action.run(); }
            public void changedUpdate(DocumentEvent event) { action.run(); }
        });
    }
    private static JPanel labeled(String label, Component component)
    {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        return panel;
    }
    private static JPanel pair(String left, Component a, String right, Component b)
    {
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
        panel.setOpaque(false);
        panel.add(labeled(left, a));
        panel.add(labeled(right, b));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        return panel;
    }

    private static Component gap(int height)
    {
        return Box.createRigidArea(new Dimension(0, height));
    }

    private static void sectionBreak(JPanel content)
    {
        content.add(gap(10));
        JPanel divider = new JPanel();
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setMinimumSize(new Dimension(1, 1));
        divider.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        content.add(divider);
        content.add(gap(10));
    }
}
