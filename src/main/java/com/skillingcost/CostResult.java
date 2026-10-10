package com.skillingcost;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CostResult
{
    enum State { AVAILABLE, MISSING_QUOTES, OUT_OF_RANGE, INVALID_SETTINGS }

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
    private final long oldestPriceTimestamp;
    private final Instant snapshotTime;
    private final State state;
    private final List<String> notices;

    CostResult(SkillingMethod method, int currentLevel, int currentXp, int targetLevel, int targetXp,
        int xpNeeded, long actionsNeeded, long inputCost, long outputGross, long tax, long net,
        List<String> requiredLines, List<String> optionalLines, List<String> outputLines,
        boolean optional, boolean sell, long oldestTimestamp, Instant snapshotTime, State state, List<String> notices)
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
        this.requiredInputLines = immutable(requiredLines);
        this.optionalInputLines = immutable(optionalLines);
        this.outputLines = immutable(outputLines);
        this.includeOptionalInputs = optional;
        this.sellOutputs = sell;
        this.oldestPriceTimestamp = oldestTimestamp;
        this.snapshotTime = snapshotTime;
        this.state = state;
        this.notices = immutable(notices);
    }

    private static List<String> immutable(List<String> values)
    {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    SkillingMethod getMethod() { return method; }
    int getCurrentLevel() { return currentLevel; }
    int getCurrentXp() { return currentXp; }
    int getTargetLevel() { return targetLevel; }
    int getTargetXp() { return targetXp; }
    int getXpNeeded() { return xpNeeded; }
    long getActionsNeeded() { return actionsNeeded; }
    long getInputCost() { return inputCost; }
    long getOutputGross() { return outputGross; }
    long getTax() { return tax; }
    long getNet() { return net; }
    List<String> getRequiredInputLines() { return requiredInputLines; }
    List<String> getOptionalInputLines() { return optionalInputLines; }
    List<String> getOutputLines() { return outputLines; }
    boolean isIncludeOptionalInputs() { return includeOptionalInputs; }
    boolean isSellOutputs() { return sellOutputs; }
    long getOldestPriceTimestamp() { return oldestPriceTimestamp; }
    Instant getSnapshotTime() { return snapshotTime; }
    State getState() { return state; }
    boolean isAvailable() { return state == State.AVAILABLE; }
    List<String> getNotices() { return notices; }
}
