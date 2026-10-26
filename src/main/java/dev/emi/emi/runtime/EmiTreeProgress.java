package dev.emi.emi.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.bom.MaterialTree;
import dev.emi.emi.bom.TreeCost;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.screen.BoMScreen;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;

/** Client-thread coordinator. The worker only owns calculation nodes and an inventory snapshot. */
public final class EmiTreeProgress {
    private static final Executor WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "EMI recipe tree");
        thread.setDaemon(true);
        return thread;
    });
    private static CompletableFuture<Outcome> pending;
    private static Request requested;
    private static Result current;

    record Request(MaterialTree tree, long revision, long batches, boolean crafting,
            EmiPlayerInventory inventory, Object screen) {
        boolean matches(MaterialTree tree, EmiPlayerInventory inventory, Object screen) {
            return this.tree == tree && revision == tree.getRevision() && batches == tree.batches
                    && crafting == BoM.craftingMode && this.inventory == inventory && this.screen == screen;
        }
    }

    public record Result(MaterialTree calculated, TreeCost total, EmiFavorites.SyntheticResult synthetic) {}

    private record Outcome(Result result, Throwable error) {}

    public static void update(EmiPlayerInventory inventory) {
        Minecraft client = Minecraft.getInstance();
        MaterialTree tree = BoM.tree;
        boolean active = tree != null && inventory != null
                && (BoM.craftingMode || client.screen instanceof BoMScreen);
        if ((!active || !BoM.craftingMode) && !EmiFavorites.syntheticFavorites.isEmpty()) {
            EmiFavorites.syntheticFavorites.clear();
            EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
        }
        if (pending != null && pending.isDone()) {
            Outcome outcome = pending.join();
            pending = null;
            if (active && requested.matches(tree, inventory, client.screen)) {
                if (outcome.error != null) {
                    EmiLog.error("Failed to calculate recipe tree progress", outcome.error);
                } else {
                    current = outcome.result;
                    tree.goal.applyProgress(current.calculated.goal);
                    tree.cost = current.calculated.cost;
                    if (BoM.craftingMode) {
                        EmiFavorites.syntheticFavorites.clear();
                        EmiFavorites.syntheticFavorites.addAll(current.synthetic.favorites());
                        if (current.synthetic.complete()) {
                            BoM.craftingMode = false;
                        }
                        EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
                    }
                    if (client.screen instanceof BoMScreen screen) {
                        screen.applyCosts(current.total, current.calculated.cost);
                    }
                }
            }
        }
        if (!active) {
            current = null;
            if (pending == null) {
                requested = null;
            }
            return;
        }
        // At most one task is outstanding. While it runs, newer inputs replace the desired request.
        if (pending == null && (requested == null || !requested.matches(tree, inventory, client.screen))) {
            requested = new Request(tree, tree.getRevision(), tree.batches, BoM.craftingMode, inventory, client.screen);
            current = null;
            MaterialTree snapshot = tree.copyForCalculation();
            EmiPlayerInventory inventorySnapshot = inventory.copy();
            boolean crafting = BoM.craftingMode;
            pending = CompletableFuture.supplyAsync(() -> calculate(snapshot, inventorySnapshot, crafting), WORKER)
                    .handle(Outcome::new);
        }
    }

    public static Result getCurrent(MaterialTree tree) {
        if (current != null && requested.tree == tree && requested.revision == tree.getRevision()
                && requested.batches == tree.batches && requested.crafting == BoM.craftingMode) {
            return current;
        }
        return null;
    }

    static Result calculate(MaterialTree snapshot, EmiPlayerInventory inventory, boolean crafting) {
        TreeCost total = new TreeCost();
        total.calculate(snapshot.goal, snapshot.batches);
        EmiFavorites.SyntheticResult synthetic = null;
        if (crafting) {
            synthetic = EmiFavorites.calculateSynthetic(snapshot, inventory, total);
        } else {
            snapshot.calculateProgress(inventory);
        }
        return new Result(snapshot, total, synthetic);
    }
}
