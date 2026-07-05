package com.skillingcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CostResult
{
    private final SkillingMethod method;
    private final int currentLevel;
    private final int currentXp;
    private final int targetLevel;
    private final int targetXp;
    private final int xpNeeded;
    private final long actionsNeeded;
    private final long inputCost;
    private final long outputGross;
    private final long tax;
    private final long net;
    private final List<String> requiredInputLines;
    private final List<String> optionalInputLines;
    private final List<String> outputLines;
    private final boolean includeOptionalInputs;
    private final boolean sellOutputs;
    private final long newestPriceTimestamp;

    CostResult(
        SkillingMethod method,
        int currentLevel,
        int currentXp,
        int targetLevel,
        int targetXp,
        int xpNeeded,
        long actionsNeeded,
        long inputCost,
        long outputGross,
        long tax,
        long net,
        List<String> requiredInputLines,
        List<String> optionalInputLines,
        List<String> outputLines,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        long newestPriceTimestamp
    )
    {
        this.method = method;
        this.currentLevel = currentLevel;
        this.currentXp = currentXp;
        this.targetLevel = targetLevel;
        this.targetXp = targetXp;
        this.xpNeeded = xpNeeded;
        this.actionsNeeded = actionsNeeded;
        this.inputCost = inputCost;
        this.outputGross = outputGross;
        this.tax = tax;
        this.net = net;
        this.requiredInputLines = new ArrayList<>(requiredInputLines);
        this.optionalInputLines = new ArrayList<>(optionalInputLines);
        this.outputLines = new ArrayList<>(outputLines);
        this.includeOptionalInputs = includeOptionalInputs;
        this.sellOutputs = sellOutputs;
        this.newestPriceTimestamp = newestPriceTimestamp;
    }

    SkillingMethod getMethod()
    {
        return method;
    }

    int getCurrentLevel()
    {
        return currentLevel;
    }

    int getCurrentXp()
    {
        return currentXp;
    }

    int getTargetLevel()
    {
        return targetLevel;
    }

    int getTargetXp()
    {
        return targetXp;
    }

    int getXpNeeded()
    {
        return xpNeeded;
    }

    long getActionsNeeded()
    {
        return actionsNeeded;
    }

    long getInputCost()
    {
        return inputCost;
    }

    long getOutputGross()
    {
        return outputGross;
    }

    long getTax()
    {
        return tax;
    }

    long getNet()
    {
        return net;
    }

    List<String> getRequiredInputLines()
    {
        return Collections.unmodifiableList(requiredInputLines);
    }

    List<String> getOptionalInputLines()
    {
        return Collections.unmodifiableList(optionalInputLines);
    }

    List<String> getOutputLines()
    {
        return Collections.unmodifiableList(outputLines);
    }

    boolean isIncludeOptionalInputs()
    {
        return includeOptionalInputs;
    }

    boolean isSellOutputs()
    {
        return sellOutputs;
    }

    long getNewestPriceTimestamp()
    {
        return newestPriceTimestamp;
    }
}
