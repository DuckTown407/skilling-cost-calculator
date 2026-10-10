package com.skillingcost;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Loads the bundled catalog and expands Prayer offerings. Preload off the client thread. */
@Singleton
final class MethodRepository
{
    private static final String RESOURCE = "/methods.json";
    private static final String PRAYER_SOURCE = "https://oldschool.runescape.wiki/w/Prayer";
    private static final EnumSet<Skill> SUPPORTED = EnumSet.of(Skill.SMITHING, Skill.HERBLORE, Skill.FISHING,
        Skill.COOKING, Skill.PRAYER, Skill.CRAFTING, Skill.FIREMAKING, Skill.MAGIC, Skill.FLETCHING,
        Skill.RUNECRAFT, Skill.CONSTRUCTION);
    private final Gson gson;
    private List<Skill> supportedSkills;
    private Map<Skill, List<SkillingMethod>> methodsBySkill;

    @Inject
    MethodRepository(Gson gson) { this.gson = gson; }

    synchronized List<Skill> getSupportedSkills() { load(); return supportedSkills; }
    synchronized Map<Skill, List<SkillingMethod>> getMethodsBySkill() { load(); return methodsBySkill; }

    private void load()
    {
        if (methodsBySkill != null) { return; }
        try (InputStream stream = MethodRepository.class.getResourceAsStream(RESOURCE))
        {
            if (stream == null) { throw new IllegalStateException("Missing bundled " + RESOURCE); }
            read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Invalid bundled catalog: " + exception.getMessage(), exception);
        }
    }

    synchronized void read(Reader reader)
    {
        RootJson root = gson.fromJson(reader, RootJson.class);
        if (root == null || root.skills == null || root.methods == null || root.prayer == null)
        {
            throw new IllegalArgumentException("Missing skills, methods, or Prayer table");
        }
        List<Skill> skills = new ArrayList<>();
        Map<Skill, List<SkillingMethod>> map = new LinkedHashMap<>();
        for (String name : root.skills)
        {
            Skill skill = Skill.valueOf(name);
            if (!SUPPORTED.contains(skill) || map.containsKey(skill))
            {
                throw new IllegalArgumentException("Invalid or duplicate skill: " + name);
            }
            skills.add(skill);
            map.put(skill, new ArrayList<>());
        }
        if (!map.keySet().equals(SUPPORTED))
        {
            throw new IllegalArgumentException("Catalog must contain the 11 supported skills");
        }
        Set<String> identities = new HashSet<>();
        for (MethodJson recipe : root.methods)
        {
            Skill skill = Skill.valueOf(recipe.skill);
            if (!map.containsKey(skill)) { throw new IllegalArgumentException("Unsupported skill: " + recipe.skill); }
            SkillingMethod method = new SkillingMethod(skill, recipe.key, recipe.name, recipe.group, recipe.variant,
                recipe.level, recipe.xp, toItems(recipe.inputs), toItems(recipe.optional),
                toItems(recipe.outputs), recipe.action, recipe.source, recipe.assumptions, recipe.outputLevels);
            add(map, method, identities);
        }
        for (SkillingMethod method : expandPrayer(root.prayer)) { add(map, method, identities); }
        for (Map.Entry<Skill, List<SkillingMethod>> entry : map.entrySet())
        {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        supportedSkills = Collections.unmodifiableList(skills);
        methodsBySkill = Collections.unmodifiableMap(map);
    }

    private static void add(Map<Skill, List<SkillingMethod>> map, SkillingMethod method, Set<String> ids)
    {
        if (!ids.add(method.getId())) { throw new IllegalArgumentException("Duplicate recipe: " + method.getId()); }
        map.get(method.getSkill()).add(method);
    }

    private static List<SkillingMethod> expandPrayer(PrayerJson prayer)
    {
        if (prayer.bones == null || prayer.ashes == null || prayer.libationMinLevel < 1
            || prayer.libationMinLevel > 99 || prayer.libationFeeGp < 0)
        {
            throw new IllegalArgumentException("Invalid Prayer table");
        }
        List<SkillingMethod> methods = new ArrayList<>();
        for (BoneJson bone : prayer.bones)
        {
            if (!Double.isFinite(bone.buryXp) || bone.buryXp <= 0 || bone.shards <= 0)
            {
                throw new IllegalArgumentException("Invalid bone: " + bone.name);
            }
            methods.add(boneMethod("bury", "Bury", bone, bone.level, bone.buryXp,
                items(ref(bone, BigDecimal.ONE)), "One bone buried."));
            methods.add(boneMethod("gilded", "Gilded altar (both burners lit)", bone, bone.level, bone.buryXp * 3.5,
                items(ref(bone, BigDecimal.ONE)), "Both burners lit. Burner and travel costs excluded."));
            methods.add(boneMethod("chaos", "Chaos altar (avg. 50% bone save)", bone, bone.level, bone.buryXp * 7,
                items(ref(bone, BigDecimal.ONE)), "Long-run average XP per consumed bone; actual bone saves vary. Travel and risk excluded."));
            methods.add(boneMethod("ecto", "Ectofuntus", bone, bone.level, bone.buryXp * 4,
                items(ref(bone, BigDecimal.ONE), ref(prayer.bucketOfSlime, BigDecimal.ONE)),
                "Collected slime required. Empty pots, returned buckets, travel, and grinding costs excluded."));
            for (boolean sunfire : new boolean[]{false, true})
            {
                List<ItemQuantity> inputs = items(ref(bone, BigDecimal.ONE),
                    new ItemQuantity(-1, "Virilis unnoting fee", 1, prayer.libationFeeGp),
                    ref(prayer.jugOfWine, BigDecimal.valueOf(bone.shards).divide(BigDecimal.valueOf(400))));
                if (sunfire)
                {
                    inputs.add(ref(prayer.sunfireSplinters,
                        BigDecimal.valueOf(bone.shards).divide(BigDecimal.valueOf(200))));
                }
                methods.add(boneMethod(sunfire ? "sunfire" : "blessed",
                    sunfire ? "Libation bowl (sunfire wine)" : "Libation bowl (blessed wine)", bone,
                    Math.max(prayer.libationMinLevel, bone.level), bone.shards * (sunfire ? 6.0 : 5.0), inputs,
                    "One action processes all shards from one bone. Wine/splinters rounded over the whole plan. No Zealot shard saves; collection and travel excluded."));
            }
        }
        for (AshJson ash : prayer.ashes)
        {
            methods.add(new SkillingMethod(Skill.PRAYER, "prayer:scatter:" + ash.resource,
                "Scatter " + ash.name, "Scatter ashes", ash.name, ash.level, ash.scatterXp,
                items(ref(ash, BigDecimal.ONE)), Collections.emptyList(), Collections.emptyList(),
                "ash scattered", PRAYER_SOURCE, "One ash scattered.", new int[0]));
        }
        return methods;
    }

    private static SkillingMethod boneMethod(String key, String group, BoneJson bone, int level, double xp,
        List<ItemQuantity> inputs, String assumptions)
    {
        return new SkillingMethod(Skill.PRAYER, "prayer:" + key + ":" + bone.resource,
            group + " - " + bone.name, group, bone.name, level, xp, inputs,
            Collections.emptyList(), Collections.emptyList(), "bone processed", PRAYER_SOURCE, assumptions, new int[0]);
    }

    private static ItemQuantity ref(ItemRefJson item, BigDecimal quantity)
    {
        if (item == null) { throw new IllegalArgumentException("Missing Prayer supply"); }
        ItemQuantity.Pricing pricing = pricing(item.pricing);
        if (pricing == ItemQuantity.Pricing.FIXED_GP) { throw new IllegalArgumentException("Prayer item is a fee"); }
        return new ItemQuantity(ResourceRegistry.require(item.resource), item.name, quantity, pricing,
            0, 1, false, false);
    }

    private static ItemQuantity.Pricing pricing(String value)
    {
        if (value == null) { throw new IllegalArgumentException("Missing pricing mode"); }
        return ItemQuantity.Pricing.valueOf(value);
    }

    private static List<ItemQuantity> items(ItemQuantity... values)
    {
        List<ItemQuantity> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }

    private static List<ItemQuantity> toItems(List<ItemJson> values)
    {
        List<ItemQuantity> list = new ArrayList<>();
        if (values == null) { return list; }
        for (ItemJson item : values)
        {
            ItemQuantity.Pricing mode = pricing(item.pricing);
            if ((mode == ItemQuantity.Pricing.FIXED_GP) != (item.fixedPrice != null)
                || (mode == ItemQuantity.Pricing.FIXED_GP && item.resource != null))
            {
                throw new IllegalArgumentException("Inconsistent pricing: " + item.name);
            }
            int id = mode == ItemQuantity.Pricing.FIXED_GP ? -1 : ResourceRegistry.require(item.resource);
            list.add(new ItemQuantity(id, item.name, item.qty, mode,
                item.fixedPrice == null ? 0 : item.fixedPrice,
                item.unitsPerItem == null ? 1 : item.unitsPerItem, item.perBatch, item.taxExempt));
        }
        return list;
    }

    private static final class RootJson
    {
        List<String> skills;
        List<MethodJson> methods;
        PrayerJson prayer;
    }

    private static final class MethodJson
    {
        String key, skill, name, group, variant, action, source, assumptions;
        int level;
        double xp;
        int[] outputLevels;
        List<ItemJson> inputs, optional, outputs;
    }

    private static final class ItemJson
    {
        String resource, name, pricing;
        BigDecimal qty;
        Long fixedPrice;
        Integer unitsPerItem;
        boolean perBatch, taxExempt;
    }

    private static class ItemRefJson
    {
        String resource, name, pricing;
    }

    private static final class BoneJson extends ItemRefJson
    {
        int level, shards;
        double buryXp;
    }

    private static final class AshJson extends ItemRefJson
    {
        int level;
        double scatterXp;
    }

    private static final class PrayerJson
    {
        int libationMinLevel;
        long libationFeeGp;
        ItemRefJson jugOfWine, sunfireSplinters, bucketOfSlime;
        List<BoneJson> bones;
        List<AshJson> ashes;
    }
}
