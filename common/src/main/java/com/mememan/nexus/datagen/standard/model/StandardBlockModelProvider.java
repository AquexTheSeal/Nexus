package com.mememan.nexus.datagen.standard.model;

import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ModelProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class StandardBlockModelProvider extends ModelProvider implements ModDataProvider {
    protected final String modId;
    protected final boolean validateAllEntries;
    protected final PackOutput.PathProvider blockModelPathProvider;
    protected final DuplicateDataPolicy dupeStrat;

    public StandardBlockModelProvider(PackOutput targetPackOutput, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(targetPackOutput);

        this.modId = modId;
        this.validateAllEntries = validateAllEntries;
        this.blockModelPathProvider = targetPackOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.dupeStrat = dupeStrat;
    }

    public void generateModels(BlockModelGenerators gen) {

    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        return super.run(cachedOutput);
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
        return NexusProviderTypes.BLOCK_MODEL_PROVIDER;
    }

    @Override
    public @NotNull DuplicateDataPolicy getDuplicateDataPolicy() {
        return dupeStrat;
    }
}
