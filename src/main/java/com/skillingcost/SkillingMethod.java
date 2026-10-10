package com.skillingcost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Skill;

final class SkillingMethod
{
    private final Skill skill;
    private final String id;
    private final String name;
    private final String groupName;
    private final String variantName;
    private final int requiredLevel;
    private final double xpPerAction;
    private final List<ItemQuantity> requiredInputs;
    private final List<ItemQuantity> optionalInputs;
    private final List<ItemQuantity> outputs;
    private final String actionUnit;
    private final String source;
    private final String assumptions;
    private final int[] outputLevels;

    SkillingMethod(Skill skill, String name, String group, String variant, int level, double xp,
        List<ItemQuantity> inputs, List<ItemQuantity> optional, List<ItemQuantity> outputs)
    {
        this(skill, skill + ":" + name, name, group, variant, level, xp, inputs, optional, outputs,
            "action", "", "", new int[0]);
    }

    SkillingMethod(Skill skill, String id, String name, String group, String variant, int level, double xp,
        List<ItemQuantity> inputs, List<ItemQuantity> optional, List<ItemQuantity> outputs,
        String actionUnit, String source, String assumptions, int[] outputLevels)
    {
        if (skill == null || id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()
            || level < 1 || level > 99 || !Double.isFinite(xp) || xp <= 0
            || actionUnit == null || actionUnit.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid recipe: " + name);
        }
        this.skill = skill;
        this.id = id;
        this.name = name;
        this.groupName = group;
        this.variantName = variant;
        this.requiredLevel = level;
        this.xpPerAction = xp;
        this.requiredInputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.optionalInputs = Collections.unmodifiableList(new ArrayList<>(optional));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
        this.actionUnit = actionUnit;
        this.source = source == null ? "" : source;
        this.assumptions = assumptions == null ? "" : assumptions;
        this.outputLevels = outputLevels == null ? new int[0] : outputLevels.clone();
        int previous = level;
        for (int threshold : this.outputLevels)
        {
            if (threshold <= previous || threshold > 99)
            {
                throw new IllegalArgumentException("Invalid output threshold: " + name);
            }
            previous = threshold;
        }
    }

    Skill getSkill() { return skill; }
    String getId() { return id; }
    String getName() { return name; }
    String getGroupName() { return groupName; }
    String getVariantName() { return variantName; }
    int getRequiredLevel() { return requiredLevel; }
    double getXpPerAction() { return xpPerAction; }
    List<ItemQuantity> getRequiredInputs() { return requiredInputs; }
    List<ItemQuantity> getOptionalInputs() { return optionalInputs; }
    List<ItemQuantity> getOutputs() { return outputs; }
    String getActionUnit() { return actionUnit; }
    String getSource() { return source; }
    String getAssumptions() { return assumptions; }
    boolean hasSellableOutputs() { return outputs.stream().anyMatch(ItemQuantity::isGePriced); }
    boolean hasLevelDependentOutput() { return outputLevels.length > 0; }
    boolean usesBatches()
    {
        return requiredInputs.stream().anyMatch(ItemQuantity::isPerBatch)
            || optionalInputs.stream().anyMatch(ItemQuantity::isPerBatch);
    }

    int outputMultiplier(int level)
    {
        int multiplier = 1;
        for (int threshold : outputLevels)
        {
            if (level >= threshold)
            {
                multiplier++;
            }
        }
        return multiplier;
    }

    long actionsFor(int xpNeeded)
    {
        return xpNeeded <= 0 ? 0 : BigDecimal.valueOf(xpNeeded)
            .divide(BigDecimal.valueOf(xpPerAction), 0, RoundingMode.CEILING).longValueExact();
    }

    Set<Integer> getPriceItemIds(boolean optional, boolean sell)
    {
        Set<Integer> ids = new LinkedHashSet<>();
        addGeIds(ids, requiredInputs);
        if (optional) { addGeIds(ids, optionalInputs); }
        if (sell) { addGeIds(ids, outputs); }
        return ids;
    }

    private static void addGeIds(Set<Integer> ids, List<ItemQuantity> items)
    {
        for (ItemQuantity item : items)
        {
            if (item.isGePriced() && item.getQuantityPerAction().signum() > 0)
            {
                ids.add(item.getItemId());
            }
        }
    }

    @Override
    public String toString() { return name; }
}
