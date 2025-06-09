package com.mememan.nexus.internal.services;

import com.google.common.collect.HashMultimap;
import com.mememan.nexus.asm.annotations.DatagenRegistrarEntry;
import com.mememan.nexus.datagen.ModDatagenConfig;
import com.mememan.nexus.datagen.ModSpecificPackOutput;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import com.mememan.nexus.datagen.standard.StandardRecipeProvider;
import com.mememan.nexus.loader.ModData;
import com.mememan.nexus.loader.ModSide;
import com.mememan.nexus.platform.NexusServices;
import com.mememan.nexus.platform.services.DataGenerator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;

public class ForgeDataGenerator implements DataGenerator {
    private static boolean HAS_CONSUMED_GENERATORS = false;
    @Nullable
    private static net.minecraft.data.DataGenerator CURRENT_GLOBAL_DATA_GENERATOR_INSTANCE;
    private static final Queue<ObjectObjectImmutablePair<String, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, ? extends Pair<Boolean, ? extends DataProvider>>>> ENQUEUED_PROVIDERS = new ConcurrentLinkedQueue<>();
    private static final Queue<ObjectObjectImmutablePair<String, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, ? extends ModDataProvider>>> ENQUEUED_MOD_PROVIDERS = new ConcurrentLinkedQueue<>();
    private static final HashMultimap<String, ProviderType> EXCLUDED_PROVIDER_TYPES_BY_ID = HashMultimap.create();
    private static final ObjectOpenHashSet<String> EXCLUDED_MODS_BY_ID = new ObjectOpenHashSet<>();
    private static final ObjectOpenHashSet<ModDatagenConfig> MOD_DATAGEN_CONFIGS = new ObjectOpenHashSet<>();

    @Override
    public void setupDataGenerator() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(ForgeDataGenerator::onGatherDataEvent);
    }

    @Override
    public <DP extends DataProvider> void registerDataProvider(String modId, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, Pair<Boolean, DP>> dataProvider) {
        ENQUEUED_PROVIDERS.add(ObjectObjectImmutablePair.of(modId, dataProvider));
    }

    @Override
    public <MDP extends ModDataProvider> void registerModDataProvider(String modId, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, MDP> modDataProvider) {
        ENQUEUED_MOD_PROVIDERS.add(ObjectObjectImmutablePair.of(modId, modDataProvider));
    }

    @Override
    public ModDatagenConfig registerConfigForMod(ModDatagenConfig modDatagenConfig) {
        MOD_DATAGEN_CONFIGS.add(modDatagenConfig);
        return modDatagenConfig;
    }

    @Override
    public Set<ModDatagenConfig> getModDatagenConfigs() {
        return MOD_DATAGEN_CONFIGS;
    }

    @Override
    public @Nullable net.minecraft.data.DataGenerator getDataGenerator() {
        return CURRENT_GLOBAL_DATA_GENERATOR_INSTANCE;
    }

    public static boolean hasConsumedGenerators() {
        return HAS_CONSUMED_GENERATORS;
    }

    public static void onGatherDataEvent(final GatherDataEvent event) {
        net.minecraft.data.DataGenerator primaryGen = event.getGenerator();
        PackOutput rootPackOutput = primaryGen.getPackOutput();
        Path formattedOutputPath = rootPackOutput.getOutputFolder();
        CompletableFuture<HolderLookup.Provider> regLookupProvider = event.getLookupProvider();

        boolean onClient = event.includeClient();
        boolean onServer = event.includeServer();

        if (!hasConsumedGenerators()) { // Safeguard against potentially running this more than once for any reason, even though GatherDataEvent is only ever initialized once in DatagenModLoader#begin
            CURRENT_GLOBAL_DATA_GENERATOR_INSTANCE = primaryGen;

            NexusServices.PLATFORM_MANAGER.discoverAnnotatedClasses(DatagenRegistrarEntry.class);

            // ModDataProvider types
            if (!ENQUEUED_MOD_PROVIDERS.isEmpty()) {
                ObjectObjectImmutablePair<String, BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, ? extends ModDataProvider>> modProviderMapper;

                while ((modProviderMapper = ENQUEUED_MOD_PROVIDERS.poll()) != null) {
                    ModData targetMod = NexusServices.PLATFORM_MANAGER.getModDataById(modProviderMapper.left());
                    String targetModId = targetMod.getModMetadata().modId();
                    ModDatagenConfig targetModConfig = NexusServices.DATA_GENERATOR.getConfigForMod(targetModId);
                    boolean allowDatagenForMod = targetModConfig == null || targetModConfig.enableDatagen();
                    ModSpecificPackOutput modSpecificPackOutput = new ModSpecificPackOutput(formattedOutputPath, targetMod, allowDatagenForMod);
                    ModDataProvider mappedModProvider = modProviderMapper.right().apply(modSpecificPackOutput, regLookupProvider);
                    ProviderType mappedModProviderType = mappedModProvider.getProviderType();
                    boolean modOnClient = mappedModProviderType.getSide() == ModSide.CLIENT;
                    boolean allowDatagenForProviderType = targetModConfig == null || !targetModConfig.disabledProviderTypes().contains(mappedModProviderType);

                    if (allowDatagenForProviderType) primaryGen.addProvider(modOnClient ? onClient : onServer, mappedModProvider);
                }
            }

            // Standard DataProvider types
            if (!ENQUEUED_PROVIDERS.isEmpty()) {

            }

            // Native Nexus datagen
            NexusServices.PLATFORM_MANAGER.getModData().forEach(curModData -> {
                String modId = curModData.getModMetadata().modId();
                ModDatagenConfig modDatagenConfig = NexusServices.DATA_GENERATOR.getConfigForMod(modId);
                boolean allowDatagenForMod = modDatagenConfig == null || modDatagenConfig.enableDatagen();
                ModSpecificPackOutput modSpecificPackOutput = new ModSpecificPackOutput(formattedOutputPath, curModData, allowDatagenForMod);
                Set<ProviderType> providersToValidate = modDatagenConfig == null ? Set.of() : modDatagenConfig.providerTypesToFullyValidate();
                Set<ProviderType> disabledProviders = modDatagenConfig == null ? Set.of() : modDatagenConfig.disabledProviderTypes();

                // Client

                // Server
                primaryGen.addProvider(allowDatagenForMod && !disabledProviders.contains(NexusProviderTypes.RECIPE_PROVIDER) && onServer, new StandardRecipeProvider(modSpecificPackOutput, modId, providersToValidate.contains(NexusProviderTypes.RECIPE_PROVIDER)));
            });

            HAS_CONSUMED_GENERATORS = true;
        }
    }
}
