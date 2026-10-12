package com.skillingcost;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("skilling-cost-calculator")
public interface SkillingCostConfig extends Config
{
    String DEFAULT_WIKI_USER_AGENT =
        "skilling-cost-calculator RuneLite plugin (contact: junk.ladybird471@proton.me)";

    @ConfigItem(
        keyName = "enableWikiPrices",
        name = "Enable Wiki prices",
        description = "Fetch live prices from the OSRS Wiki real-time price API.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 0
    )
    default boolean enableWikiPrices()
    {
        return true;
    }

    @ConfigItem(
        keyName = "wikiUserAgent",
        name = "Wiki User-Agent",
        description = "User-Agent sent to the OSRS Wiki price API.",
        position = 1
    )
    default String wikiUserAgent()
    {
        return DEFAULT_WIKI_USER_AGENT;
    }

    @ConfigItem(
        keyName = "taxBasisPoints",
        name = "GE tax basis points",
        description = "GE tax in basis points. 200 means 2.00%.",
        position = 2
    )
    default int taxBasisPoints()
    {
        return 200;
    }

    @ConfigItem(
        keyName = "taxCapGp",
        name = "GE tax cap",
        description = "Maximum GE tax per sold item.",
        position = 3
    )
    default int taxCapGp()
    {
        return 5_000_000;
    }

    @ConfigItem(
        keyName = "taxExemptBelowGp",
        name = "Tax exempt below GP",
        description = "No GE tax is applied to output items sold below this unit price.",
        position = 4
    )
    default int taxExemptBelowGp()
    {
        return 50;
    }
}
