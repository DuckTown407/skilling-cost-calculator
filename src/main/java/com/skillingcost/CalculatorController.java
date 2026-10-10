package com.skillingcost;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.runelite.api.Skill;

interface CalculatorController
{
    void loadCurrentSkillXp(Skill skill, IntConsumer callback);
    void estimate(CalculationRequest request, Consumer<Map<String, CostResult>> success, Consumer<String> failure);
}
