package com.mememan.nexus.datagen.standard;

import com.google.gson.JsonElement;
import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.platform.NexusServices;
import com.mememan.nexus.util.ResourceLocationUtil;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StandardDatapackRegistryProvider extends RegistriesDatapackGenerator implements ModDataProvider {
    protected final String modId;
    protected final boolean validateAllEntries;
    protected final DuplicateDataPolicy dupeStrat;

    public StandardDatapackRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup, RegistrySetBuilder datapackEntriesBuilder, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, NexusServices.DATA_GENERATOR.getConfigForMod(modId) == null || !NexusServices.DATA_GENERATOR.getConfigForMod(modId).enableDatagen() ? registryLookup : registryLookup.thenApply(lookupProvider -> constructDummyRegistries(lookupProvider, datapackEntriesBuilder))); // Only start populating dummy registries when (if) we hit a mod whose datagen is enabled

        this.modId = modId;
        this.validateAllEntries = validateAllEntries;
        this.dupeStrat = dupeStrat;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        return this.registries.thenCompose((regProvider) -> {
            DynamicOps<JsonElement> regEncoderOps = RegistryOps.create(JsonOps.INSTANCE, regProvider);

            return CompletableFuture.allOf(NexusServices.REGISTRAR.getDynamicRegistries().stream()
                    .flatMap((regData) -> dumpRegistryCap(cachedOutput, regProvider, regEncoderOps, regData).stream())
                    .toArray(CompletableFuture[]::new));
        });
    }

    @Override
    protected <T> @NotNull Optional<CompletableFuture<?>> dumpRegistryCap(CachedOutput cachedOutput, HolderLookup.Provider regProvider, DynamicOps<JsonElement> encoderOps, RegistryDataLoader.RegistryData<T> regData) {
        ResourceKey<? extends Registry<T>> curElement = regData.key();
        Object2ObjectOpenHashMap<ResourceKey<T>, T> serializedElements = new Object2ObjectOpenHashMap<>();

        return regProvider.lookup(curElement).map((regLookup) -> {
            PackOutput.PathProvider datapackOutputPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, ResourceLocationUtil.formatPath(curElement.location()));

            return CompletableFuture.allOf(regLookup.listElements()
                    .filter(holderRef -> holderRef.key().location().getNamespace().equals(modId))
                    .filter(holderRef -> {
                        if (validateAllEntries() && holderRef.value() == null) throw new NullPointerException(String.format("Missing registry entry for: %s (from mod of ID: %s)", holderRef.key().location(), getModId()));

                        if (serializedElements.get(holderRef.key()) != null) { // Registries themselves already handle duplicates anyway, but you never know
                            switch (getDuplicateDataPolicy()) {
                                case CRASH -> throw new IllegalStateException(String.format("Duplicate registry entry for: %s (from mod of ID: %s), specified DuplicateDataPolicy is CRASH.", holderRef.key(), getModId()));
                                case EXCLUDE_WARN -> {
                                    NexusConstants.LOGGER.warn("Duplicate registry entry for: {} (from mod of ID: {}), specified DuplicateDataPolicy is EXCLUDE_WARN. Skipping...", holderRef.key(), getModId());
                                    return false;
                                }
                                case EXCLUDE_SILENT -> {
                                    return false;
                                }
                                case OVERRIDE_WARN -> {
                                    NexusConstants.LOGGER.warn("Overriding duplicate registry entry for: {} (from mod of ID: {}), specified DuplicateDataPolicy is OVERRIDE_WARN.", holderRef.key(), getModId());
                                    return true;
                                }
                                case OVERRIDE_SILENT -> {
                                    return true;
                                }
                            }
                        } else serializedElements.put(holderRef.key(), holderRef.value());

                        return true;
                    })
                    .map((holderRef) -> {
                        NexusConstants.LOGGER.debug("Serializing registry entry: {} (from mod of ID: {})", holderRef.key(), getModId());
                        return dumpValue(datapackOutputPathProvider.json(holderRef.key().location()), cachedOutput, encoderOps, regData.elementCodec(), holderRef.value());
                    })
                    .toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public @NotNull String getName() {
        return String.format("Dynamic Registries [%s]", getModId());
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
        return NexusProviderTypes.DYNAMIC_REGISTRY_PROVIDER;
    }

    @Override
    public @NotNull DuplicateDataPolicy getDuplicateDataPolicy() {
        return dupeStrat;
    }

    protected static HolderLookup.Provider constructDummyRegistries(HolderLookup.Provider original, RegistrySetBuilder datapackEntriesBuilder)  { // Modified Forge impl for proper dynamic registry handling (+ not worth reinventing the wheel)
        HashSet<ResourceKey<? extends Registry<?>>> builderKeys = new HashSet<>(datapackEntriesBuilder.entries.stream().map(RegistrySetBuilder.RegistryStub::key).toList());

        NexusServices.REGISTRAR.getDynamicRegistries().stream() // While the underlying names are misleading, these are indeed all the existing datapack registries
                .filter(data -> !builderKeys.contains(data.key()))
                .forEach(data -> {
                    NexusConstants.LOGGER.debug("Adding dummy registry entry for empty or unmapped dynamic registry: {}", data.key().location());
                    datapackEntriesBuilder.add(data.key(), context -> {});
                }); // Add dummy mappings for unmapped registries, just in case they're referenced elsewhere to prevent annoying NPEs among other things

        return datapackEntriesBuilder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), original); // This is only run once during datagen since we only populate the RSB Nexus API uses for all of its dependants
    }
}
