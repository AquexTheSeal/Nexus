package com.mememan.nexus.platform.services;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.data.DataProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A loader-agnostic {@code interface} whose purpose is to
 */
public interface DataGenerator {

    @ApiStatus.Internal
    void setupDataGenerator();

    <DP extends DataProvider> DP registerDataProvider(final DP dataProvider);

    <DP extends DataProvider> Map<String, Pair<Boolean, DP>> getMappedDataProviders();

    @Nullable
    net.minecraft.data.DataGenerator getDataGenerator();
}
