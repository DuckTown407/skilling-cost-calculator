package com.skillingcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One row ranks its selected eligible variant. */
final class MethodListEntry
{
    private final String displayName;
    private final List<SkillingMethod> variants;
    private int selectedVariantIndex;

    private MethodListEntry(String name, List<SkillingMethod> variants)
    {
        this.displayName = name;
        this.variants = Collections.unmodifiableList(new ArrayList<>(variants));
    }

    static List<MethodListEntry> build(List<SkillingMethod> methods)
    {
        Map<String, List<SkillingMethod>> groups = new LinkedHashMap<>();
        for (SkillingMethod method : methods)
        {
            String key = method.getGroupName() == null ? method.getId() : method.getGroupName();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(method);
        }
        List<MethodListEntry> result = new ArrayList<>();
        for (List<SkillingMethod> group : groups.values())
        {
            SkillingMethod first = group.get(0);
            result.add(new MethodListEntry(first.getGroupName() == null ? first.getName() : first.getGroupName(), group));
        }
        return result;
    }

    MethodListEntry filtered(String search, int level, boolean hideLocked, String preferredId)
    {
        String query = search.trim().toLowerCase(Locale.ROOT);
        boolean groupMatch = displayName.toLowerCase(Locale.ROOT).contains(query);
        List<SkillingMethod> eligible = new ArrayList<>();
        for (SkillingMethod method : variants)
        {
            boolean match = groupMatch || method.getName().toLowerCase(Locale.ROOT).contains(query)
                || (method.getVariantName() != null && method.getVariantName().toLowerCase(Locale.ROOT).contains(query));
            if (match && (!hideLocked || method.getRequiredLevel() <= level)) { eligible.add(method); }
        }
        if (eligible.isEmpty()) { return null; }
        MethodListEntry result = new MethodListEntry(displayName, eligible);
        for (int i = 0; i < eligible.size(); i++)
        {
            if (eligible.get(i).getId().equals(preferredId)) { result.selectedVariantIndex = i; }
        }
        return result;
    }

    String getDisplayName() { return displayName; }
    boolean hasVariants() { return variants.size() > 1; }
    List<SkillingMethod> getVariants() { return variants; }
    SkillingMethod getSelected() { return variants.get(selectedVariantIndex); }
    int getSelectedVariantIndex() { return selectedVariantIndex; }
    void setSelectedVariantIndex(int index)
    {
        if (index >= 0 && index < variants.size()) { selectedVariantIndex = index; }
    }
    @Override
    public String toString() { return displayName; }
}
