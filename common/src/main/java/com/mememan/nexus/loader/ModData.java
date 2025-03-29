package com.mememan.nexus.loader;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Data-holding {@code interface} representing metadata/file data pertaining to a given mod.
 * <br></br>
 * This is primarily useful for performing mod-specific operations segregated from the rest of the environment, thereby
 * minimizing compat oddities and performance overhead, or to check for basic mod metadata.
 * <br></br>
 * It should be noted that loader-specific implementations of this {@code interface} cache all mod meta/data only once
 * during startup. Resource management and other later runtime-dependant tasks should therefore be handled elsewhere.
 */
public interface ModData {

    /**
     * Gets this instance's mod metadata, as commonly defined in each loader's respective MTD files
     * ({@code fabric.mod.json}, {@code mods.toml}, etc.).
     *
     * @return This instance's mod metadata.
     */
    @NotNull
    ModMetadata getModMetadata();

    List<String> getAllFilePaths();

    List<String> getAllClassPaths();

    List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz);
}
