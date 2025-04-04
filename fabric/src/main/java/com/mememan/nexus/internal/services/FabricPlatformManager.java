package com.mememan.nexus.internal.services;

import com.mememan.nexus.internal.loader.FabricGamePathWrapper;
import com.mememan.nexus.internal.loader.FabricModData;
import com.mememan.nexus.loader.GamePathWrapper;
import com.mememan.nexus.loader.ModData;
import com.mememan.nexus.loader.ModLoader;
import com.mememan.nexus.platform.services.PlatformManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fabric-specific implementation of {@link PlatformManager}.
 */
public class FabricPlatformManager implements PlatformManager {
    private static final FabricGamePathWrapper FABRIC_GAME_PATH_WRAPPER = new FabricGamePathWrapper();
    private static final ObjectOpenHashSet<ModData> MOD_DATA_CACHE = FabricLoader.getInstance().getAllMods().stream()
            .map(FabricModData::new)
            .collect(Collectors.toCollection(ObjectOpenHashSet::new));

    @Override
    public ModLoader getPlatform() {
        return ModLoader.FABRIC;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz, @Nullable Comparator<String> classLoadingSorter, @Nullable List<String> validModIds) {
        return MOD_DATA_CACHE.stream()
                .filter(currentModData -> validModIds == null || validModIds.isEmpty() || validModIds.contains(currentModData.getModMetadata().modId()))
                .flatMap(currentModData -> currentModData.discoverAnnotatedClasses(annotationTypeClazz, classLoadingSorter).stream())
                .collect(Collectors.toCollection(ObjectArrayList::new));
    }

    @Override
    public Set<ModData> getModData() {
        return MOD_DATA_CACHE;
    }

    @Override
    public GamePathWrapper getGamePathWrapper() {
        return FABRIC_GAME_PATH_WRAPPER;
    }
}
