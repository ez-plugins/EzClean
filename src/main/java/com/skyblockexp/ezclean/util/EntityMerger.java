package com.skyblockexp.ezclean.util;

import com.skyblockexp.ezclean.config.MergeSettings;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges nearby identical dropped items and experience orbs into single entities.
 *
 * <p>Called once per world per cleanup pass. For each group of same-material items
 * within {@code radiusBlocks} of each other, the first item absorbs the others' amounts
 * up to the configured maximum stack size; leftover items (beyond max stack) stay in the
 * world rather than being deleted. XP orbs within the radius are summed into one orb.</p>
 *
 * <p>{@link #merge(World)} returns two counts that are added to cleanup statistics under
 * the group keys {@code merged-items} and {@code merged-orbs}.</p>
 */
public final class EntityMerger {

    private final MergeSettings settings;

    public EntityMerger(MergeSettings settings) {
        this.settings = settings;
    }

    /**
     * Runs a merge pass over all entities in the given world.
     *
     * @param world the world to process
     * @return counts of entities removed by merging, keyed by group name
     */
    public Map<String, Integer> merge(World world) {
        Map<String, Integer> counts = new HashMap<>();
        Set<Entity> processed = new HashSet<>();

        List<Entity> entities = world.getEntities();

        if (settings.isMergeItems()) {
            int merged = mergeItems(entities, processed);
            if (merged > 0) {
                counts.put("merged-items", merged);
            }
        }

        if (settings.isMergeOrbs()) {
            int merged = mergeOrbs(entities, processed);
            if (merged > 0) {
                counts.put("merged-orbs", merged);
            }
        }

        return counts;
    }

    // -----------------------------------------------------------------------------------------
    // Item merging
    // -----------------------------------------------------------------------------------------

    private int mergeItems(List<Entity> entities, Set<Entity> processed) {
        int removedCount = 0;
        double radiusSq = (double) settings.getRadiusBlocks() * settings.getRadiusBlocks();

        // Group items by material so we only do proximity checks within the same material.
        Map<Material, List<Item>> byMaterial = new HashMap<>();
        for (Entity entity : entities) {
            if (entity instanceof Item item && entity.isValid()) {
                byMaterial.computeIfAbsent(item.getItemStack().getType(), k -> new ArrayList<>()).add(item);
            }
        }

        for (Map.Entry<Material, List<Item>> entry : byMaterial.entrySet()) {
            List<Item> group = entry.getValue();
            for (int i = 0; i < group.size(); i++) {
                Item base = group.get(i);
                if (processed.contains(base) || !base.isValid()) {
                    continue;
                }
                ItemStack baseStack = base.getItemStack();
                int effectiveMax = settings.getMaxStackSize() > 0
                        ? settings.getMaxStackSize()
                        : baseStack.getMaxStackSize();

                Location baseLoc = base.getLocation();

                for (int j = i + 1; j < group.size(); j++) {
                    Item other = group.get(j);
                    if (processed.contains(other) || !other.isValid()) {
                        continue;
                    }
                    // Only merge items whose ItemStacks are truly identical (same data/meta).
                    ItemStack otherStack = other.getItemStack();
                    if (!baseStack.isSimilar(otherStack)) {
                        continue;
                    }
                    // Proximity check.
                    if (baseLoc.distanceSquared(other.getLocation()) > radiusSq) {
                        continue;
                    }

                    int baseAmount = baseStack.getAmount();
                    int otherAmount = otherStack.getAmount();
                    int available = effectiveMax - baseAmount;

                    if (available <= 0) {
                        // Base stack is full; cannot absorb more.
                        break;
                    }

                    if (otherAmount <= available) {
                        // Absorb the entire other item.
                        baseStack.setAmount(baseAmount + otherAmount);
                        base.setItemStack(baseStack);
                        other.remove();
                        processed.add(other);
                        removedCount++;
                    } else {
                        // Partial absorption: fill base to max, leave remainder.
                        baseStack.setAmount(effectiveMax);
                        base.setItemStack(baseStack);
                        otherStack.setAmount(otherAmount - available);
                        other.setItemStack(otherStack);
                        // Base is now full; move on to next base candidate.
                        break;
                    }
                }
                // Refresh local reference after potential mutations.
                baseStack = base.getItemStack();
            }
        }

        return removedCount;
    }

    // -----------------------------------------------------------------------------------------
    // XP orb merging
    // -----------------------------------------------------------------------------------------

    private int mergeOrbs(List<Entity> entities, Set<Entity> processed) {
        int removedCount = 0;
        double radiusSq = (double) settings.getRadiusBlocks() * settings.getRadiusBlocks();

        List<ExperienceOrb> orbs = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof ExperienceOrb orb && entity.isValid()) {
                orbs.add(orb);
            }
        }

        for (int i = 0; i < orbs.size(); i++) {
            ExperienceOrb base = orbs.get(i);
            if (processed.contains(base) || !base.isValid()) {
                continue;
            }
            Location baseLoc = base.getLocation();

            for (int j = i + 1; j < orbs.size(); j++) {
                ExperienceOrb other = orbs.get(j);
                if (processed.contains(other) || !other.isValid()) {
                    continue;
                }
                if (baseLoc.distanceSquared(other.getLocation()) > radiusSq) {
                    continue;
                }

                // Merge: add other's XP to base, remove other.
                // Clamp to Integer.MAX_VALUE to prevent overflow.
                long merged = (long) base.getExperience() + other.getExperience();
                base.setExperience((int) Math.min(merged, Integer.MAX_VALUE));
                other.remove();
                processed.add(other);
                removedCount++;
            }
        }

        return removedCount;
    }
}
