package com.skillingcost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;

final class SkillingMethod
{
    private static final int AIR_RUNE = 556;
    private static final int WATER_RUNE = 555;
    private static final int EARTH_RUNE = 557;
    private static final int FIRE_RUNE = 554;
    private static final int BODY_RUNE = 559;
    private static final int COSMIC_RUNE = 564;
    private static final int NATURE_RUNE = 561;
    private static final int LAW_RUNE = 563;
    private static final int SOUL_RUNE = 566;
    private static final int ASTRAL_RUNE = 9075;
    private static final int PURE_ESSENCE = 7936;
    private static final int THREAD = 1734;
    private static final int BOW_STRING = 1777;
    private static final int FEATHER = 314;
    private static final int FISHING_BAIT = 313;

    static final List<SkillingMethod> ALL_METHODS = Collections.unmodifiableList(Arrays.asList(
        // Smithing - smelting
        m(Skill.SMITHING, "Smelt bronze bar", 1, 6.2,
            req(q(436, "Copper ore", 1), q(438, "Tin ore", 1)), none(), out(q(2349, "Bronze bar", 1))),
        m(Skill.SMITHING, "Smelt iron bar", 15, 12.5,
            req(q(440, "Iron ore", 1)), none(), out(q(2351, "Iron bar", 1))),
        m(Skill.SMITHING, "Smelt silver bar", 20, 13.7,
            req(q(442, "Silver ore", 1)), none(), out(q(2355, "Silver bar", 1))),
        m(Skill.SMITHING, "Smelt steel bar", 30, 17.5,
            req(q(440, "Iron ore", 1), q(453, "Coal", 2)), none(), out(q(2353, "Steel bar", 1))),
        m(Skill.SMITHING, "Smelt gold bar", 40, 22.5,
            req(q(444, "Gold ore", 1)), none(), out(q(2357, "Gold bar", 1))),
        m(Skill.SMITHING, "Smelt gold bar - goldsmith gauntlets", 40, 56.2,
            req(q(444, "Gold ore", 1)), none(), out(q(2357, "Gold bar", 1))),
        m(Skill.SMITHING, "Smelt mithril bar", 50, 30.0,
            req(q(447, "Mithril ore", 1), q(453, "Coal", 4)), none(), out(q(2359, "Mithril bar", 1))),
        m(Skill.SMITHING, "Smelt adamantite bar", 70, 37.5,
            req(q(449, "Adamantite ore", 1), q(453, "Coal", 6)), none(), out(q(2361, "Adamantite bar", 1))),
        m(Skill.SMITHING, "Smelt runite bar", 85, 50.0,
            req(q(451, "Runite ore", 1), q(453, "Coal", 8)), none(), out(q(2363, "Runite bar", 1))),
        m(Skill.SMITHING, "Cannonballs (buy bars)", 35, 25.6,
            req(q(2353, "Steel bar", 1)), none(), out(q(2, "Cannonball", 4))),

        // Smithing - platebodies
        m(Skill.SMITHING, "Bronze platebody (buy bars)", 18, 62.5,
            req(q(2349, "Bronze bar", 5)), none(), out(q(1117, "Bronze platebody", 1))),
        m(Skill.SMITHING, "Iron platebody (buy bars)", 33, 125.0,
            req(q(2351, "Iron bar", 5)), none(), out(q(1115, "Iron platebody", 1))),
        m(Skill.SMITHING, "Steel platebody (buy bars)", 48, 187.5,
            req(q(2353, "Steel bar", 5)), none(), out(q(1119, "Steel platebody", 1))),
        m(Skill.SMITHING, "Mithril platebody (buy bars)", 68, 250.0,
            req(q(2359, "Mithril bar", 5)), none(), out(q(1121, "Mithril platebody", 1))),
        m(Skill.SMITHING, "Adamant platebody (buy bars)", 88, 312.5,
            req(q(2361, "Adamantite bar", 5)), none(), out(q(1123, "Adamant platebody", 1))),
        m(Skill.SMITHING, "Rune platebody (buy bars)", 99, 375.0,
            req(q(2363, "Runite bar", 5)), none(), out(q(1127, "Rune platebody", 1))),

        // Smithing - dart tips
        m(Skill.SMITHING, "Bronze dart tips (buy bars)", 4, 12.5,
            req(q(2349, "Bronze bar", 1)), none(), out(q(819, "Bronze dart tip", 10))),
        m(Skill.SMITHING, "Iron dart tips (buy bars)", 19, 25.0,
            req(q(2351, "Iron bar", 1)), none(), out(q(820, "Iron dart tip", 10))),
        m(Skill.SMITHING, "Steel dart tips (buy bars)", 34, 37.5,
            req(q(2353, "Steel bar", 1)), none(), out(q(821, "Steel dart tip", 10))),
        m(Skill.SMITHING, "Mithril dart tips (buy bars)", 54, 50.0,
            req(q(2359, "Mithril bar", 1)), none(), out(q(822, "Mithril dart tip", 10))),
        m(Skill.SMITHING, "Adamant dart tips (buy bars)", 74, 62.5,
            req(q(2361, "Adamantite bar", 1)), none(), out(q(823, "Adamant dart tip", 10))),
        m(Skill.SMITHING, "Rune dart tips (buy bars)", 89, 75.0,
            req(q(2363, "Runite bar", 1)), none(), out(q(824, "Rune dart tip", 10))),

        // Smithing - make bars from ore, then smith the item
        m(Skill.SMITHING, "Cannonballs (smelt bars)", 35, 43.1,
            req(q(440, "Iron ore", 1), q(453, "Coal", 2)), none(), out(q(2, "Cannonball", 4))),

        m(Skill.SMITHING, "Bronze platebody (smelt bars)", 18, 93.5,
            req(q(436, "Copper ore", 5), q(438, "Tin ore", 5)), none(), out(q(1117, "Bronze platebody", 1))),
        m(Skill.SMITHING, "Iron platebody (smelt bars)", 33, 187.5,
            req(q(440, "Iron ore", 5)), none(), out(q(1115, "Iron platebody", 1))),
        m(Skill.SMITHING, "Steel platebody (smelt bars)", 48, 275.0,
            req(q(440, "Iron ore", 5), q(453, "Coal", 10)), none(), out(q(1119, "Steel platebody", 1))),
        m(Skill.SMITHING, "Mithril platebody (smelt bars)", 68, 400.0,
            req(q(447, "Mithril ore", 5), q(453, "Coal", 20)), none(), out(q(1121, "Mithril platebody", 1))),
        m(Skill.SMITHING, "Adamant platebody (smelt bars)", 88, 500.0,
            req(q(449, "Adamantite ore", 5), q(453, "Coal", 30)), none(), out(q(1123, "Adamant platebody", 1))),
        m(Skill.SMITHING, "Rune platebody (smelt bars)", 99, 625.0,
            req(q(451, "Runite ore", 5), q(453, "Coal", 40)), none(), out(q(1127, "Rune platebody", 1))),

        m(Skill.SMITHING, "Bronze dart tips (smelt bars)", 4, 18.7,
            req(q(436, "Copper ore", 1), q(438, "Tin ore", 1)), none(), out(q(819, "Bronze dart tip", 10))),
        m(Skill.SMITHING, "Iron dart tips (smelt bars)", 19, 37.5,
            req(q(440, "Iron ore", 1)), none(), out(q(820, "Iron dart tip", 10))),
        m(Skill.SMITHING, "Steel dart tips (smelt bars)", 34, 55.0,
            req(q(440, "Iron ore", 1), q(453, "Coal", 2)), none(), out(q(821, "Steel dart tip", 10))),
        m(Skill.SMITHING, "Mithril dart tips (smelt bars)", 54, 80.0,
            req(q(447, "Mithril ore", 1), q(453, "Coal", 4)), none(), out(q(822, "Mithril dart tip", 10))),
        m(Skill.SMITHING, "Adamant dart tips (smelt bars)", 74, 100.0,
            req(q(449, "Adamantite ore", 1), q(453, "Coal", 6)), none(), out(q(823, "Adamant dart tip", 10))),
        m(Skill.SMITHING, "Rune dart tips (smelt bars)", 89, 125.0,
            req(q(451, "Runite ore", 1), q(453, "Coal", 8)), none(), out(q(824, "Rune dart tip", 10))),

        // Herblore - standard potions from unfinished potion + secondary
        m(Skill.HERBLORE, "Attack potion", 3, 25.0,
            req(q(91, "Guam potion (unf)", 1), q(221, "Eye of newt", 1)), none(), out(q(121, "Attack potion(3)", 1))),
        m(Skill.HERBLORE, "Antipoison", 5, 37.5,
            req(q(93, "Marrentill potion (unf)", 1), q(235, "Unicorn horn dust", 1)), none(), out(q(175, "Antipoison(3)", 1))),
        m(Skill.HERBLORE, "Strength potion", 12, 50.0,
            req(q(95, "Tarromin potion (unf)", 1), q(225, "Limpwurt root", 1)), none(), out(q(115, "Strength potion(3)", 1))),
        m(Skill.HERBLORE, "Serum 207", 15, 50.0,
            req(q(95, "Tarromin potion (unf)", 1), q(592, "Ashes", 1)), none(), out(q(3410, "Serum 207(3)", 1))),
        m(Skill.HERBLORE, "Restore potion", 22, 62.5,
            req(q(97, "Harralander potion (unf)", 1), q(223, "Red spiders' eggs", 1)), none(), out(q(127, "Restore potion(3)", 1))),
        m(Skill.HERBLORE, "Energy potion", 26, 67.5,
            req(q(97, "Harralander potion (unf)", 1), q(1975, "Chocolate dust", 1)), none(), out(q(3010, "Energy potion(3)", 1))),
        m(Skill.HERBLORE, "Defence potion", 30, 75.0,
            req(q(99, "Ranarr potion (unf)", 1), q(239, "White berries", 1)), none(), out(q(133, "Defence potion(3)", 1))),
        m(Skill.HERBLORE, "Agility potion", 34, 80.0,
            req(q(3002, "Toadflax potion (unf)", 1), q(2152, "Toad's legs", 1)), none(), out(q(3034, "Agility potion(3)", 1))),
        m(Skill.HERBLORE, "Combat potion", 36, 84.0,
            req(q(97, "Harralander potion (unf)", 1), q(9736, "Goat horn dust", 1)), none(), out(q(9741, "Combat potion(3)", 1))),
        m(Skill.HERBLORE, "Prayer potion", 38, 87.5,
            req(q(99, "Ranarr potion (unf)", 1), q(231, "Snape grass", 1)), none(), out(q(139, "Prayer potion(3)", 1))),
        m(Skill.HERBLORE, "Super attack", 45, 100.0,
            req(q(101, "Irit potion (unf)", 1), q(221, "Eye of newt", 1)), none(), out(q(145, "Super attack(3)", 1))),
        m(Skill.HERBLORE, "Superantipoison", 48, 106.3,
            req(q(101, "Irit potion (unf)", 1), q(235, "Unicorn horn dust", 1)), none(), out(q(181, "Superantipoison(3)", 1))),
        m(Skill.HERBLORE, "Fishing potion", 50, 112.5,
            req(q(103, "Avantoe potion (unf)", 1), q(231, "Snape grass", 1)), none(), out(q(151, "Fishing potion(3)", 1))),
        m(Skill.HERBLORE, "Super energy", 52, 117.5,
            req(q(103, "Avantoe potion (unf)", 1), q(2970, "Mort myre fungus", 1)), none(), out(q(3018, "Super energy(3)", 1))),
        m(Skill.HERBLORE, "Hunter potion", 53, 120.0,
            req(q(103, "Avantoe potion (unf)", 1), q(10111, "Kebbit teeth dust", 1)), none(), out(q(10000, "Hunter potion(3)", 1))),
        m(Skill.HERBLORE, "Super strength", 55, 125.0,
            req(q(105, "Kwuarm potion (unf)", 1), q(225, "Limpwurt root", 1)), none(), out(q(157, "Super strength(3)", 1))),
        m(Skill.HERBLORE, "Weapon poison", 60, 137.5,
            req(q(105, "Kwuarm potion (unf)", 1), q(241, "Dragon scale dust", 1)), none(), out(q(187, "Weapon poison", 1))),
        m(Skill.HERBLORE, "Super restore", 63, 142.5,
            req(q(3004, "Snapdragon potion (unf)", 1), q(223, "Red spiders' eggs", 1)), none(), out(q(3024, "Super restore(3)", 1))),
        m(Skill.HERBLORE, "Super defence", 66, 150.0,
            req(q(107, "Cadantine potion (unf)", 1), q(239, "White berries", 1)), none(), out(q(163, "Super defence(3)", 1))),
        m(Skill.HERBLORE, "Antifire potion", 69, 157.5,
            req(q(2483, "Lantadyme potion (unf)", 1), q(241, "Dragon scale dust", 1)), none(), out(q(2454, "Antifire potion(3)", 1))),
        m(Skill.HERBLORE, "Ranging potion", 72, 162.5,
            req(q(109, "Dwarf weed potion (unf)", 1), q(245, "Wine of zamorak", 1)), none(), out(q(169, "Ranging potion(3)", 1))),
        m(Skill.HERBLORE, "Magic potion", 76, 172.5,
            req(q(2483, "Lantadyme potion (unf)", 1), q(3138, "Potato cactus", 1)), none(), out(q(3042, "Magic potion(3)", 1))),
        m(Skill.HERBLORE, "Zamorak brew", 78, 175.0,
            req(q(111, "Torstol potion (unf)", 1), q(245, "Wine of zamorak", 1)), none(), out(q(189, "Zamorak brew(3)", 1))),
        m(Skill.HERBLORE, "Saradomin brew", 81, 180.0,
            req(q(3002, "Toadflax potion (unf)", 1), q(6693, "Crushed nest", 1)), none(), out(q(6685, "Saradomin brew(3)", 1))),
        m(Skill.HERBLORE, "Super combat potion", 90, 150.0,
            req(q(145, "Super attack(3)", 1), q(157, "Super strength(3)", 1), q(163, "Super defence(3)", 1), q(269, "Torstol", 1)), none(), out(q(12695, "Super combat potion(4)", 1))),

        // Fishing - bought consumables or zero-input catch value
        m(Skill.FISHING, "Net fishing - shrimps", 1, 10.0,
            none(), none(), out(q(317, "Raw shrimps", 1))),
        m(Skill.FISHING, "Bait fishing - sardine", 5, 20.0,
            req(q(FISHING_BAIT, "Fishing bait", 1)), none(), out(q(327, "Raw sardine", 1))),
        m(Skill.FISHING, "Bait fishing - herring", 10, 30.0,
            req(q(FISHING_BAIT, "Fishing bait", 1)), none(), out(q(345, "Raw herring", 1))),
        m(Skill.FISHING, "Fly fishing - trout", 20, 50.0,
            req(q(FEATHER, "Feather", 1)), none(), out(q(335, "Raw trout", 1))),
        m(Skill.FISHING, "Fly fishing - salmon", 30, 70.0,
            req(q(FEATHER, "Feather", 1)), none(), out(q(331, "Raw salmon", 1))),
        m(Skill.FISHING, "Lobster", 40, 90.0,
            none(), none(), out(q(377, "Raw lobster", 1))),
        m(Skill.FISHING, "Swordfish", 50, 100.0,
            none(), none(), out(q(371, "Raw swordfish", 1))),
        m(Skill.FISHING, "Monkfish", 62, 120.0,
            none(), none(), out(q(7944, "Raw monkfish", 1))),
        m(Skill.FISHING, "Raw karambwan", 65, 50.0,
            req(q(3150, "Raw karambwanji", 1)), none(), out(q(3142, "Raw karambwan", 1))),
        m(Skill.FISHING, "Shark", 76, 110.0,
            none(), none(), out(q(383, "Raw shark", 1))),
        m(Skill.FISHING, "Anglerfish", 82, 120.0,
            none(), none(), out(q(13439, "Raw anglerfish", 1))),
        m(Skill.FISHING, "Dark crab", 85, 130.0,
            req(q(FISHING_BAIT, "Fishing bait", 1)), none(), out(q(11934, "Raw dark crab", 1))),

        // Cooking - fish
        m(Skill.COOKING, "Cook shrimps", 1, 30.0,
            req(q(317, "Raw shrimps", 1)), none(), out(q(315, "Shrimps", 1))),
        m(Skill.COOKING, "Cook anchovies", 1, 30.0,
            req(q(321, "Raw anchovies", 1)), none(), out(q(319, "Anchovies", 1))),
        m(Skill.COOKING, "Cook sardine", 1, 40.0,
            req(q(327, "Raw sardine", 1)), none(), out(q(325, "Sardine", 1))),
        m(Skill.COOKING, "Cook herring", 5, 50.0,
            req(q(345, "Raw herring", 1)), none(), out(q(347, "Herring", 1))),
        m(Skill.COOKING, "Cook mackerel", 10, 60.0,
            req(q(353, "Raw mackerel", 1)), none(), out(q(355, "Mackerel", 1))),
        m(Skill.COOKING, "Cook trout", 15, 70.0,
            req(q(335, "Raw trout", 1)), none(), out(q(333, "Trout", 1))),
        m(Skill.COOKING, "Cook cod", 18, 75.0,
            req(q(341, "Raw cod", 1)), none(), out(q(339, "Cod", 1))),
        m(Skill.COOKING, "Cook pike", 20, 80.0,
            req(q(349, "Raw pike", 1)), none(), out(q(351, "Pike", 1))),
        m(Skill.COOKING, "Cook salmon", 25, 90.0,
            req(q(331, "Raw salmon", 1)), none(), out(q(329, "Salmon", 1))),
        m(Skill.COOKING, "Cook tuna", 30, 100.0,
            req(q(359, "Raw tuna", 1)), none(), out(q(361, "Tuna", 1))),
        m(Skill.COOKING, "Cook lobster", 40, 120.0,
            req(q(377, "Raw lobster", 1)), none(), out(q(379, "Lobster", 1))),
        m(Skill.COOKING, "Cook bass", 43, 130.0,
            req(q(363, "Raw bass", 1)), none(), out(q(365, "Bass", 1))),
        m(Skill.COOKING, "Cook swordfish", 45, 140.0,
            req(q(371, "Raw swordfish", 1)), none(), out(q(373, "Swordfish", 1))),
        m(Skill.COOKING, "Cook monkfish", 62, 150.0,
            req(q(7944, "Raw monkfish", 1)), none(), out(q(7946, "Monkfish", 1))),
        m(Skill.COOKING, "Cook karambwan", 30, 190.0,
            req(q(3142, "Raw karambwan", 1)), none(), out(q(3144, "Cooked karambwan", 1))),
        m(Skill.COOKING, "Cook shark", 80, 210.0,
            req(q(383, "Raw shark", 1)), none(), out(q(385, "Shark", 1))),
        m(Skill.COOKING, "Cook sea turtle", 82, 211.3,
            req(q(395, "Raw sea turtle", 1)), none(), out(q(397, "Sea turtle", 1))),
        m(Skill.COOKING, "Cook manta ray", 91, 216.3,
            req(q(389, "Raw manta ray", 1)), none(), out(q(391, "Manta ray", 1))),
        m(Skill.COOKING, "Cook dark crab", 90, 215.0,
            req(q(11934, "Raw dark crab", 1)), none(), out(q(11936, "Dark crab", 1))),
        m(Skill.COOKING, "Cook anglerfish", 84, 230.0,
            req(q(13439, "Raw anglerfish", 1)), none(), out(q(13441, "Anglerfish", 1))),

        // Prayer - bury/scatter and common altar multipliers
        m(Skill.PRAYER, "Bury bones", 1, 4.5,
            req(q(526, "Bones", 1)), none(), none()),
        m(Skill.PRAYER, "Bury big bones", 1, 15.0,
            req(q(532, "Big bones", 1)), none(), none()),
        m(Skill.PRAYER, "Bury baby dragon bones", 1, 30.0,
            req(q(534, "Baby dragon bones", 1)), none(), none()),
        m(Skill.PRAYER, "Bury dragon bones", 1, 72.0,
            req(q(536, "Dragon bones", 1)), none(), none()),
        m(Skill.PRAYER, "Bury wyvern bones", 1, 72.0,
            req(q(6812, "Wyvern bones", 1)), none(), none()),
        m(Skill.PRAYER, "Bury dagannoth bones", 1, 125.0,
            req(q(6729, "Dagannoth bones", 1)), none(), none()),
        m(Skill.PRAYER, "Bury superior dragon bones", 70, 150.0,
            req(q(22124, "Superior dragon bones", 1)), none(), none()),
        m(Skill.PRAYER, "Gilded altar - big bones", 1, 52.5,
            req(q(532, "Big bones", 1)), none(), none()),
        m(Skill.PRAYER, "Gilded altar - dragon bones", 1, 252.0,
            req(q(536, "Dragon bones", 1)), none(), none()),
        m(Skill.PRAYER, "Gilded altar - wyvern bones", 1, 252.0,
            req(q(6812, "Wyvern bones", 1)), none(), none()),
        m(Skill.PRAYER, "Gilded altar - dagannoth bones", 1, 437.5,
            req(q(6729, "Dagannoth bones", 1)), none(), none()),
        m(Skill.PRAYER, "Gilded altar - superior dragon bones", 70, 525.0,
            req(q(22124, "Superior dragon bones", 1)), none(), none()),
        m(Skill.PRAYER, "Scatter vile ashes", 1, 25.0,
            req(q(25766, "Vile ashes", 1)), none(), none()),
        m(Skill.PRAYER, "Scatter malicious ashes", 1, 65.0,
            req(q(25769, "Malicious ashes", 1)), none(), none()),
        m(Skill.PRAYER, "Scatter abyssal ashes", 1, 85.0,
            req(q(25775, "Abyssal ashes", 1)), none(), none()),
        m(Skill.PRAYER, "Scatter infernal ashes", 1, 110.0,
            req(q(25772, "Infernal ashes", 1)), none(), none()),

        // Crafting - leather and d'hide
        m(Skill.CRAFTING, "Leather gloves", 1, 13.8,
            req(q(1741, "Leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1059, "Leather gloves", 1))),
        m(Skill.CRAFTING, "Leather boots", 7, 16.3,
            req(q(1741, "Leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1061, "Leather boots", 1))),
        m(Skill.CRAFTING, "Leather cowl", 9, 18.5,
            req(q(1741, "Leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1167, "Leather cowl", 1))),
        m(Skill.CRAFTING, "Hardleather body", 10, 35.0,
            req(q(1743, "Hard leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1131, "Hardleather body", 1))),
        m(Skill.CRAFTING, "Leather vambraces", 11, 22.0,
            req(q(1741, "Leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1063, "Leather vambraces", 1))),
        m(Skill.CRAFTING, "Leather body", 14, 25.0,
            req(q(1741, "Leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1129, "Leather body", 1))),
        m(Skill.CRAFTING, "Leather chaps", 18, 27.0,
            req(q(1741, "Leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1095, "Leather chaps", 1))),
        m(Skill.CRAFTING, "Green d'hide vambraces", 57, 62.0,
            req(q(1745, "Green dragon leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(1065, "Green d'hide vambraces", 1))),
        m(Skill.CRAFTING, "Green d'hide chaps", 60, 124.0,
            req(q(1745, "Green dragon leather", 2)), opt(q(THREAD, "Thread", 0.2)), out(q(1099, "Green d'hide chaps", 1))),
        m(Skill.CRAFTING, "Green d'hide body", 63, 186.0,
            req(q(1745, "Green dragon leather", 3)), opt(q(THREAD, "Thread", 0.2)), out(q(1135, "Green d'hide body", 1))),
        m(Skill.CRAFTING, "Blue d'hide vambraces", 66, 70.0,
            req(q(2505, "Blue dragon leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(2487, "Blue d'hide vambraces", 1))),
        m(Skill.CRAFTING, "Blue d'hide chaps", 68, 140.0,
            req(q(2505, "Blue dragon leather", 2)), opt(q(THREAD, "Thread", 0.2)), out(q(2493, "Blue d'hide chaps", 1))),
        m(Skill.CRAFTING, "Blue d'hide body", 71, 210.0,
            req(q(2505, "Blue dragon leather", 3)), opt(q(THREAD, "Thread", 0.2)), out(q(2499, "Blue d'hide body", 1))),
        m(Skill.CRAFTING, "Red d'hide vambraces", 73, 78.0,
            req(q(2507, "Red dragon leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(2489, "Red d'hide vambraces", 1))),
        m(Skill.CRAFTING, "Red d'hide chaps", 75, 156.0,
            req(q(2507, "Red dragon leather", 2)), opt(q(THREAD, "Thread", 0.2)), out(q(2495, "Red d'hide chaps", 1))),
        m(Skill.CRAFTING, "Red d'hide body", 77, 234.0,
            req(q(2507, "Red dragon leather", 3)), opt(q(THREAD, "Thread", 0.2)), out(q(2501, "Red d'hide body", 1))),
        m(Skill.CRAFTING, "Black d'hide vambraces", 79, 86.0,
            req(q(2509, "Black dragon leather", 1)), opt(q(THREAD, "Thread", 0.2)), out(q(2491, "Black d'hide vambraces", 1))),
        m(Skill.CRAFTING, "Black d'hide chaps", 82, 172.0,
            req(q(2509, "Black dragon leather", 2)), opt(q(THREAD, "Thread", 0.2)), out(q(2497, "Black d'hide chaps", 1))),
        m(Skill.CRAFTING, "Black d'hide body", 84, 258.0,
            req(q(2509, "Black dragon leather", 3)), opt(q(THREAD, "Thread", 0.2)), out(q(2503, "Black d'hide body", 1))),

        // Crafting - gems and glassblowing
        m(Skill.CRAFTING, "Cut opal", 1, 15.0,
            req(q(1625, "Uncut opal", 1)), none(), out(q(1609, "Opal", 1))),
        m(Skill.CRAFTING, "Cut jade", 13, 20.0,
            req(q(1627, "Uncut jade", 1)), none(), out(q(1611, "Jade", 1))),
        m(Skill.CRAFTING, "Cut red topaz", 16, 25.0,
            req(q(1629, "Uncut red topaz", 1)), none(), out(q(1613, "Red topaz", 1))),
        m(Skill.CRAFTING, "Cut sapphire", 20, 50.0,
            req(q(1623, "Uncut sapphire", 1)), none(), out(q(1607, "Sapphire", 1))),
        m(Skill.CRAFTING, "Cut emerald", 27, 67.5,
            req(q(1621, "Uncut emerald", 1)), none(), out(q(1605, "Emerald", 1))),
        m(Skill.CRAFTING, "Cut ruby", 34, 85.0,
            req(q(1619, "Uncut ruby", 1)), none(), out(q(1603, "Ruby", 1))),
        m(Skill.CRAFTING, "Cut diamond", 43, 107.5,
            req(q(1617, "Uncut diamond", 1)), none(), out(q(1601, "Diamond", 1))),
        m(Skill.CRAFTING, "Cut dragonstone", 55, 137.5,
            req(q(1631, "Uncut dragonstone", 1)), none(), out(q(1615, "Dragonstone", 1))),
        m(Skill.CRAFTING, "Cut onyx", 67, 167.5,
            req(q(6571, "Uncut onyx", 1)), none(), out(q(6573, "Onyx", 1))),
        m(Skill.CRAFTING, "Cut zenyte", 89, 200.0,
            req(q(19496, "Uncut zenyte", 1)), none(), out(q(19493, "Zenyte", 1))),
        m(Skill.CRAFTING, "Blow beer glass", 1, 17.5,
            req(q(1775, "Molten glass", 1)), none(), out(q(1919, "Beer glass", 1))),
        m(Skill.CRAFTING, "Blow empty candle lantern", 4, 19.0,
            req(q(1775, "Molten glass", 1)), none(), out(q(4527, "Empty candle lantern", 1))),
        m(Skill.CRAFTING, "Blow oil lamp", 12, 25.0,
            req(q(1775, "Molten glass", 1)), none(), out(q(4522, "Oil lamp", 1))),
        m(Skill.CRAFTING, "Blow vial", 33, 35.0,
            req(q(1775, "Molten glass", 1)), none(), out(q(229, "Vial", 1))),
        m(Skill.CRAFTING, "Blow unpowered orb", 46, 52.5,
            req(q(1775, "Molten glass", 1)), none(), out(q(567, "Unpowered orb", 1))),
        m(Skill.CRAFTING, "Blow lantern lens", 49, 55.0,
            req(q(1775, "Molten glass", 1)), none(), out(q(4542, "Lantern lens", 1))),
        m(Skill.CRAFTING, "Blow light orb", 87, 70.0,
            req(q(1775, "Molten glass", 1)), none(), out(q(10973, "Light orb", 1))),

        // Crafting - battlestaves and jewelry
        m(Skill.CRAFTING, "Water battlestaff", 54, 100.0,
            req(q(1391, "Battlestaff", 1), q(571, "Water orb", 1)), none(), out(q(1395, "Water battlestaff", 1))),
        m(Skill.CRAFTING, "Earth battlestaff", 58, 112.5,
            req(q(1391, "Battlestaff", 1), q(575, "Earth orb", 1)), none(), out(q(1399, "Earth battlestaff", 1))),
        m(Skill.CRAFTING, "Fire battlestaff", 62, 125.0,
            req(q(1391, "Battlestaff", 1), q(569, "Fire orb", 1)), none(), out(q(1393, "Fire battlestaff", 1))),
        m(Skill.CRAFTING, "Air battlestaff", 66, 137.5,
            req(q(1391, "Battlestaff", 1), q(573, "Air orb", 1)), none(), out(q(1397, "Air battlestaff", 1))),
        m(Skill.CRAFTING, "Gold ring", 5, 15.0,
            req(q(2357, "Gold bar", 1)), none(), out(q(1635, "Gold ring", 1))),
        m(Skill.CRAFTING, "Gold necklace", 6, 20.0,
            req(q(2357, "Gold bar", 1)), none(), out(q(1654, "Gold necklace", 1))),
        m(Skill.CRAFTING, "Gold bracelet", 7, 25.0,
            req(q(2357, "Gold bar", 1)), none(), out(q(11069, "Gold bracelet", 1))),
        m(Skill.CRAFTING, "Gold amulet (u)", 8, 30.0,
            req(q(2357, "Gold bar", 1)), none(), out(q(1673, "Gold amulet (u)", 1))),
        m(Skill.CRAFTING, "Sapphire ring", 20, 40.0,
            req(q(2357, "Gold bar", 1), q(1607, "Sapphire", 1)), none(), out(q(1637, "Sapphire ring", 1))),
        m(Skill.CRAFTING, "Emerald ring", 27, 55.0,
            req(q(2357, "Gold bar", 1), q(1605, "Emerald", 1)), none(), out(q(1639, "Emerald ring", 1))),
        m(Skill.CRAFTING, "Ruby ring", 34, 70.0,
            req(q(2357, "Gold bar", 1), q(1603, "Ruby", 1)), none(), out(q(1641, "Ruby ring", 1))),
        m(Skill.CRAFTING, "Diamond ring", 43, 85.0,
            req(q(2357, "Gold bar", 1), q(1601, "Diamond", 1)), none(), out(q(1643, "Diamond ring", 1))),
        m(Skill.CRAFTING, "Dragonstone ring", 55, 100.0,
            req(q(2357, "Gold bar", 1), q(1615, "Dragonstone", 1)), none(), out(q(1645, "Dragonstone ring", 1))),
        m(Skill.CRAFTING, "Onyx ring", 67, 115.0,
            req(q(2357, "Gold bar", 1), q(6573, "Onyx", 1)), none(), out(q(6575, "Onyx ring", 1))),
        m(Skill.CRAFTING, "Zenyte ring", 89, 150.0,
            req(q(2357, "Gold bar", 1), q(19493, "Zenyte", 1)), none(), out(q(19538, "Zenyte ring", 1))),

        // Firemaking - logs
        m(Skill.FIREMAKING, "Burn logs", 1, 40.0,
            req(q(1511, "Logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn oak logs", 15, 60.0,
            req(q(1521, "Oak logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn willow logs", 30, 90.0,
            req(q(1519, "Willow logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn teak logs", 35, 105.0,
            req(q(6333, "Teak logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn maple logs", 45, 135.0,
            req(q(1517, "Maple logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn mahogany logs", 50, 157.5,
            req(q(6332, "Mahogany logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn yew logs", 60, 202.5,
            req(q(1515, "Yew logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn magic logs", 75, 303.8,
            req(q(1513, "Magic logs", 1)), none(), none()),
        m(Skill.FIREMAKING, "Burn redwood logs", 90, 350.0,
            req(q(19669, "Redwood logs", 1)), none(), none()),

        // Magic - common rune-cost methods. Staff substitutions are not modeled yet.
        m(Skill.MAGIC, "Curse", 19, 29.0,
            req(q(BODY_RUNE, "Body rune", 1), q(WATER_RUNE, "Water rune", 3), q(EARTH_RUNE, "Earth rune", 2)), none(), none()),
        m(Skill.MAGIC, "Varrock Teleport", 25, 35.0,
            req(q(LAW_RUNE, "Law rune", 1), q(AIR_RUNE, "Air rune", 3), q(FIRE_RUNE, "Fire rune", 1)), none(), none()),
        m(Skill.MAGIC, "Falador Teleport", 37, 48.0,
            req(q(LAW_RUNE, "Law rune", 1), q(AIR_RUNE, "Air rune", 3), q(WATER_RUNE, "Water rune", 1)), none(), none()),
        m(Skill.MAGIC, "Camelot Teleport", 45, 55.5,
            req(q(LAW_RUNE, "Law rune", 1), q(AIR_RUNE, "Air rune", 5)), none(), none()),
        m(Skill.MAGIC, "High Level Alchemy - rune cost only", 55, 65.0,
            req(q(NATURE_RUNE, "Nature rune", 1), q(FIRE_RUNE, "Fire rune", 5)), none(), none()),
        m(Skill.MAGIC, "Ardougne Teleport", 51, 61.0,
            req(q(LAW_RUNE, "Law rune", 2), q(WATER_RUNE, "Water rune", 2)), none(), none()),
        m(Skill.MAGIC, "Watchtower Teleport", 58, 68.0,
            req(q(LAW_RUNE, "Law rune", 2), q(EARTH_RUNE, "Earth rune", 2)), none(), none()),
        m(Skill.MAGIC, "Stun", 80, 90.0,
            req(q(SOUL_RUNE, "Soul rune", 1), q(BODY_RUNE, "Body rune", 12), q(WATER_RUNE, "Water rune", 12), q(EARTH_RUNE, "Earth rune", 12)), none(), none()),
        m(Skill.MAGIC, "String Jewellery", 80, 83.0,
            req(q(ASTRAL_RUNE, "Astral rune", 2), q(WATER_RUNE, "Water rune", 5), q(EARTH_RUNE, "Earth rune", 10)), none(), none()),
        m(Skill.MAGIC, "Plank Make - mahogany", 86, 90.0,
            req(q(6332, "Mahogany logs", 1), q(ASTRAL_RUNE, "Astral rune", 2), q(NATURE_RUNE, "Nature rune", 1)), none(), out(q(8782, "Mahogany plank", 1))),

        // Fletching - bows, arrows, darts
        m(Skill.FLETCHING, "Arrow shafts from logs", 1, 5.0,
            req(q(1511, "Logs", 1)), none(), out(q(52, "Arrow shaft", 15))),
        m(Skill.FLETCHING, "Headless arrows", 1, 15.0,
            req(q(52, "Arrow shaft", 15), q(FEATHER, "Feather", 15)), none(), out(q(53, "Headless arrow", 15))),
        m(Skill.FLETCHING, "Broad arrows", 52, 150.0,
            req(q(53, "Headless arrow", 15), q(11874, "Broad arrowheads", 15)), none(), out(q(4160, "Broad arrows", 15))),
        m(Skill.FLETCHING, "Cut shortbow (u)", 5, 5.0,
            req(q(1511, "Logs", 1)), none(), out(q(50, "Shortbow (u)", 1))),
        m(Skill.FLETCHING, "String shortbow", 5, 5.0,
            req(q(50, "Shortbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(841, "Shortbow", 1))),
        m(Skill.FLETCHING, "Cut longbow (u)", 10, 10.0,
            req(q(1511, "Logs", 1)), none(), out(q(48, "Longbow (u)", 1))),
        m(Skill.FLETCHING, "String longbow", 10, 10.0,
            req(q(48, "Longbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(839, "Longbow", 1))),
        m(Skill.FLETCHING, "Cut oak shortbow (u)", 20, 16.5,
            req(q(1521, "Oak logs", 1)), none(), out(q(54, "Oak shortbow (u)", 1))),
        m(Skill.FLETCHING, "String oak shortbow", 20, 16.5,
            req(q(54, "Oak shortbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(843, "Oak shortbow", 1))),
        m(Skill.FLETCHING, "Cut oak longbow (u)", 25, 25.0,
            req(q(1521, "Oak logs", 1)), none(), out(q(56, "Oak longbow (u)", 1))),
        m(Skill.FLETCHING, "String oak longbow", 25, 25.0,
            req(q(56, "Oak longbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(845, "Oak longbow", 1))),
        m(Skill.FLETCHING, "Cut willow shortbow (u)", 35, 33.3,
            req(q(1519, "Willow logs", 1)), none(), out(q(60, "Willow shortbow (u)", 1))),
        m(Skill.FLETCHING, "String willow shortbow", 35, 33.3,
            req(q(60, "Willow shortbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(849, "Willow shortbow", 1))),
        m(Skill.FLETCHING, "Cut willow longbow (u)", 40, 41.5,
            req(q(1519, "Willow logs", 1)), none(), out(q(58, "Willow longbow (u)", 1))),
        m(Skill.FLETCHING, "String willow longbow", 40, 41.5,
            req(q(58, "Willow longbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(847, "Willow longbow", 1))),
        m(Skill.FLETCHING, "Cut maple shortbow (u)", 50, 50.0,
            req(q(1517, "Maple logs", 1)), none(), out(q(64, "Maple shortbow (u)", 1))),
        m(Skill.FLETCHING, "String maple shortbow", 50, 50.0,
            req(q(64, "Maple shortbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(853, "Maple shortbow", 1))),
        m(Skill.FLETCHING, "Cut maple longbow (u)", 55, 58.3,
            req(q(1517, "Maple logs", 1)), none(), out(q(62, "Maple longbow (u)", 1))),
        m(Skill.FLETCHING, "String maple longbow", 55, 58.3,
            req(q(62, "Maple longbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(851, "Maple longbow", 1))),
        m(Skill.FLETCHING, "Cut yew shortbow (u)", 65, 67.5,
            req(q(1515, "Yew logs", 1)), none(), out(q(68, "Yew shortbow (u)", 1))),
        m(Skill.FLETCHING, "String yew shortbow", 65, 67.5,
            req(q(68, "Yew shortbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(857, "Yew shortbow", 1))),
        m(Skill.FLETCHING, "Cut yew longbow (u)", 70, 75.0,
            req(q(1515, "Yew logs", 1)), none(), out(q(66, "Yew longbow (u)", 1))),
        m(Skill.FLETCHING, "String yew longbow", 70, 75.0,
            req(q(66, "Yew longbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(855, "Yew longbow", 1))),
        m(Skill.FLETCHING, "Cut magic shortbow (u)", 80, 83.3,
            req(q(1513, "Magic logs", 1)), none(), out(q(72, "Magic shortbow (u)", 1))),
        m(Skill.FLETCHING, "String magic shortbow", 80, 83.3,
            req(q(72, "Magic shortbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(861, "Magic shortbow", 1))),
        m(Skill.FLETCHING, "Cut magic longbow (u)", 85, 91.5,
            req(q(1513, "Magic logs", 1)), none(), out(q(70, "Magic longbow (u)", 1))),
        m(Skill.FLETCHING, "String magic longbow", 85, 91.5,
            req(q(70, "Magic longbow (u)", 1), q(BOW_STRING, "Bow string", 1)), none(), out(q(859, "Magic longbow", 1))),
        m(Skill.FLETCHING, "Bronze darts", 10, 18.0,
            req(q(819, "Bronze dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(806, "Bronze dart", 10))),
        m(Skill.FLETCHING, "Iron darts", 22, 38.0,
            req(q(820, "Iron dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(807, "Iron dart", 10))),
        m(Skill.FLETCHING, "Steel darts", 37, 75.0,
            req(q(821, "Steel dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(808, "Steel dart", 10))),
        m(Skill.FLETCHING, "Mithril darts", 52, 112.0,
            req(q(822, "Mithril dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(809, "Mithril dart", 10))),
        m(Skill.FLETCHING, "Adamant darts", 67, 150.0,
            req(q(823, "Adamant dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(810, "Adamant dart", 10))),
        m(Skill.FLETCHING, "Rune darts", 81, 188.0,
            req(q(824, "Rune dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(811, "Rune dart", 10))),
        m(Skill.FLETCHING, "Dragon darts", 95, 250.0,
            req(q(11232, "Dragon dart tip", 10), q(FEATHER, "Feather", 10)), none(), out(q(11230, "Dragon dart", 10))),

        // Runecraft - direct and combination methods. Multiple-rune output by level is not modeled yet.
        m(Skill.RUNECRAFT, "Craft air rune", 1, 5.0,
            req(q(1436, "Rune essence", 1)), none(), out(q(AIR_RUNE, "Air rune", 1))),
        m(Skill.RUNECRAFT, "Craft mind rune", 2, 5.5,
            req(q(1436, "Rune essence", 1)), none(), out(q(558, "Mind rune", 1))),
        m(Skill.RUNECRAFT, "Craft water rune", 5, 6.0,
            req(q(1436, "Rune essence", 1)), none(), out(q(WATER_RUNE, "Water rune", 1))),
        m(Skill.RUNECRAFT, "Craft earth rune", 9, 6.5,
            req(q(1436, "Rune essence", 1)), none(), out(q(EARTH_RUNE, "Earth rune", 1))),
        m(Skill.RUNECRAFT, "Craft fire rune", 14, 7.0,
            req(q(1436, "Rune essence", 1)), none(), out(q(FIRE_RUNE, "Fire rune", 1))),
        m(Skill.RUNECRAFT, "Craft body rune", 20, 7.5,
            req(q(1436, "Rune essence", 1)), none(), out(q(BODY_RUNE, "Body rune", 1))),
        m(Skill.RUNECRAFT, "Craft cosmic rune", 27, 8.0,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(COSMIC_RUNE, "Cosmic rune", 1))),
        m(Skill.RUNECRAFT, "Craft chaos rune", 35, 8.5,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(562, "Chaos rune", 1))),
        m(Skill.RUNECRAFT, "Craft nature rune", 44, 9.0,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(NATURE_RUNE, "Nature rune", 1))),
        m(Skill.RUNECRAFT, "Craft law rune", 54, 9.5,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(LAW_RUNE, "Law rune", 1))),
        m(Skill.RUNECRAFT, "Craft death rune", 65, 10.0,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(560, "Death rune", 1))),
        m(Skill.RUNECRAFT, "Craft blood rune", 77, 23.8,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(565, "Blood rune", 1))),
        m(Skill.RUNECRAFT, "Craft soul rune", 90, 29.7,
            req(q(PURE_ESSENCE, "Pure essence", 1)), none(), out(q(566, "Soul rune", 1))),
        m(Skill.RUNECRAFT, "Craft lava rune", 23, 10.5,
            req(q(PURE_ESSENCE, "Pure essence", 1), q(1440, "Earth talisman", 1)), opt(q(5521, "Binding necklace", 0.0625)), out(q(4699, "Lava rune", 1))),

        // Construction - common bought-plank methods. Tools are not consumed.
        m(Skill.CONSTRUCTION, "Crude wooden chair", 1, 58.0,
            req(q(960, "Plank", 2)), none(), none()),
        m(Skill.CONSTRUCTION, "Wooden bookcase", 4, 115.0,
            req(q(960, "Plank", 4)), none(), none()),
        m(Skill.CONSTRUCTION, "Oak chair", 19, 120.0,
            req(q(8778, "Oak plank", 2)), none(), none()),
        m(Skill.CONSTRUCTION, "Oak armchair", 26, 180.0,
            req(q(8778, "Oak plank", 3)), none(), none()),
        m(Skill.CONSTRUCTION, "Oak larder", 33, 480.0,
            req(q(8778, "Oak plank", 8)), none(), none()),
        m(Skill.CONSTRUCTION, "Mahogany table", 52, 840.0,
            req(q(8782, "Mahogany plank", 6)), none(), none()),
        m(Skill.CONSTRUCTION, "Teak garden bench", 66, 540.0,
            req(q(8780, "Teak plank", 6)), none(), none()),
        m(Skill.CONSTRUCTION, "Oak dungeon door", 74, 600.0,
            req(q(8778, "Oak plank", 10)), none(), none()),
        m(Skill.CONSTRUCTION, "Gnome bench", 77, 840.0,
            req(q(8782, "Mahogany plank", 6)), none(), none())
    ));

    static final List<Skill> SUPPORTED_SKILLS = Collections.unmodifiableList(Arrays.asList(
        Skill.SMITHING,
        Skill.HERBLORE,
        Skill.FISHING,
        Skill.COOKING,
        Skill.PRAYER,
        Skill.CRAFTING,
        Skill.FIREMAKING,
        Skill.MAGIC,
        Skill.FLETCHING,
        Skill.RUNECRAFT,
        Skill.CONSTRUCTION
    ));

    private final Skill skill;
    private final String name;
    private final int requiredLevel;
    private final double xpPerAction;
    private final List<ItemQuantity> requiredInputs;
    private final List<ItemQuantity> optionalInputs;
    private final List<ItemQuantity> outputs;

    private SkillingMethod(
        Skill skill,
        String name,
        int requiredLevel,
        double xpPerAction,
        List<ItemQuantity> requiredInputs,
        List<ItemQuantity> optionalInputs,
        List<ItemQuantity> outputs)
    {
        this.skill = skill;
        this.name = name;
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

    String getOptionalInputSummary()
    {
        if (optionalInputs.isEmpty())
        {
            return "";
        }

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

    Set<Integer> getPriceItemIds(boolean includeOptionalInputs, boolean sellOutputs)
    {
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemQuantity input : requiredInputs)
        {
            ids.add(input.getItemId());
        }
        if (includeOptionalInputs)
        {
            for (ItemQuantity input : optionalInputs)
            {
                ids.add(input.getItemId());
            }
        }
        if (sellOutputs)
        {
            for (ItemQuantity output : outputs)
            {
                ids.add(output.getItemId());
            }
        }
        return ids;
    }

    static Map<Skill, List<SkillingMethod>> methodsBySkill()
    {
        Map<Skill, List<SkillingMethod>> map = new LinkedHashMap<>();
        for (Skill skill : SUPPORTED_SKILLS)
        {
            map.put(skill, new ArrayList<>());
        }
        for (SkillingMethod method : ALL_METHODS)
        {
            map.get(method.getSkill()).add(method);
        }
        return map;
    }

    @Override
    public String toString()
    {
        return name;
    }

    private static SkillingMethod m(Skill skill, String name, int level, double xp, List<ItemQuantity> inputs, List<ItemQuantity> optionalInputs, List<ItemQuantity> outputs)
    {
        return new SkillingMethod(skill, name, level, xp, inputs, optionalInputs, outputs);
    }

    private static ItemQuantity q(int itemId, String name, double quantity)
    {
        return new ItemQuantity(itemId, name, quantity);
    }


    private static List<ItemQuantity> req(ItemQuantity... items)
    {
        return Arrays.asList(items);
    }

    private static List<ItemQuantity> opt(ItemQuantity... items)
    {
        return Arrays.asList(items);
    }

    private static List<ItemQuantity> out(ItemQuantity... items)
    {
        return Arrays.asList(items);
    }

    private static List<ItemQuantity> none()
    {
        return Collections.emptyList();
    }
}
