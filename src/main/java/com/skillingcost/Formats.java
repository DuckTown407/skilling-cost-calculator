package com.skillingcost;

import net.runelite.api.Skill;

/** Small text-formatting helpers shared by the plugin and panel. */
final class Formats
{
    private Formats()
    {
    }

    /** Formats a gp amount with thousands separators, e.g. {@code -12,345 gp}. */
    static String gp(long value)
    {
        String sign = value < 0 ? "-" : "";
        return sign + String.format("%,d gp", Math.abs(value));
    }

    /** Compact gp for narrow UI spots, e.g. {@code -82.3M gp}, {@code 15.2K gp}, {@code 950 gp}. */
    static String gpCompact(long value)
    {
        String sign = value < 0 ? "-" : "";
        long abs = Math.abs(value);
        if (abs >= 1_000_000_000L)
        {
            return sign + String.format("%.2fB gp", abs / 1_000_000_000.0);
        }
        if (abs >= 1_000_000L)
        {
            return sign + String.format("%.1fM gp", abs / 1_000_000.0);
        }
        if (abs >= 10_000L)
        {
            return sign + String.format("%.0fK gp", abs / 1_000.0);
        }
        return sign + String.format("%,d gp", abs);
    }

    /** {@code Skill.RUNECRAFT} -&gt; {@code "Runecraft"}. */
    static String skillName(Skill skill)
    {
        String text = skill.name().toLowerCase().replace('_', ' ');
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
    static String itemLine(String name, long quantity, int unitPrice, long total)
    {
        return String.format("%,d × %s", quantity, name)
            + "\n" + String.format("%,d gp each", unitPrice)
            + "\n" + gp(total) + " total";
    }
}
