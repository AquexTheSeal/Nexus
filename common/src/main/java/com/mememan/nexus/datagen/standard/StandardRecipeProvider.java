package com.mememan.nexus.datagen.standard;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Standard loader-agnostic mod-specific recipe provider in Nexus API. Instanced based on the provided mod ID.
 */
public class StandardRecipeProvider extends RecipeProvider implements ModDataProvider {
    protected final String modId;
    protected final Object2ObjectOpenHashMap<Supplier<Block>, BlockPropertyWrapper> mappedModBPWs;
    protected final Object2ObjectOpenHashMap<Supplier<Item>, ItemPropertyWrapper> mappedItemIPWs;

    public StandardRecipeProvider(PackOutput targetPackOutput, String modId) {
        super(targetPackOutput);

        this.modId = modId;

        this.mappedModBPWs = BlockPropertyWrapper.getMappedBpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.BLOCK.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
        this.mappedItemIPWs = ItemPropertyWrapper.getMappedIpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.ITEM.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);;
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> recipeActionConsumer) {
        if (!mappedModBPWs.isEmpty()) {
            mappedModBPWs.forEach((blockSupEntry, bpwEntry) -> {
                Function<Consumer<FinishedRecipe>, Consumer<Supplier<Block>>> mappedRecipe = bpwEntry.getRecipeMappingFunction();

                if (mappedRecipe != null) {
                    NexusConstants.LOGGER.debug("[{}] [Generating Block Recipe]: {}", modId, blockSupEntry.get().getDescriptionId());

                    mappedRecipe.apply(recipeActionConsumer).accept(blockSupEntry);
                }
            });
        }

        if (!mappedItemIPWs.isEmpty()) {
            mappedItemIPWs.forEach((itemSupEntry, ipwEntry) -> {
                Function<Consumer<FinishedRecipe>, Consumer<Supplier<Item>>> mappedRecipe = ipwEntry.getRecipeMappingFunction();

                if (mappedRecipe != null) {
                    NexusConstants.LOGGER.debug("[{}] [Generating Item Recipe]: {}", modId, itemSupEntry.get().getDescriptionId());

                    mappedRecipe.apply(recipeActionConsumer).accept(itemSupEntry);
                }
            });
        }
    }

    @Override
    public @NotNull String getModId() {
        return modId;
    }

    @Override
    public boolean validateAllEntries() {
        return false;
    }
}
