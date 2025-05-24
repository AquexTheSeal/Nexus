package com.mememan.nexus.platform.services;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * A loader-agnostic {@code interface} whose purpose is to handle the registration of segregated mod-specific data
 * providers.
 */
public interface DataGenerator {

    /**
     * Bootstrap method responsible for setting the data generator service up and notifying/calling all data provider
     * registrars for processing/setup.
     * <br></br>
     * Should <b>NOT</b> be called anywhere else!
     */
    @ApiStatus.Internal
    @ApiStatus.OverrideOnly
    void setupDataGenerator();

    /**
     * 
     *
     * @param dataProvider
     *
     * @return
     *
     * @param <DP>
     */
    <DP extends DataProvider> DP registerDataProvider(final Function<PackOutput, DP> dataProvider);

    <DP extends DataProvider> Map<String, Pair<Boolean, DP>> getMappedDataProviders();

    /**
     * Gets the global {@link DataGenerator} instance running, if available.
     * <br></br>
     * Should be fairly obvious that this will return {@code null} if you're not running a datagen Gradle task.
     *
     * @return The globally-running {@link DataGenerator} instance. May be {@code null}.
     */
    @Nullable
    net.minecraft.data.DataGenerator getDataGenerator();
}
