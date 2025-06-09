package com.mememan.nexus.internal.services;

import com.mememan.nexus.datagen.ModDatagenConfig;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import com.mememan.nexus.platform.services.DataGenerator;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class FabricDataGenerator implements DataGenerator {

    @Override
    public void setupDataGenerator() {

    }

    @Override
    public <DP extends DataProvider> void registerDataProvider(String modId, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, Pair<Boolean, DP>> dataProvider) {

    }

    @Override
    public <MDP extends ModDataProvider> void registerModDataProvider(String modId, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, MDP> modDataProvider) {

    }

    @Override
    public ModDatagenConfig registerConfigForMod(ModDatagenConfig modDatagenConfig) {
        return null;
    }

    @Override
    public Set<ModDatagenConfig> getModDatagenConfigs() {
        return Set.of();
    }

    @Override
    public @Nullable net.minecraft.data.DataGenerator getDataGenerator() {
        return null;
    }
}
