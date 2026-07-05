package com.skillingcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row in the method list. Either a standalone method, or a group of variants (e.g. every
 * bone offered at a gilded altar) shown as a single row with a variant dropdown.
 */
final class MethodListEntry
{
    private final String displayName;
    private final List<SkillingMethod> variants;
    private int selectedVariantIndex;

    private MethodListEntry(String displayName, List<SkillingMethod> variants)
    {
        this.displayName = displayName;
        this.variants = variants;
    }

    /** Collapses a flat method list into rows, preserving order of first appearance. */
    static List<MethodListEntry> build(List<SkillingMethod> methods)
    {
        Map<String, List<SkillingMethod>> byGroup = new LinkedHashMap<>();
        List<MethodListEntry> entries = new ArrayList<>();
        // Placeholder inserted into 'entries' where each group first appears, replaced below.
        Map<String, Integer> groupPositions = new LinkedHashMap<>();

        for (SkillingMethod method : methods)
        {
            String group = method.getGroupName();
            if (group == null)
            {
                entries.add(new MethodListEntry(method.getName(), Collections.singletonList(method)));
                continue;
            }
            List<SkillingMethod> variants = byGroup.get(group);
            if (variants == null)
            {
                variants = new ArrayList<>();
                byGroup.put(group, variants);
                groupPositions.put(group, entries.size());
                entries.add(null);
            }
            variants.add(method);
        }

        for (Map.Entry<String, Integer> position : groupPositions.entrySet())
        {
            entries.set(position.getValue(),
                new MethodListEntry(position.getKey(), byGroup.get(position.getKey())));
        }
        return entries;
    }

    String getDisplayName()
    {
        return displayName;
    }

    boolean hasVariants()
    {
        return variants.size() > 1;
    }

    List<SkillingMethod> getVariants()
    {
        return Collections.unmodifiableList(variants);
    }

    SkillingMethod getSelected()
    {
        return variants.get(selectedVariantIndex);
    }

    int getSelectedVariantIndex()
    {
        return selectedVariantIndex;
    }

    void setSelectedVariantIndex(int index)
    {
        if (index >= 0 && index < variants.size())
        {
            selectedVariantIndex = index;
        }
    }

    /** Lowest level requirement across variants; used by the "hide locked" filter. */
    int getMinRequiredLevel()
    {
        int min = Integer.MAX_VALUE;
        for (SkillingMethod method : variants)
        {
            min = Math.min(min, method.getRequiredLevel());
        }
        return min;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
