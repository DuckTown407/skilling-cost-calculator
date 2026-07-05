package com.skillingcost;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
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
import net.runelite.client.util.ImageUtil;

/**
 * Plugin entry point. Owns the side panel, reads XP from the client, and orchestrates the
 * async flow: fetch prices ({@link PriceService}) -> compute costs ({@link CostCalculator})
 * -> hand results back to the panel on the Swing thread.
 *
 * Stale-response handling: every user action that invalidates in-flight work bumps
 * {@code calculationRequestId}; responses carrying an older id are dropped.
 */
@PluginDescriptor(
    name = "Skilling Cost Calculator",
    description = "Calculates skilling method cost with current XP and GE prices",
    tags = {"skilling", "calculator", "crafting", "herblore", "prayer", "ge", "prices"}
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

    @Inject
    private MethodRepository methodRepository;

    private SkillingCostPanel panel;
    private NavigationButton navButton;
    private int calculationRequestId;

    @Override
    protected void startUp()
    {
        panel = new SkillingCostPanel(this, methodRepository);
        navButton = NavigationButton.builder()
            .tooltip("Skilling Cost Calculator")
            .icon(loadIcon())
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

    /** Drops any in-flight calculation whose result has not been displayed yet. */
    void invalidatePendingCalculations()
    {
        calculationRequestId++;
    }

    /**
     * Reads the player's XP in the given skill on the client thread. The callback receives -1
     * when not logged in, and always runs on the Swing thread.
     */
    void loadCurrentSkillXp(Skill skill, IntConsumer callback)
    {
        clientThread.invokeLater(() -> {
            int xp = client.getGameState() == GameState.LOGGED_IN
                ? Math.max(0, client.getSkillExperience(skill))
                : -1;
            SwingUtilities.invokeLater(() -> callback.accept(xp));
        });
    }

    /**
     * Estimates net gp for a batch of methods (used by the "most profitable" sort). One price
     * fetch covers all methods. Callbacks run on the Swing thread.
     */
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

        final int xpNeeded = Math.max(0, Math.max(0, targetXp) - Math.max(0, currentXp));
        final CostCalculator.TaxSettings tax = taxSettings();

        priceService.getLatestPrices(requiredItemIds, prices -> {
            Map<SkillingMethod, Long> estimates = new HashMap<>();
            for (SkillingMethod method : methods)
            {
                long actionsNeeded = (long) Math.ceil(xpNeeded / method.getXpPerAction());
                estimates.put(method, CostCalculator.estimateNet(
                    method, actionsNeeded, includeOptionalInputs,
                    sellOutputs && method.hasOutputs(), includeTax, prices, tax));
            }
            SwingUtilities.invokeLater(() -> onSuccess.accept(estimates));
        }, error -> SwingUtilities.invokeLater(() -> onError.accept(error)));
    }

    /** Runs the full breakdown for one method and shows it in the panel. */
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
        final CostCalculator.TaxSettings tax = taxSettings();

        int currentLevel = Math.min(99, Experience.getLevelForXp(safeCurrentXp));
        int xpNeeded = Math.max(0, safeTargetXp - safeCurrentXp);
        long actionsNeeded = (long) Math.ceil(xpNeeded / method.getXpPerAction());

        panel.showStatus("Fetching prices…");
        priceService.getLatestPrices(method.getPriceItemIds(includeOptionalInputs, shouldSellOutputs), prices -> {
            if (requestId != calculationRequestId || panel == null)
            {
                return;
            }

            CostResult result = CostCalculator.buildResult(
                method,
                currentLevel,
                safeCurrentXp,
                targetLevel,
                safeTargetXp,
                xpNeeded,
                actionsNeeded,
                includeOptionalInputs,
                shouldSellOutputs,
                includeTax,
                prices,
                tax);

            SwingUtilities.invokeLater(() -> {
                if (requestId == calculationRequestId && panel != null)
                {
                    panel.showResult(result);
                }
            });
        }, error -> SwingUtilities.invokeLater(() -> {
            if (requestId == calculationRequestId && panel != null)
            {
                panel.showStatus(error);
            }
        }));
    }

    private CostCalculator.TaxSettings taxSettings()
    {
        return new CostCalculator.TaxSettings(
            config.taxBasisPoints(),
            config.taxCapGp(),
            config.taxExemptBelowGp());
    }

    private static BufferedImage loadIcon()
    {
        return ImageUtil.loadImageResource(SkillingCostPlugin.class, "/icon.png");
    }

    @Provides
    SkillingCostConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SkillingCostConfig.class);
    }
}
