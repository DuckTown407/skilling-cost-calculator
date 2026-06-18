package com.skillingcost;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
    name = "Skilling Cost Calculator",
    description = "Calculates skilling method cost with current XP and GE prices",
    tags = {"skilling", "calculator", "crafting", "herblore", "ge", "prices"}
)
public class SkillingCostPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private SkillingCostConfig config;

    @Inject
    private PriceService priceService;

    private SkillingCostPanel panel;
    private NavigationButton navButton;
    private int calculationRequestId;

    @Override
    protected void startUp()
    {
        panel = new SkillingCostPanel(this);
        navButton = NavigationButton.builder()
            .tooltip("Skilling Cost Calculator")
            .icon(createIcon())
            .priority(8)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        priceService.cancel();
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
        navButton = null;
        panel = null;
    }

    void invalidatePendingCalculations()
    {
        calculationRequestId++;
    }

    void loadCurrentSkillXp(Skill skill, IntConsumer callback)
    {
        clientThread.invokeLater(() -> {
            int xp = client.getGameState() == GameState.LOGGED_IN
                ? Math.max(0, client.getSkillExperience(skill))
                : -1;
            javax.swing.SwingUtilities.invokeLater(() -> callback.accept(xp));
        });
    }

    void estimateNetForMethods(
        List<SkillingMethod> methods,
        int currentXp,
        int targetXp,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        boolean includeTax,
        Consumer<Map<SkillingMethod, Long>> onSuccess,
        Consumer<String> onError)
    {
        if (!config.enableWikiPrices())
        {
            onError.accept("Enable Wiki prices in plugin config first.");
            return;
        }

        Set<Integer> requiredItemIds = new LinkedHashSet<>();
        for (SkillingMethod method : methods)
        {
            requiredItemIds.addAll(method.getPriceItemIds(includeOptionalInputs, sellOutputs && method.hasOutputs()));
        }

        final int safeCurrentXp = Math.max(0, currentXp);
        final int safeTargetXp = Math.max(0, targetXp);
        final int xpNeeded = Math.max(0, safeTargetXp - safeCurrentXp);

        priceService.getLatestPrices(requiredItemIds, prices -> {
            Map<SkillingMethod, Long> estimates = new HashMap<>();
            for (SkillingMethod method : methods)
            {
                long actionsNeeded = (long) Math.ceil(xpNeeded / method.getXpPerAction());
                estimates.put(method, estimateNet(method, actionsNeeded, includeOptionalInputs, sellOutputs && method.hasOutputs(), includeTax, prices));
            }

            javax.swing.SwingUtilities.invokeLater(() -> onSuccess.accept(estimates));
        }, error -> javax.swing.SwingUtilities.invokeLater(() -> onError.accept(error)));
    }

    void calculate(
        SkillingMethod method,
        int currentXp,
        int targetLevel,
        int targetXp,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        boolean includeTax)
    {
        if (!config.enableWikiPrices())
        {
            panel.showStatus("Enable Wiki prices in plugin config first.");
            return;
        }

        final int requestId = ++calculationRequestId;
        final int safeCurrentXp = Math.max(0, currentXp);
        final int safeTargetXp = Math.max(0, targetXp);
        final boolean shouldSellOutputs = sellOutputs && method.hasOutputs();
        final boolean shouldIncludeOptionalInputs = includeOptionalInputs;
        final boolean shouldIncludeTax = includeTax;

        int currentLevel = Math.min(99, Experience.getLevelForXp(safeCurrentXp));
        int xpNeeded = Math.max(0, safeTargetXp - safeCurrentXp);
        long actionsNeeded = (long) Math.ceil(xpNeeded / method.getXpPerAction());

        panel.showStatus("Fetching prices…");
        priceService.getLatestPrices(method.getPriceItemIds(shouldIncludeOptionalInputs, shouldSellOutputs), prices -> {
            if (requestId != calculationRequestId || panel == null)
            {
                return;
            }

            CostResult result = buildResult(
                method,
                currentLevel,
                safeCurrentXp,
                targetLevel,
                safeTargetXp,
                xpNeeded,
                actionsNeeded,
                shouldIncludeOptionalInputs,
                shouldSellOutputs,
                shouldIncludeTax,
                prices
            );

            javax.swing.SwingUtilities.invokeLater(() -> {
                if (requestId == calculationRequestId && panel != null)
                {
                    panel.showResult(result);
                }
            });
        }, error -> javax.swing.SwingUtilities.invokeLater(() -> {
            if (requestId == calculationRequestId && panel != null)
            {
                panel.showStatus(error);
            }
        }));
    }

    private long estimateNet(
        SkillingMethod method,
        long actionsNeeded,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        boolean includeTax,
        Map<Integer, WikiPrice> prices)
    {
        long inputCost = 0;
        long outputGross = 0;
        long tax = 0;

        for (ItemQuantity input : method.getRequiredInputs())
        {
            WikiPrice price = prices.get(input.getItemId());
            int unitBuy = price == null ? 0 : price.getInstantBuyPrice();
            inputCost += input.getTotalQuantity(actionsNeeded) * unitBuy;
        }

        if (includeOptionalInputs)
        {
            for (ItemQuantity input : method.getOptionalInputs())
            {
                WikiPrice price = prices.get(input.getItemId());
                int unitBuy = price == null ? 0 : price.getInstantBuyPrice();
                inputCost += input.getTotalQuantity(actionsNeeded) * unitBuy;
            }
        }

        if (sellOutputs)
        {
            for (ItemQuantity output : method.getOutputs())
            {
                WikiPrice price = prices.get(output.getItemId());
                int unitSell = price == null ? 0 : price.getInstantSellPrice();
                long quantity = output.getTotalQuantity(actionsNeeded);
                outputGross += quantity * unitSell;
                tax += includeTax ? calculateGeTax(unitSell, quantity) : 0;
            }
        }

        return outputGross - tax - inputCost;
    }

    private CostResult buildResult(
        SkillingMethod method,
        int currentLevel,
        int currentXp,
        int targetLevel,
        int targetXp,
        int xpNeeded,
        long actionsNeeded,
        boolean includeOptionalInputs,
        boolean sellOutputs,
        boolean includeTax,
        Map<Integer, WikiPrice> prices
    )
    {
        long inputCost = 0;
        long outputGross = 0;
        long tax = 0;
        long newestPriceTimestamp = 0;
        List<String> requiredInputLines = new ArrayList<>();
        List<String> optionalInputLines = new ArrayList<>();
        List<String> outputLines = new ArrayList<>();

        for (ItemQuantity input : method.getRequiredInputs())
        {
            LinePrice line = priceInput(input, actionsNeeded, prices, false);
            inputCost += line.total;
            requiredInputLines.add(line.display);
            newestPriceTimestamp = Math.max(newestPriceTimestamp, line.timestamp);
        }

        if (includeOptionalInputs)
        {
            for (ItemQuantity input : method.getOptionalInputs())
            {
                LinePrice line = priceInput(input, actionsNeeded, prices, true);
                inputCost += line.total;
                optionalInputLines.add(line.display);
                newestPriceTimestamp = Math.max(newestPriceTimestamp, line.timestamp);
            }
        }

        if (sellOutputs)
        {
            for (ItemQuantity output : method.getOutputs())
            {
                WikiPrice price = prices.get(output.getItemId());
                int unitSell = price == null ? 0 : price.getInstantSellPrice();
                long quantity = output.getTotalQuantity(actionsNeeded);
                long lineTotal = quantity * unitSell;
                outputGross += lineTotal;
                tax += includeTax ? calculateGeTax(unitSell, quantity) : 0;
                outputLines.add(formatItemLine(output.getName(), quantity, unitSell, lineTotal));
                if (price != null)
                {
                    newestPriceTimestamp = Math.max(newestPriceTimestamp, price.getNewestTimestamp());
                }
            }
        }

        long net = outputGross - tax - inputCost;

        return new CostResult(
            method,
            currentLevel,
            currentXp,
            targetLevel,
            targetXp,
            xpNeeded,
            actionsNeeded,
            inputCost,
            outputGross,
            tax,
            net,
            requiredInputLines,
            optionalInputLines,
            outputLines,
            includeOptionalInputs,
            sellOutputs,
            newestPriceTimestamp
        );
    }

    private LinePrice priceInput(ItemQuantity item, long actionsNeeded, Map<Integer, WikiPrice> prices, boolean optional)
    {
        WikiPrice price = prices.get(item.getItemId());
        int unitBuy = price == null ? 0 : price.getInstantBuyPrice();
        long quantity = item.getTotalQuantity(actionsNeeded);
        long total = quantity * unitBuy;
        long timestamp = price == null ? 0 : price.getNewestTimestamp();
        String name = optional ? item.getName() + " (optional)" : item.getName();
        return new LinePrice(formatItemLine(name, quantity, unitBuy, total), total, timestamp);
    }

    private long calculateGeTax(int unitSellPrice, long quantity)
    {
        if (unitSellPrice < config.taxExemptBelowGp())
        {
            return 0;
        }

        long taxPerItem = ((long) unitSellPrice * config.taxBasisPoints()) / 10_000L;
        taxPerItem = Math.min(taxPerItem, config.taxCapGp());
        return taxPerItem * quantity;
    }

    private static String formatItemLine(String name, long quantity, int unitPrice, long total)
    {
        return String.format("%,d × %s", quantity, name)
            + "\n" + String.format("%,d gp each", unitPrice)
            + "\n" + formatGp(total) + " total";
    }

    static String formatGp(long value)
    {
        String sign = value < 0 ? "-" : "";
        long abs = Math.abs(value);
        return sign + String.format("%,d gp", abs);
    }

    static String displaySkill(Skill skill)
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

    private static BufferedImage createIcon()
    {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(214, 176, 72));
        graphics.fillOval(3, 3, 26, 26);
        graphics.setColor(new Color(65, 48, 23));
        graphics.drawOval(3, 3, 26, 26);
        graphics.drawString("$", 12, 21);
        graphics.dispose();
        return image;
    }

    @Provides
    SkillingCostConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SkillingCostConfig.class);
    }

    private static final class LinePrice
    {
        private final String display;
        private final long total;
        private final long timestamp;

        private LinePrice(String display, long total, long timestamp)
        {
            this.display = display;
            this.total = total;
            this.timestamp = timestamp;
        }
    }
}
