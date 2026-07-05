package com.skillingcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Skill;

/**
 * A single training method: what it consumes, what it produces, and how much XP one action gives.
 *
 * Method definitions live in {@code src/main/resources/methods.json} and are loaded by
 * {@link MethodRepository}. Related methods can share a {@code groupName} (e.g. all bones offered
 * at a gilded altar), in which case each one carries a {@code variantName} ("Dragon bones") and
 * the UI shows the group as a single row with a variant dropdown.
 */
final class SkillingMethod
{
    private final Skill skill;
    private final String name;
    /** Group shown as one row in the method list, or null for a standalone method. */
    private final String groupName;
    /** Label inside the group's variant dropdown, or null for a standalone method. */
    private final String variantName;
    private final int requiredLevel;
    private final double xpPerAction;
    private final List<ItemQuantity> requiredInputs;
    private final List<ItemQuantity> optionalInputs;
    private final List<ItemQuantity> outputs;

    SkillingMethod(
        Skill skill,
        String name,
        String groupName,
        String variantName,
        int requiredLevel,
        double xpPerAction,
        List<ItemQuantity> requiredInputs,
        List<ItemQuantity> optionalInputs,
        List<ItemQuantity> outputs)
    {
        this.skill = skill;
        this.name = name;
        this.groupName = groupName;
        this.variantName = variantName;
        this.requiredLevel = requiredLevel;
        this.xpPerAction = xpPerAction;
        this.requiredInputs = new ArrayList<>(requiredInputs);
        this.optionalInputs = new ArrayList<>(optionalInputs);
        this.outputs = new ArrayList<>(outputs);
    }

    Skill getSkill()
    {
        return skill;
    }

    String getName()
    {
        return name;
    }

    String getGroupName()
    {
        return groupName;
    }

    String getVariantName()
    {
        return variantName;
    }

    int getRequiredLevel()
    {
        return requiredLevel;
    }

    double getXpPerAction()
    {
        return xpPerAction;
    }

    List<ItemQuantity> getRequiredInputs()
    {
        return Collections.unmodifiableList(requiredInputs);
    }

    List<ItemQuantity> getOptionalInputs()
    {
        return Collections.unmodifiableList(optionalInputs);
    }

    List<ItemQuantity> getOutputs()
    {
        return Collections.unmodifiableList(outputs);
    }

    boolean hasOutputs()
    {
        return !outputs.isEmpty();
    }

    /** Comma-separated names of the optional inputs, for checkbox labels. */
    String getOptionalInputSummary()
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < optionalInputs.size(); i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }
            builder.append(optionalInputs.get(i).getName());
        }
        return builder.toString();
    }

    /**
     * Every GE item id whose price is needed to cost this method out. Items with a fixed gp
     * price (e.g. NPC fees) are excluded because they never hit the price API.
     */
    Set<Integer> getPriceItemIds(boolean includeOptionalInputs, boolean sellOutputs)
    {
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemQuantity input : requiredInputs)
        {
            if (!input.hasFixedUnitPrice())
            {
                ids.add(input.getItemId());
            }
        }
        if (includeOptionalInputs)
        {
            for (ItemQuantity input : optionalInputs)
            {
                if (!input.hasFixedUnitPrice())
                {
                    ids.add(input.getItemId());
                }
            }
        }
        if (sellOutputs)
        {
            for (ItemQuantity output : outputs)
            {
                if (!output.hasFixedUnitPrice())
                {
                    ids.add(output.getItemId());
                }
            }
        }
        return ids;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
