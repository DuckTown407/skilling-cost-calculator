package com.skillingcost;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
    name = "Skilling Cost Calculator",
    description = "Calculates skilling method cost with current XP and GE prices",
    tags = {"skilling", "calculator", "crafting", "herblore", "prayer", "ge", "prices"}
)
public class SkillingCostPlugin extends Plugin implements CalculatorController
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private SkillingCostConfig config;
    @Inject private PriceService priceService;
    @Inject private MethodRepository methodRepository;

    private volatile SkillingCostPanel panel;
    private NavigationButton navButton;
    private long panelGeneration;
    private volatile ExecutorService worker;
    private volatile long lifecycle;

    @Override
    protected void startUp()
    {
        long generation = ++lifecycle;
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "skilling-cost-calculator");
            thread.setDaemon(true);
            return thread;
        });
        worker = executor;
        executor.execute(() -> {
            try
            {
                // Bundled-resource IO and parsing run off the client thread.
                methodRepository.getMethodsBySkill();
                SwingUtilities.invokeLater(() -> {
                    if (generation != lifecycle) { return; }
                    removePanel();
                    panel = new SkillingCostPanel(this, methodRepository);
                    panelGeneration = generation;
                    navButton = NavigationButton.builder().tooltip("Skilling Cost Calculator")
                        .icon(ImageUtil.loadImageResource(SkillingCostPlugin.class, "/icon.png"))
                        .priority(8).panel(panel).build();
                    clientToolbar.addNavigation(navButton);
                });
            }
            catch (RuntimeException exception)
            {
                log.debug("Cannot load bundled skilling methods", exception);
            }
        });
    }

    @Override
    protected void shutDown()
    {
        long stoppedGeneration = ++lifecycle;
        priceService.cancel();
        ExecutorService executor = worker;
        worker = null;
        if (executor != null) { executor.shutdownNow(); }
        SwingUtilities.invokeLater(() -> {
            if (panelGeneration < stoppedGeneration) { removePanel(); }
        });
    }

    /** All panel/navigation ownership changes are serialized on the Swing EDT. */
    private void removePanel()
    {
        if (panel != null) { panel.dispose(); panel = null; }
        if (navButton != null) { clientToolbar.removeNavigation(navButton); navButton = null; }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"skilling-cost-calculator".equals(event.getGroup())) { return; }
        if ("enableWikiPrices".equals(event.getKey())) { priceService.cancel(); }
        long generation = lifecycle;
        SwingUtilities.invokeLater(() -> {
            SkillingCostPanel current = panel;
            if (generation == lifecycle && current != null) { current.pricesChanged(); }
        });
    }

    @Override
    public void loadCurrentSkillXp(Skill skill, IntConsumer callback)
    {
        long generation = lifecycle;
        clientThread.invokeLater(() -> {
            if (generation != lifecycle) { return; }
            int xp = client.getGameState() == GameState.LOGGED_IN
                ? Math.max(0, client.getSkillExperience(skill)) : -1;
            SwingUtilities.invokeLater(() -> {
                if (generation == lifecycle) { callback.accept(xp); }
            });
        });
    }

    @Override
    public void estimate(CalculationRequest request, Consumer<Map<String, CostResult>> success, Consumer<String> failure)
    {
        long generation = lifecycle;
        ExecutorService executor = worker;
        if (executor == null || executor.isShutdown()) { return; }
        CostCalculator.TaxSettings tax = new CostCalculator.TaxSettings(
            config.taxBasisPoints(), config.taxCapGp(), config.taxExemptBelowGp());
        priceService.getLatestSnapshot(request.priceIds(), request.refresh, snapshot -> {
            if (generation != lifecycle || executor.isShutdown()) { return; }
            try
            {
                executor.execute(() -> {
                    if (generation != lifecycle) { return; }
                    Map<String, CostResult> results = new LinkedHashMap<>();
                    Instant now = Instant.now();
                    for (SkillingMethod method : request.methods)
                    {
                        results.put(method.getId(), CostCalculator.calculate(method,
                            request.currentLevel, request.currentXp, request.targetLevel, request.targetXp,
                            request.optional, request.sell, request.tax, request.batchSize, snapshot, tax, now));
                    }
                    Map<String, CostResult> immutable = Collections.unmodifiableMap(results);
                    SwingUtilities.invokeLater(() -> {
                        if (generation == lifecycle) { success.accept(immutable); }
                    });
                });
            }
            catch (java.util.concurrent.RejectedExecutionException ignored)
            {
                // Shutdown raced with a response; its panel has already been invalidated.
            }
        }, message -> SwingUtilities.invokeLater(() -> {
            if (generation == lifecycle) { failure.accept(message); }
        }));
    }

    @Provides
    SkillingCostConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(SkillingCostConfig.class);
    }
}
