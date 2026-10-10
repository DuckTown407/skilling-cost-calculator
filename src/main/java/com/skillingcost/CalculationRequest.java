package com.skillingcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Values captured on the EDT; workers never read mutable panel controls. */
final class CalculationRequest
{
    final long revision;
    final List<SkillingMethod> methods;
    final String selectedId;
    final int currentXp;
    final int targetXp;
    final int currentLevel;
    final int targetLevel;
    final int batchSize;
    final boolean optional;
    final boolean sell;
    final boolean tax;
    final boolean refresh;

    CalculationRequest(long revision, List<SkillingMethod> methods, String selectedId,
        int currentXp, int targetXp, int currentLevel, int targetLevel, int batchSize,
        boolean optional, boolean sell, boolean tax, boolean refresh)
    {
        this.revision = revision;
        this.methods = Collections.unmodifiableList(new ArrayList<>(methods));
        this.selectedId = selectedId;
        this.currentXp = currentXp;
        this.targetXp = targetXp;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.batchSize = batchSize;
        this.optional = optional;
        this.sell = sell;
        this.tax = tax;
        this.refresh = refresh;
    }

    Set<Integer> priceIds()
    {
        Set<Integer> ids = new LinkedHashSet<>();
        if (targetXp <= currentXp) { return ids; }
        for (SkillingMethod method : methods)
        {
            ids.addAll(method.getPriceItemIds(optional, sell));
        }
        return ids;
    }
}
