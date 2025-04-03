package com.mememan.nexus.internal.services;

import com.mememan.nexus.loader.GamePathWrapper;
import com.mememan.nexus.loader.ModData;
import com.mememan.nexus.loader.ModLoader;
import com.mememan.nexus.platform.services.PlatformManager;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FabricPlatformManager implements PlatformManager {

    @Override
    public ModLoader getPlatform() {
        return ModLoader.FABRIC;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return false;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return false;
    }

    @Override
    public List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz, @Nullable Comparator<String> classLoadingSorter, @Nullable List<String> validModIds) {
        return List.of();
    }

    @Override
    public Set<ModData> getModData() {
        return Set.of();
    }

    @Override
    public GamePathWrapper getGamePathWrapper() {
        return null;
    }
}
