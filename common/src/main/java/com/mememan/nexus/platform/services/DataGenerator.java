package com.mememan.nexus.platform.services;

import net.minecraft.data.DataProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface DataGenerator {

    @ApiStatus.Internal
    void setupDataGenerator();

    <DP extends DataProvider> DP registerDataProvider(final DP dataProvider);

    @Nullable
    net.minecraft.data.DataGenerator getDataGenerator();
}
