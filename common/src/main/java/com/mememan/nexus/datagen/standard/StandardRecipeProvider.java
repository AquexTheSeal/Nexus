package com.mememan.nexus.datagen.standard;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
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
    protected final boolean validateAllEntries;

    public StandardRecipeProvider(PackOutput targetPackOutput, String modId, boolean validateAllEntries) {
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
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);

        this.validateAllEntries = validateAllEntries;
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> recipeActionConsumer) {
        if (!mappedModBPWs.isEmpty()) {
            mappedModBPWs.forEach((blockSupEntry, bpwEntry) -> {
                Function<Consumer<FinishedRecipe>, Consumer<Supplier<Block>>> mappedRecipe = bpwEntry.getRecipeMappingFunction();

                if (mappedRecipe != null) {
                    NexusConstants.LOGGER.debug("[{}] [Generating Block Recipe]: {}", modId, blockSupEntry.get().getDescriptionId());

                    mappedRecipe.apply(recipeActionConsumer).accept(blockSupEntry);
                } else if (validateAllEntries() || bpwEntry.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) throw new NullPointerException(String.format("Missing recipe for block: %s, required by mod: %s, either because validateAllEntries is set to true for this provider or the block itself requires validation through BlockPropertyWrapper#getProviderRequisites().", blockSupEntry.get().getDescriptionId(), modId));
            });
        }

        if (!mappedItemIPWs.isEmpty()) {
            mappedItemIPWs.forEach((itemSupEntry, ipwEntry) -> {
                Function<Consumer<FinishedRecipe>, Consumer<Supplier<Item>>> mappedRecipe = ipwEntry.getRecipeMappingFunction();

                if (mappedRecipe != null) {
                    NexusConstants.LOGGER.debug("[{}] [Generating Item Recipe]: {}", modId, itemSupEntry.get().getDescriptionId());

                    mappedRecipe.apply(recipeActionConsumer).accept(itemSupEntry);
                } else if (validateAllEntries() || ipwEntry.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) throw new NullPointerException(String.format("Missing recipe for item: %s, required by mod: %s, either because validateAllEntries is set to true for this provider or the item itself requires validation through ItemPropertyWrapper#getProviderRequisites().", itemSupEntry.get().getDescriptionId(), modId));
            });
        }
    }

    @Override
    public @NotNull String getName() {
        return super.getName() + " [" + getModId() + "]";
    }

    @Override
    public @NotNull String getModId() {
        return modId;
    }

    @Override
    public boolean validateAllEntries() {
        return validateAllEntries;
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.RECIPE_PROVIDER;
    }
}
