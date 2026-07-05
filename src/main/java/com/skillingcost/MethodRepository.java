package com.skillingcost;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Loads and caches all skilling methods from the bundled {@code methods.json} resource.
 *
 * Most methods are listed verbatim in the JSON. Prayer is special-cased: the JSON only contains
 * a table of bones (bury XP + blessed bone shards) and ashes, and this class expands that table
 * into one method group per way of offering them (bury, gilded altar, chaos altar, Ectofuntus,
 * libation bowl with regular or sunfire wine, scatter). Adding a new bone to the JSON therefore
 * automatically adds it to every offering method.
 */
@Singleton
final class MethodRepository
{
    private static final String RESOURCE = "/methods.json";

    // Offering multipliers, all relative to bury XP. See e.g.
    // https://oldschool.runescape.wiki/w/Prayer#Bones
    private static final double GILDED_ALTAR_MULTIPLIER = 3.5;
    // Chaos altar offerings give gilded-altar XP with a 50% chance to not consume the bone,
    // so on average each bone consumed is worth double: 7x bury XP.
    private static final double CHAOS_ALTAR_MULTIPLIER = 7.0;
    private static final double ECTOFUNTUS_MULTIPLIER = 4.0;
    // Libation bowl XP is per blessed bone shard: 5 XP with blessed wine, 6 with sunfire wine.
    private static final double XP_PER_SHARD_WINE = 5.0;
    private static final double XP_PER_SHARD_SUNFIRE = 6.0;
    // Consumption rates carried over from the original hand-computed data:
    // one jug of wine per 400 shards, one batch of sunfire splinters per 200 shards.
    private static final double SHARDS_PER_JUG_OF_WINE = 400.0;
    private static final double SHARDS_PER_SUNFIRE_SPLINTER = 200.0;

    private final Gson gson;

    private List<Skill> supportedSkills;
    private Map<Skill, List<SkillingMethod>> methodsBySkill;

    @Inject
    MethodRepository(Gson gson)
    {
        this.gson = gson;
    }

    synchronized List<Skill> getSupportedSkills()
    {
        load();
        return supportedSkills;
    }

    /** Methods per skill, in display order. The returned lists must not be modified. */
    synchronized Map<Skill, List<SkillingMethod>> getMethodsBySkill()
    {
        load();
        return methodsBySkill;
    }

    private void load()
    {
        if (methodsBySkill != null)
        {
            return;
        }

        RootJson root;
        try (InputStream in = MethodRepository.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                throw new IllegalStateException("Missing bundled resource " + RESOURCE);
            }
            root = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), RootJson.class);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to load " + RESOURCE + ": " + e.getMessage(), e);
        }
        if (root == null || root.skills == null || root.methods == null || root.prayer == null)
        {
            throw new IllegalStateException(RESOURCE + " is malformed");
        }

        List<Skill> skills = new ArrayList<>();
        Map<Skill, List<SkillingMethod>> map = new LinkedHashMap<>();
        for (String skillName : root.skills)
        {
            Skill skill = Skill.valueOf(skillName);
            skills.add(skill);
            map.put(skill, new ArrayList<>());
        }

        for (MethodJson method : root.methods)
        {
            Skill skill = Skill.valueOf(method.skill);
            map.get(skill).add(new SkillingMethod(
                skill,
                method.name,
                method.group,
                method.variant,
                method.level,
                method.xp,
                toItems(method.inputs),
                toItems(method.optional),
                toItems(method.outputs)));
        }

        List<SkillingMethod> prayer = map.get(Skill.PRAYER);
        if (prayer != null)
        {
            prayer.addAll(expandPrayer(root.prayer));
        }

        for (Map.Entry<Skill, List<SkillingMethod>> entry : map.entrySet())
        {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        supportedSkills = Collections.unmodifiableList(skills);
        methodsBySkill = Collections.unmodifiableMap(map);
    }

    /** Expands the bones/ashes tables into full method groups. */
    private static List<SkillingMethod> expandPrayer(PrayerJson prayer)
    {
        List<SkillingMethod> methods = new ArrayList<>();

        for (BoneJson bone : prayer.bones)
        {
            methods.add(boneMethod("Bury", bone, bone.level, bone.buryXp,
                items(bone(bone))));
        }
        for (BoneJson bone : prayer.bones)
        {
            methods.add(boneMethod("Gilded altar (both burners lit)", bone, bone.level,
                bone.buryXp * GILDED_ALTAR_MULTIPLIER, items(bone(bone))));
        }
        for (BoneJson bone : prayer.bones)
        {
            methods.add(boneMethod("Chaos altar (avg. 50% bone save)", bone, bone.level,
                bone.buryXp * CHAOS_ALTAR_MULTIPLIER, items(bone(bone))));
        }
        for (BoneJson bone : prayer.bones)
        {
            methods.add(boneMethod("Ectofuntus", bone, bone.level,
                bone.buryXp * ECTOFUNTUS_MULTIPLIER,
                items(bone(bone), ref(prayer.bucketOfSlime, 1))));
        }
        for (BoneJson bone : prayer.bones)
        {
            methods.add(boneMethod("Libation bowl (blessed wine)", bone,
                Math.max(prayer.libationMinLevel, bone.level),
                bone.shards * XP_PER_SHARD_WINE,
                items(bone(bone), unnotingFee(prayer),
                    ref(prayer.jugOfWine, bone.shards / SHARDS_PER_JUG_OF_WINE))));
        }
        for (BoneJson bone : prayer.bones)
        {
            methods.add(boneMethod("Libation bowl (sunfire wine)", bone,
                Math.max(prayer.libationMinLevel, bone.level),
                bone.shards * XP_PER_SHARD_SUNFIRE,
                items(bone(bone), unnotingFee(prayer),
                    ref(prayer.jugOfWine, bone.shards / SHARDS_PER_JUG_OF_WINE),
                    ref(prayer.sunfireSplinters, bone.shards / SHARDS_PER_SUNFIRE_SPLINTER))));
        }

        for (AshJson ash : prayer.ashes)
        {
            methods.add(new SkillingMethod(Skill.PRAYER,
                "Scatter " + ash.name.toLowerCase(),
                "Scatter ashes", ash.name,
                ash.level, ash.scatterXp,
                items(new ItemQuantity(ash.id, ash.name, 1)),
                Collections.emptyList(), Collections.emptyList()));
        }

        return methods;
    }

    private static SkillingMethod boneMethod(String group, BoneJson bone, int level, double xp, List<ItemQuantity> inputs)
    {
        return new SkillingMethod(Skill.PRAYER,
            group + " - " + bone.name.toLowerCase(),
            group, bone.name,
            level, xp,
            inputs, Collections.emptyList(), Collections.emptyList());
    }

    private static ItemQuantity bone(BoneJson bone)
    {
        return new ItemQuantity(bone.id, bone.name, 1);
    }

    private static ItemQuantity ref(ItemRefJson item, double quantityPerAction)
    {
        return new ItemQuantity(item.id, item.name, quantityPerAction);
    }

    private static ItemQuantity unnotingFee(PrayerJson prayer)
    {
        // Virilis outside the libation altar un-notes bones for a flat gp fee per bone.
        return new ItemQuantity(-1, "Virilis unnoting fee", 1, true, prayer.libationFeeGp);
    }

    private static List<ItemQuantity> items(ItemQuantity... entries)
    {
        List<ItemQuantity> list = new ArrayList<>();
        Collections.addAll(list, entries);
        return list;
    }

    private static List<ItemQuantity> toItems(List<ItemJson> items)
    {
        List<ItemQuantity> out = new ArrayList<>();
        if (items == null)
        {
            return out;
        }
        for (ItemJson item : items)
        {
            if (item.fixedPrice != null)
            {
                out.add(new ItemQuantity(item.id, item.name, item.qty, true, item.fixedPrice));
            }
            else
            {
                out.add(new ItemQuantity(item.id, item.name, item.qty));
            }
        }
        return out;
    }

    // --- Gson DTOs mirroring methods.json ---

    private static final class RootJson
    {
        List<String> skills;
        List<MethodJson> methods;
        PrayerJson prayer;
    }

    private static final class MethodJson
    {
        String skill;
        String name;
        String group;
        String variant;
        int level;
        double xp;
        List<ItemJson> inputs;
        List<ItemJson> optional;
        List<ItemJson> outputs;
    }

    private static final class ItemJson
    {
        int id;
        String name;
        double qty;
        Integer fixedPrice;
    }

    private static final class PrayerJson
    {
        int libationMinLevel;
        int libationFeeGp;
        ItemRefJson jugOfWine;
        ItemRefJson sunfireSplinters;
        ItemRefJson bucketOfSlime;
        List<BoneJson> bones;
        List<AshJson> ashes;
    }

    /** A bare item reference (no quantity) used by the generated Prayer methods. */
    private static final class ItemRefJson
    {
        int id;
        String name;
    }

    private static final class BoneJson
    {
        int id;
        String name;
        int level;
        double buryXp;
        int shards;
    }

    private static final class AshJson
    {
        int id;
        String name;
        int level;
        double scatterXp;
    }
}
