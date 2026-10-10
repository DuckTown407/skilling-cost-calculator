package com.skillingcost;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class CostCalculatorTest
{
    private static final CostCalculator.TaxSettings TAX = new CostCalculator.TaxSettings(200, 5_000_000, 50);
    private static final Instant NOW = Instant.ofEpochSecond(1000);

    private SkillingMethod recipe(ItemQuantity input, ItemQuantity output)
    {
        return new SkillingMethod(net.runelite.api.Skill.MAGIC, "test", null, null, 1, 50,
            input == null ? Collections.emptyList() : Arrays.asList(input), Collections.emptyList(),
            output == null ? Collections.emptyList() : Arrays.asList(output));
    }

    private CostResult calculate(SkillingMethod method, int xp, boolean sell, PriceSnapshot snapshot)
    {
        return CostCalculator.calculate(method, 1, 0, 2, xp, true, sell, true, 28, snapshot, TAX, NOW);
    }

    @Test
    public void largeInputTotalAndMissingQuote()
    {
        ItemQuantity input = new ItemQuantity(1, "input", 1);
        PriceSnapshot snapshot = new PriceSnapshot(Collections.singletonMap(1, new WikiPrice(2_450_000_000L, null, 900L, null)), NOW);
        CostResult result = calculate(recipe(input, null), 101, false, snapshot);
        assertEquals(3, result.getActionsNeeded());
        assertEquals(7_350_000_000L, result.getInputCost());
        assertEquals(-7_350_000_000L, result.getNet());
        assertTrue(result.isAvailable());
        assertFalse(calculate(recipe(input, null), 101, false, new PriceSnapshot(Collections.emptyMap(), NOW)).isAvailable());
        assertTrue(calculate(recipe(input, null), 0, false, new PriceSnapshot(Collections.emptyMap(), NOW)).isAvailable());
    }

    @Test
    public void taxFloorCapAndOverflow()
    {
        assertEquals(0, CostCalculator.geTax(49, 2, TAX));
        assertEquals(2, CostCalculator.geTax(99, 2, TAX));
        assertEquals(5_000_000, CostCalculator.geTax(2_450_000_000L, 1, TAX));
        assertEquals(5_000_000, CostCalculator.geTax(Long.MAX_VALUE, 1, TAX));
        PriceSnapshot snapshot = new PriceSnapshot(Collections.singletonMap(1, new WikiPrice(Long.MAX_VALUE, null, 900L, null)), NOW);
        assertEquals(CostResult.State.OUT_OF_RANGE,
            calculate(recipe(new ItemQuantity(1, "input", 2), null), 50, false, snapshot).getState());
    }

    @Test
    public void exactQuantityAndBatchBoundaries()
    {
        ItemQuantity jug = new ItemQuantity(1, "jug", new BigDecimal("0.01"), ItemQuantity.Pricing.GE, 0, 1, false, false);
        assertEquals(2, jug.total(101, 28, 1, false));
        ItemQuantity talisman = new ItemQuantity(2, "talisman", BigDecimal.ONE, ItemQuantity.Pricing.GE, 0, 1, true, false);
        assertEquals(2, talisman.total(29, 28, 1, false));
        ItemQuantity necklace = new ItemQuantity(3, "necklace", BigDecimal.ONE, ItemQuantity.Pricing.GE, 0, 16, true, false);
        assertEquals(1, necklace.total(448, 28, 1, false));
        assertEquals(2, necklace.total(449, 28, 1, false));
        assertEquals(1, jug.total(199, 28, 1, true));
    }

    @Test
    public void fixedUntradeableAndExcludedOutput()
    {
        ItemQuantity fee = new ItemQuantity(-1, "fee", 1, 1050L);
        ItemQuantity output = new ItemQuantity(2, "output", 1);
        PriceSnapshot snapshot = new PriceSnapshot(Collections.emptyMap(), NOW);
        assertEquals(1050, calculate(recipe(fee, output), 50, false, snapshot).getInputCost());
        assertTrue(calculate(recipe(fee, output), 50, false, snapshot).isAvailable());
        assertFalse(calculate(recipe(fee, output), 50, true, snapshot).isAvailable());
        ItemQuantity slime = new ItemQuantity(3, "slime", BigDecimal.ONE, ItemQuantity.Pricing.UNTRADEABLE, 0, 1, false, false);
        assertTrue(recipe(slime, null).getPriceItemIds(true, true).isEmpty());
        CostResult result = calculate(recipe(slime, null), 50, true, snapshot);
        assertTrue(result.isAvailable());
        assertTrue(result.getRequiredInputLines().get(0).contains("Not GE-priced"));
    }

    @Test
    public void actualSideAgeAndStartingLevelMultiplier()
    {
        ItemQuantity output = new ItemQuantity(1, "output", 1);
        SkillingMethod method = new SkillingMethod(net.runelite.api.Skill.RUNECRAFT, "runes", "runes", null, null, 1, 5,
            Collections.emptyList(), Collections.emptyList(), Arrays.asList(output), "essence", "source", "assumption", new int[]{11,22});
        PriceSnapshot snapshot = new PriceSnapshot(Collections.singletonMap(1, new WikiPrice(20L, 10L, 990L, 100L)), NOW);
        CostResult result = CostCalculator.calculate(method, 22, 0, 99, 5, true, true, false, 28, snapshot, TAX, NOW);
        assertEquals(30, result.getOutputGross());
        assertEquals(100, result.getOldestPriceTimestamp());
        assertTrue(result.getNotices().stream().anyMatch(x -> x.contains("ten minutes")));
        assertTrue(result.getNotices().stream().anyMatch(x -> x.contains("held constant")));
    }

    @Test
    public void fallbackUnknownAgeAndSignedTotalBoundaries()
    {
        PriceSnapshot unknown = new PriceSnapshot(Collections.singletonMap(1, new WikiPrice(null, 10L, null, null)), NOW);
        CostResult fallback = calculate(recipe(new ItemQuantity(1, "input", 1), null), 50, false, unknown);
        assertEquals(10, fallback.getInputCost());
        assertTrue(fallback.getNotices().stream().anyMatch(note -> note.contains("Opposite-side")));
        assertTrue(fallback.getNotices().stream().anyMatch(note -> note.contains("age unknown")));
        assertEquals(0, fallback.getOldestPriceTimestamp());
        PriceSnapshot edge = new PriceSnapshot(Collections.singletonMap(1, new WikiPrice(Long.MAX_VALUE, null, 900L, null)), NOW);
        assertEquals(-Long.MAX_VALUE, calculate(recipe(new ItemQuantity(1, "input", 1), null), 50, false, edge).getNet());
        SkillingMethod twoInputs = new SkillingMethod(net.runelite.api.Skill.MAGIC, "two inputs", null, null, 1, 50,
            Arrays.asList(new ItemQuantity(1, "first", 1), new ItemQuantity(1, "second", 1)), Collections.emptyList(), Collections.emptyList());
        assertEquals(CostResult.State.OUT_OF_RANGE, calculate(twoInputs, 50, false, edge).getState());
    }

    @Test(expected = ArithmeticException.class)
    public void taxQuantityMultiplicationIsChecked()
    {
        CostCalculator.geTax(2_450_000_000L, Long.MAX_VALUE, TAX);
    }
}
