package com.mememan.nexus.platform.services;

import com.google.common.collect.HashMultimap;
import com.mememan.nexus.datagen.ModDatagenConfig;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * A loader-agnostic {@code interface} whose purpose is to handle the registration of segregated mod-specific data
 * providers.
 * <br></br>
 * Primarily designed to accept provider implementations regardless of whether they're loader-specific or
 * common implementations.
 * <br></br>
 * Main use case is to allow for mods to modify Nexus API's behaviour when auto-generating related data. Mods should
 * otherwise prefer writing/implementing their own data providers.
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
     * Registers a {@link DataProvider} with minimal context (using only a {@link PackOutput}). Providers are run based
     * on their own data, meaning that if you were to pass a {@link ModDataProvider} subtype, it would output data based
     * on the {@link ModDataProvider#getModId()} specified within the output {@code DP's} constructor.
     * <br></br>
     * If you're registering loader-specific data providers that require loader-specific parameters beyond what vanilla
     * (or your own code) offers, you should use your respective loader to register them as necessary. Nexus only aims to
     * provide a data provider registration entrypoint for code within {@code common} modules and allow for mods to
     * configure Nexus API's native datagen.
     *
     * @param modId The parent mod ID under which the target provider should be registered.
     * @param dataProvider The provider to register. Takes an input of {@link PackOutput} and
     *                     {@code CompletableFuture<HolderLookup.Provider>} to be passed into the output
     *                     {@linkplain Pair} of whether the provided {@code DP} is client or server-side and the {@code DP}
     *                     itself.
     *
     * @param <DP> Any subtype of {@link DataProvider} to be registered and run during datagen.
     *
     * @apiNote For clarity, it is completely plausible to write data providers for custom objects within your {@code common}
     * module and pass them into here, since you would be controlling the parameters they need in order to be constructed
     * and run. The loader-specific providers referred to above are in reference to providers supplied by Neo/Forge, Fabric,
     * etc. which are typically extensions of Vanilla providers with their own additions and/or nuances.
     *
     * @implNote Loader-specific implementations will ignore the left output {@code boolean} if the {@code DP} is an
     * instance of {@linkplain ModDataProvider}, since {@linkplain ModDataProvider#getProviderType()} would be used to
     * validate sides instead.
     */
    <DP extends DataProvider> void registerDataProvider(String modId, final BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, Pair<Boolean, DP>> dataProvider);

    /**
     * Alternate variant of {@linkplain #registerDataProvider(String, BiFunction)}, specifically tailored for registering
     * {@link ModDataProvider} types. Useful as a shortcut method that additionally handles providing the metadata needed
     * to run generators that rely on storing/consuming mod IDs or otherwise simply extend {@link ModDataProvider}.
     *
     * @param modId The parent ID mod under which the target provider should be registered.
     * @param modDataProvider The mod provider to register. Takes an input of {@link PackOutput} and
     *                        {@code CompletableFuture<HolderLookup.Provider>} to be passed into the output {@code DP}.
     *
     * @param <MDP> Any subtype of {@link ModDataProvider} to be registered and run during datagen.
     */
    <MDP extends ModDataProvider> void registerModDataProvider(String modId, final BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, MDP> modDataProvider);

    /**
     * Registers a {@link ModDatagenConfig}. Allows for more flexibility and control over how Nexus API handles datagen
     * for specific mods.
     *
     * @param modDatagenConfig The datagen config to register.
     *
     * @return The registered {@link ModDatagenConfig}.
     */
    ModDatagenConfig registerConfigForMod(ModDatagenConfig modDatagenConfig);

    /**
     *
     *
     * @return
     */
    Set<ModDatagenConfig> getModDatagenConfigs();

    @Nullable
    default ModDatagenConfig getConfigForMod(String modId) {
        return getModDatagenConfigs().stream()
                .filter(curConfig -> curConfig.modId().equals(modId))
                .findFirst()
                .orElse(null);
    }

    /**
     *
     *
     * @return
     */
    default HashMultimap<String, ProviderType> getDisabledDataProviders() {
        HashMultimap<String, ProviderType> mappedDisabledProviders = HashMultimap.create();

        if (getModDatagenConfigs().isEmpty()) return mappedDisabledProviders;

        getModDatagenConfigs().forEach(curModDGConfig -> {
            mappedDisabledProviders.asMap()
                    .computeIfAbsent(curModDGConfig.modId(), oK -> new ObjectOpenHashSet<>())
                    .addAll(curModDGConfig.disabledProviderTypes());
        });

        return mappedDisabledProviders;
    }

    /**
     *
     *
     * @return
     */
    default ObjectOpenHashSet<ModDatagenConfig> getDisabledMods() {
        return getModDatagenConfigs().isEmpty() ? ObjectOpenHashSet.of() : getModDatagenConfigs().stream()
                .filter(curModDGConfig -> !curModDGConfig.enableDatagen())
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));
    }

    /**
     * Gets the global {@link net.minecraft.data.DataGenerator} instance running, if available.
     * <br></br>
     * Should be fairly obvious that this will return {@code null} if you're not running a datagen Gradle task (or you
     * try accessing it too early).
     *
     * @return The globally-running {@link net.minecraft.data.DataGenerator} instance. May be {@code null}.
     */
    @Nullable
    net.minecraft.data.DataGenerator getDataGenerator();
}
