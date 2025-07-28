package com.mememan.nexus.datagen.standard.model;

import com.google.gson.JsonElement;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.ModelProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class StandardModelProvider<JES extends Supplier<JsonElement>> extends ModelProvider implements ModDataProvider {
    protected final String modId;
    protected final boolean validateAllEntries;
    protected final DuplicateDataPolicy dupeStrat;

    public StandardModelProvider(PackOutput targetPackOutput, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(targetPackOutput);

        this.modId = modId;
        this.validateAllEntries = validateAllEntries;
        this.dupeStrat = dupeStrat;
    }

    public abstract void generateModels();

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        return super.run(cachedOutput);
    }

    @Override
    public @NotNull String getName() {
        return String.format("Models [%s] [%s]", getTypeName(), getModId());
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

    public abstract String getTypeName();

    @Override
    public @NotNull DuplicateDataPolicy getDuplicateDataPolicy() {
        return dupeStrat;
    }
}
