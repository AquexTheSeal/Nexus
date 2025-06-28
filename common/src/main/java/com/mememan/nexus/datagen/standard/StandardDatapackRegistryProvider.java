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
        super(output, registryLookup.thenApply(provider -> constructRegistries(provider, datapackEntriesBuilder, dupeStrat)));

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

        return regProvider.lookup(curElement).map((regLookup) -> {
            PackOutput.PathProvider datapackOutputPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, ResourceLocationUtil.formatPath(curElement.location()));

            return CompletableFuture.allOf(regLookup.listElements()
                    .filter(holderRef -> holderRef.key().location().getNamespace().equals(modId))
                    .map((holderRef) -> dumpValue(datapackOutputPathProvider.json(holderRef.key().location()), cachedOutput, encoderOps, regData.elementCodec(), holderRef.value()))
                    .toArray(CompletableFuture[]::new));
        });
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
    public boolean validateAllEntries() { // This isn't used here cuz all registries alongside their entries are already validated. Still kept here JIC.
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

    private static HolderLookup.Provider constructRegistries(HolderLookup.Provider original, RegistrySetBuilder datapackEntriesBuilder, DuplicateDataPolicy dupeStrat)  { // Modified Forge impl for proper dynamic registry handling (+ not worth reinventing the wheel)
        HashSet<ResourceKey<? extends Registry<?>>> builderKeys = new HashSet<>(datapackEntriesBuilder.entries.stream().map(RegistrySetBuilder.RegistryStub::key).toList());

        NexusServices.REGISTRAR.getDynamicRegistries().stream() // While the names are misleading, these are indeed all the existing datapack registries
                .filter(data -> {
                    if (builderKeys.contains(data.key())) {
                        switch (dupeStrat) { // Setting this to CRASH is not recommended. Weird ahh dupe registries outta nowhere
                            case CRASH -> throw new IllegalStateException(String.format("Found duplicate registry %s in mod of ID %s, specified DuplicateDataPolicy is CRASH.", data.key(), data.key().location().getNamespace()));
                            case EXCLUDE_WARN -> {
                                NexusConstants.LOGGER.warn("Found duplicate registry {} in mod of ID {}, specified DuplicateDataPolicy is EXCLUDE_WARN. Skipping...", data.key(), data.key().location().getNamespace());
                                return false;
                            }
                            case EXCLUDE_SILENT, OVERRIDE_SILENT -> {
                                return false;
                            }
                            case OVERRIDE_WARN -> {
                                NexusConstants.LOGGER.warn("Found duplicate registry {} in mod of ID {}, specified DuplicateDataPolicy is OVERRIDE_WARN, which is not supported since RSBs do their own strict duplicate validation. Skipping...", data.key(), data.key().location().getNamespace());
                                return false;
                            }
                        }
                    }

                    return true;
                })
                .forEach(data -> datapackEntriesBuilder.add(data.key(), context -> {})); // Add dummy mappings for unmapped registries

        return datapackEntriesBuilder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), original);
    }
}
