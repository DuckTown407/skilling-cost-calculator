package com.skillingcost;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Ephemeral goals and preferences, independent of control applicability. */
final class CalculatorSession
{
    static final int MAX_XP = 200_000_000;
    static final class Goal
    {
        int currentXp;
        int targetXp = Experience.getXpForLevel(2);
        boolean explicitTarget;
        boolean xpLoaded;
        long xpEditRevision;
        String selectedId;
        String search = "";
        final Map<String, String> variants = new HashMap<>();

        void current(int xp, boolean manual)
        {
            currentXp = xp;
            if (manual) { xpEditRevision++; }
            if (!explicitTarget)
            {
                int level = level(xp);
                targetXp = level == 99 ? xp : Experience.getXpForLevel(level + 1);
            }
        }

        void target(int xp)
        {
            targetXp = xp;
            explicitTarget = true;
        }

        boolean applyLoaded(int xp, long editRevision)
        {
            if (xp < 0 || xpEditRevision != editRevision) { return false; }
            current(Math.min(MAX_XP, xp), false);
            xpLoaded = true;
            return true;
        }
    }

    private final Map<Skill, Goal> goals = new EnumMap<>(Skill.class);
    boolean optional = true;
    boolean sell = true;
    boolean tax = true;
    int batchSize = 28;

    Goal goal(Skill skill) { return goals.computeIfAbsent(skill, ignored -> new Goal()); }
    static int level(int xp) { return Math.min(99, Experience.getLevelForXp(xp)); }
}
