package com.mememan.nexus.internal.loader;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.asm.ClassFinder;
import com.mememan.nexus.loader.ModData;
import com.mememan.nexus.loader.ModMetadata;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fabric-specific implementation of {@link ModData}.
 * <br></br>
 * Pre-emptively caches mod data on startup, using {@link ModContainer} as the backing source for mod meta/data. Additionally,
 * handles mapping Fabric-specific metadata and logs general operations executed on object construction.
 */
public class FabricModData implements ModData {
    private final ModContainer ownerModContainer;
    private final ModMetadata ownerModMetadata;
    private final ObjectArrayList<String> allFilePaths;
    private final ConcurrentHashMap<String, ObjectArrayList<String>> cachedAnnotatedClasses = new ConcurrentHashMap<>(); // Stored as strings to avoid unnecessary classloading

    public FabricModData(ModContainer ownerModContainer) {
        long startTime = System.currentTimeMillis();

        this.ownerModContainer = ownerModContainer;

        this.ownerModMetadata = null;
        this.allFilePaths = ObjectArrayList.of();

        long endTime = System.currentTimeMillis();
        long milliDuration = endTime - startTime;

        NexusConstants.LOGGER.info("Loaded mod data for mod {} (within file {}) in {} ms", ownerModContainer.getMetadata().getId(), ownerModContainer.getOrigin(), milliDuration);
    }

    public ModContainer getOwnerModContainer() {
        return ownerModContainer;
    }

    @Override
    public @NotNull ModMetadata getModMetadata() {
        return ownerModMetadata;
    }

    @Override
    public List<String> getAllFilePaths() {
        return allFilePaths;
    }

    @Override
    public ConcurrentHashMap<String, ObjectArrayList<String>> getCachedAnnotatedClasses() {
        return cachedAnnotatedClasses;
    }

    @Override
    public List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz, @Nullable Comparator<String> classLoadingSorter) {
        return cachedAnnotatedClasses.get(annotationTypeClazz.getName()) == null ? ObjectArrayList.of() : cachedAnnotatedClasses.get(annotationTypeClazz.getName())
                .stream()
                .sorted(classLoadingSorter != null ? classLoadingSorter : String::compareTo)
                .map(ClassFinder::forName)
                .collect(Collectors.toCollection(ObjectArrayList::new));
    }
}
