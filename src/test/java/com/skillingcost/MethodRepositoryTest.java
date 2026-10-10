package com.skillingcost;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodRepositoryTest
{
    private final Gson gson = new Gson();

    private JsonObject catalog()
    {
        return gson.fromJson(new InputStreamReader(getClass().getResourceAsStream("/methods.json"), StandardCharsets.UTF_8), JsonObject.class);
    }

    private void reject(Consumer<JsonObject> mutate)
    {
        JsonObject root = catalog();
        mutate.accept(root);
        try
        {
            new MethodRepository(gson).read(new StringReader(root.toString()));
            fail("Invalid catalog accepted");
        }
        catch (IllegalArgumentException expected) { }
    }

    @Test
    public void bundledCatalogLoadsAllSupportedSkills()
    {
        MethodRepository repository = new MethodRepository(gson);
        assertEquals(11, repository.getSupportedSkills().size());
        assertTrue(repository.getMethodsBySkill().values().stream().allMatch(methods -> !methods.isEmpty()));
        assertTrue(repository.getMethodsBySkill().values().stream().flatMap(java.util.Collection::stream)
            .allMatch(method -> !method.getId().isEmpty() && method.getXpPerAction() > 0));
    }

    @Test
    public void rejectsBadStructure()
    {
        reject(root -> root.getAsJsonArray("methods").get(0).getAsJsonObject().addProperty("xp", 0));
        reject(root -> root.getAsJsonArray("methods").get(0).getAsJsonObject().addProperty("level", 100));
        reject(root -> root.getAsJsonArray("methods").get(0).getAsJsonObject().getAsJsonArray("inputs")
            .get(0).getAsJsonObject().addProperty("qty", -1));
        reject(root -> root.getAsJsonArray("methods").get(0).getAsJsonObject().getAsJsonArray("inputs")
            .get(0).getAsJsonObject().addProperty("resource", "MISSING_RESOURCE"));
        reject(root -> root.getAsJsonArray("methods").add(root.getAsJsonArray("methods").get(0)));
        reject(root -> root.getAsJsonArray("methods").get(0).getAsJsonObject().getAsJsonArray("inputs")
            .get(0).getAsJsonObject().addProperty("fixedPrice", -1));
    }

    @Test
    public void addedBoneExpandsUsingItsOwnFacts()
    {
        JsonObject root = catalog();
        JsonObject bone = new JsonObject();
        bone.addProperty("resource", "BAT_BONES");
        bone.addProperty("name", "Fixture bat bones");
        bone.addProperty("pricing", "GE");
        bone.addProperty("level", 1);
        bone.addProperty("buryXp", 5.3);
        bone.addProperty("shards", 5);
        root.getAsJsonObject("prayer").getAsJsonArray("bones").add(bone);
        // Use an already registered resource, so the fixture isolates expansion from identity maintenance.
        bone.addProperty("resource", root.getAsJsonObject("prayer").getAsJsonArray("bones").get(0)
            .getAsJsonObject().get("resource").getAsString());
        root.getAsJsonObject("prayer").getAsJsonArray("bones").remove(0);
        MethodRepository repository = new MethodRepository(gson);
        repository.read(new StringReader(root.toString()));
        assertEquals(6, repository.getMethodsBySkill().get(net.runelite.api.Skill.PRAYER).stream()
            .filter(method -> "Fixture bat bones".equals(method.getVariantName())).count());
        assertTrue(repository.getMethodsBySkill().get(net.runelite.api.Skill.PRAYER).stream()
            .anyMatch(method -> "Fixture bat bones".equals(method.getVariantName()) && method.getXpPerAction() == 25));
    }
}
