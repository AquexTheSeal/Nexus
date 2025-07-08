package com.mememan.nexus.datagen.standard.model;

import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.ModelProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class StandardModelProvider<T> extends ModelProvider implements ModDataProvider {
    protected final String modId;
    protected final boolean validateAllEntries;
    protected final DuplicateDataPolicy dupeStrat;
    protected final PackOutput.PathProvider blockModelPathProvider;

    public StandardModelProvider(PackOutput targetPackOutput, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(targetPackOutput);

        this.modId = modId;
        this.validateAllEntries = validateAllEntries;
        this.dupeStrat = dupeStrat;
        this.blockModelPathProvider = targetPackOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    public void generateModels() {

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
    public abstract @NotNull ProviderType getProviderType();

    @Override
    public @NotNull DuplicateDataPolicy getDuplicateDataPolicy() {
        return dupeStrat;
    }
}
