package com.skillingcost;

import net.runelite.api.Skill;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Small text-formatting helpers shared by the plugin and panel. */
final class Formats
{
    private Formats()
    {
    }

    /** Formats a gp amount with thousands separators, e.g. {@code -12,345 gp}. */
    static String gp(long value)
    {
        return String.format(Locale.US, "%,d gp", value);
    }

    /** Compact gp for narrow UI spots, e.g. {@code -82.3M gp}, {@code 15.2K gp}, {@code 950 gp}. */
    static String gpCompact(long value)
    {
        String sign = value < 0 ? "-" : "";
        double abs = Math.abs((double) value);
        if (abs >= 1_000_000_000_000L)
        {
            return sign + String.format(Locale.US, "%.2fT gp", abs / 1_000_000_000_000.0);
        }
        if (abs >= 1_000_000_000L)
        {
            return sign + String.format(Locale.US, "%.2fB gp", abs / 1_000_000_000.0);
        }
        if (abs >= 1_000_000L)
        {
            return sign + String.format(Locale.US, "%.1fM gp", abs / 1_000_000.0);
        }
        if (abs >= 10_000L)
        {
            return sign + String.format(Locale.US, "%.0fK gp", abs / 1_000.0);
        }
        return gp(value);
    }

    static String gpPerXp(long value, int xp)
    {
        if (xp <= 0) { throw new IllegalArgumentException("XP must be positive"); }
        BigDecimal ratio = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(xp), 2, RoundingMode.HALF_UP);
        return ratio.abs().compareTo(BigDecimal.valueOf(10_000)) >= 0
            ? gpCompact(ratio.longValue()) : String.format(Locale.US, "%,.2f", ratio);
    }

    /** {@code Skill.RUNECRAFT} -&gt; {@code "Runecraft"}. */
    static String skillName(Skill skill)
    {
        String text = skill.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (nextUpper && Character.isLetter(c))
            {
                out.append(Character.toUpperCase(c));
                nextUpper = false;
            }
            else
            {
                out.append(c);
            }
            if (c == ' ')
            {
                nextUpper = true;
            }
        }
        return out.toString();
    }

    /** Three display lines for one priced item: quantity x name, unit price, line total. */
    static String itemLine(String name, long quantity, long unitPrice, long total)
    {
        return String.format("%,d × %s", quantity, name)
            + "\n" + String.format("%,d gp each", unitPrice)
            + "\n" + gp(total) + " total";
    }
}
