package com.skillingcost;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Recipe facts checked against the source references in docs/CATALOG_COVERAGE.md. */
public class CatalogMechanicsTest
{
    private final Map<String, SkillingMethod> catalog = new MethodRepository(new Gson()).getMethodsBySkill()
        .values().stream().flatMap(List::stream).collect(Collectors.toMap(SkillingMethod::getId, method -> method));

    private SkillingMethod method(String id)
    {
        assertTrue("Missing recipe " + id, catalog.containsKey(id));
        return catalog.get(id);
    }

    private ItemQuantity item(List<ItemQuantity> resources, String name)
    {
        return resources.stream().filter(item -> item.getName().equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError("Missing resource " + name));
    }

    private void quantity(ItemQuantity item, String expected)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(item.getQuantityPerAction()));
    }

    @Test
    public void correctedDoseAndAshIdentities()
    {
        assertEquals(3026, method("herblore:super-restore").getOutputs().get(0).getItemId());
        assertEquals(6687, method("herblore:saradomin-brew").getOutputs().get(0).getItemId());
        SkillingMethod combat = method("herblore:super-combat-potion");
        assertEquals(150, combat.getXpPerAction(), 0);
        for (int id : new int[]{2436, 2440, 2442})
        {
            assertTrue(combat.getRequiredInputs().stream().anyMatch(item -> item.getItemId() == id));
        }
        String[] ashes = {"FIENDISH_ASHES", "VILE_ASHES", "MALICIOUS_ASHES", "ABYSSAL_ASHES", "INFERNAL_ASHES"};
        int[] ids = {25766, 25769, 25772, 25775, 25778};
        double[] xp = {10, 25, 65, 85, 110};
        for (int i = 0; i < ashes.length; i++)
        {
            SkillingMethod ash = method("prayer:scatter:" + ashes[i]);
            assertEquals(ids[i], ash.getRequiredInputs().get(0).getItemId());
            assertEquals(xp[i], ash.getXpPerAction(), 0);
        }
    }

    @Test
    public void guaranteedIronAndReusedConstructionMaterials()
    {
        ItemQuantity ring = item(method("smithing:smelt-iron-bar").getRequiredInputs(), "Ring of forging");
        assertEquals(1, ring.total(140, 28, 1, false));
        assertEquals(2, ring.total(141, 28, 1, false));
        assertTrue(method("smithing:smelt-iron-bar").getOptionalInputs().isEmpty());
        quantity(item(method("construction:crude-wooden-chair").getRequiredInputs(), "Steel nails"), "2");
        quantity(item(method("construction:wooden-bookcase").getRequiredInputs(), "Steel nails"), "4");
        SkillingMethod cape = method("construction:mounted-mythical-cape");
        assertEquals(370, cape.getXpPerAction(), 0);
        quantity(item(cape.getRequiredInputs(), "Teak plank"), "3");
        assertTrue(cape.getRequiredInputs().stream().noneMatch(item -> item.getName().contains("cape")));
        assertTrue(cape.getAssumptions().contains("returned on removal"));
    }

    @Test
    public void magicRunesFeesAndEquipmentAreExplicit()
    {
        SkillingMethod plank = method("magic:plank-make-mahogany");
        quantity(item(plank.getRequiredInputs(), "Earth rune"), "15");
        quantity(item(plank.getRequiredInputs(), "Astral rune"), "2");
        quantity(item(plank.getRequiredInputs(), "Nature rune"), "1");
        assertEquals(1050, plank.getRequiredInputs().stream().filter(ItemQuantity::hasFixedUnitPrice)
            .findFirst().get().getFixedUnitPrice());
        SkillingMethod staff = method("magic:plank-make-mahogany-earth-staff");
        assertTrue(staff.getRequiredInputs().stream().noneMatch(item -> item.getName().equals("Earth rune")));
        assertTrue(staff.getAssumptions().contains("staff"));
        assertTrue(catalog.values().stream().filter(method -> method.getSkill() == Skill.MAGIC
            && method.getVariantName() != null && method.getVariantName().endsWith("staff"))
            .noneMatch(method -> method.getAssumptions().contains("No staff")));
        quantity(item(method("magic:superheat-steel-bar").getRequiredInputs(), "Fire rune"), "4");
        quantity(item(method("magic:superheat-steel-bar").getRequiredInputs(), "Coal"), "2");
        assertFalse(method("magic:stun").getRequiredInputs().stream().anyMatch(item -> item.getName().equals("Body rune")));
        SkillingMethod string = method("magic:string-jewellery");
        assertEquals(ItemID.UNSTRUNG_GOLD_AMULET, item(string.getRequiredInputs(), "Gold amulet (u)").getItemId());
        assertEquals(ItemID.STRUNG_GOLD_AMULET, string.getOutputs().get(0).getItemId());
        assertTrue(string.getRequiredInputs().stream().noneMatch(item -> item.getName().equals("Ball of wool")));
        quantity(item(method("magic:spin-flax").getRequiredInputs(), "Flax"), "5");
        quantity(method("magic:spin-flax").getOutputs().get(0), "5");
        quantity(method("magic:tan-green").getOutputs().get(0), "5");
        quantity(item(method("magic:superglass-giant_seaweed").getRequiredInputs(), "Bucket of sand"), "18");
        assertEquals(28, method("magic:superglass-giant_seaweed").getOutputs().get(0).total(1, 28, 1, true));
        assertEquals(57, method("magic:superglass-giant_seaweed").getOutputs().get(0).total(2, 28, 1, true));
    }

    @Test
    public void runecraftingUsesCorrectEssenceYieldAndBatchGuarantees()
    {
        SkillingMethod pure = method("runecraft:craft-blood-rune");
        assertEquals(10.5, pure.getXpPerAction(), 0);
        assertEquals(ItemID.BLANKRUNE_HIGH, pure.getRequiredInputs().get(0).getItemId());
        SkillingMethod dark = method("runecraft:blood-dark-essence");
        assertEquals(23.8, dark.getXpPerAction(), 0);
        assertEquals(ItemQuantity.Pricing.UNTRADEABLE, dark.getRequiredInputs().get(0).getPricing());
        SkillingMethod astral = method("runecraft:astral");
        assertEquals(1, astral.outputMultiplier(81));
        assertEquals(2, astral.outputMultiplier(82));
        assertEquals(10, method("runecraft:craft-air-rune").outputMultiplier(99));
        assertEquals(2, method("runecraft:sunfire").outputMultiplier(49));
        assertEquals(3, method("runecraft:sunfire").outputMultiplier(98));
        SkillingMethod aether = method("runecraft:aether");
        quantity(item(aether.getRequiredInputs(), "Soul rune"), "1");
        quantity(item(aether.getRequiredInputs(), "Aether catalyst"), "1");
        assertEquals(4, item(aether.getRequiredInputs(), "Astral rune").total(29, 28, 1, false));
        assertEquals(1, item(aether.getRequiredInputs(), "Binding necklace").total(128, 128, 1, false));
        String[] combinations = {"craft-lava-rune", "mist", "dust", "mud", "smoke", "steam"};
        for (String combination : combinations)
        {
            SkillingMethod method = method("runecraft:" + combination);
            assertEquals(4, method.getRequiredInputs().size());
            assertTrue(method.getOptionalInputs().isEmpty());
            ItemQuantity talisman = method.getRequiredInputs().stream().filter(item -> item.getName().endsWith("talisman")).findFirst().get();
            assertEquals(2, talisman.total(29, 28, 1, false));
            ItemQuantity necklace = item(method.getRequiredInputs(), "Binding necklace");
            assertEquals(1, necklace.total(448, 28, 1, false));
            assertEquals(2, necklace.total(449, 28, 1, false));
        }
    }

    @Test
    public void modernHerbloreKeepsDosesAndIntermediatesSeparate()
    {
        assertEquals(58, method("herblore:clean-huasca").getRequiredLevel());
        assertEquals(11.8, method("herblore:clean-huasca").getXpPerAction(), 0);
        SkillingMethod goading = method("herblore:goading");
        assertEquals(54, goading.getRequiredLevel());
        assertEquals(132, goading.getXpPerAction(), 0);
        assertTrue(goading.getRequiredInputs().stream().anyMatch(item -> item.getName().contains("(unf)")));
        assertFalse(method("herblore:goading-clean").getRequiredInputs().stream().anyMatch(item -> item.getName().contains("(unf)")));
        assertTrue(goading.getOutputs().get(0).getName().contains("(3)"));
        assertEquals(132, method("herblore:prayer-regeneration").getXpPerAction(), 0);
        quantity(item(method("herblore:stamina").getRequiredInputs(), "Amylase crystal"), "4");
    }

    @Test
    public void atlatlStagesAndAmethystUseTheirActualBatchSizes()
    {
        double[] xp = {30, 10, 10, 95};
        String[] stages = {"shafts", "tips", "headless", "assembly"};
        int[] counts = {100, 100, 10, 10};
        for (int i = 0; i < stages.length; i++)
        {
            SkillingMethod method = method("fletching:atlatl-" + stages[i]);
            assertEquals(74, method.getRequiredLevel());
            assertEquals(xp[i], method.getXpPerAction(), 0);
            assertEquals(counts[i], method.getOutputs().get(0).total(1, 28, 1, true));
        }
        assertEquals(1090, method("fletching:atlatl-full").getXpPerAction(), 0);
        assertEquals(100, method("fletching:atlatl-full").getOutputs().get(0).total(1, 28, 1, true));
        assertEquals(8, method("crafting:amethyst-darts").getOutputs().get(0).total(1, 28, 1, true));
        assertEquals(60, method("crafting:amethyst-darts").getXpPerAction(), 0);
        assertEquals(58.2, method("fletching:string-maple-longbow").getXpPerAction(), 0);
        assertEquals(210, method("fletching:amethyst-darts").getXpPerAction(), 0);
        assertEquals(10.5, method("fletching:redwood-hiking-staff").getXpPerAction(), 0);
        assertEquals(28, method("crafting:hardleather-body").getRequiredLevel());
    }

    @Test
    public void suppliesAndSuccessAssumptionsAreVisible()
    {
        assertTrue(catalog.values().stream().filter(method -> method.getSkill() == Skill.FISHING)
            .anyMatch(method -> method.getRequiredInputs().stream().anyMatch(item -> item.getName().equals("Sandworms"))));
        assertTrue(catalog.values().stream().filter(method -> method.getSkill() == Skill.FISHING)
            .anyMatch(method -> method.getRequiredInputs().stream().anyMatch(item -> item.getItemId() == ItemID.WILDERNESS_FISHING_BAIT)));
        SkillingMethod bream = method("cooking:cook-cooked_bream");
        assertEquals(45, bream.getXpPerAction(), 0);
        assertTrue(bream.getPriceItemIds(true, true).isEmpty());
        SkillingMethod pizza = method("cooking:bake-plain_pizza");
        assertEquals(143, pizza.getXpPerAction(), 0);
        assertEquals(3, pizza.getRequiredInputs().size());
        assertEquals(26, method("cooking:top-meat-pizza").getXpPerAction(), 0);
        assertEquals(169, method("cooking:make-meat-pizza").getXpPerAction(), 0);
        assertEquals(120, method("firemaking:burn-jatoba_logs").getXpPerAction(), 0);
        assertTrue(method("firemaking:burn-jatoba_logs").getPriceItemIds(true, true).isEmpty());
    }

    @Test
    public void prayerAndNewCannonballsHaveDistinctSupplies()
    {
        SkillingMethod bat = method("prayer:sinister:BAT_BONES");
        assertEquals(47.7, bat.getXpPerAction(), 0);
        assertEquals(10, bat.actionsFor(477));
        assertEquals(3, bat.getRequiredInputs().get(0).total(1, 28, 1, false));
        quantity(item(bat.getRequiredInputs(), "Blood rune"), "1");
        quantity(item(bat.getRequiredInputs(), "Wrath rune"), "1");
        quantity(item(method("prayer:demonic:INFERNAL_ASHES").getRequiredInputs(), "Soul rune"), "1");
        SkillingMethod ecto = method("prayer:ecto:DRAGON_BONES");
        assertTrue(ecto.getRequiredInputs().stream().anyMatch(item -> item.getPricing() == ItemQuantity.Pricing.UNTRADEABLE));
        SkillingMethod libation = method("prayer:sunfire:DRAGON_BONES");
        assertTrue(libation.getRequiredInputs().stream().anyMatch(ItemQuantity::hasFixedUnitPrice));
        assertEquals(34, method("smithing:mithril-cannonballs-buy").getXpPerAction(), 0);
        assertEquals(4, method("smithing:mithril-cannonballs-buy").getOutputs().get(0).total(1, 28, 1, true));
        SkillingMethod chain = catalog.values().stream().filter(method -> method.getSkill() == Skill.SMITHING
            && method.getName().toLowerCase().contains("steel chainshot")).findFirst().get();
        assertEquals(24, chain.getXpPerAction(), 0);
        assertEquals(2, chain.getOutputs().get(0).total(1, 28, 1, true));
    }

    @Test
    public void standardForgingAndCombinedSmeltingCountEveryBar()
    {
        SkillingMethod rune = method("smithing:rune-plateskirt-buy");
        assertEquals(99, rune.getRequiredLevel());
        assertEquals(225, rune.getXpPerAction(), 0);
        quantity(rune.getRequiredInputs().get(0), "3");
        SkillingMethod combined = method("smithing:iron-2h_sword-smelt");
        quantity(item(combined.getRequiredInputs(), "Iron ore"), "3");
        assertEquals(112.5, combined.getXpPerAction(), 0);
        assertEquals(1, item(combined.getRequiredInputs(), "Ring of forging").total(46, 28, 1, false));
        assertEquals(2, item(combined.getRequiredInputs(), "Ring of forging").total(47, 28, 1, false));
    }
}
