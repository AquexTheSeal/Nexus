package com.mememan.nexus.platform.services;

import com.mememan.nexus.loader.EnvironmentType;
import com.mememan.nexus.loader.GamePathWrapper;
import com.mememan.nexus.loader.ModData;
import com.mememan.nexus.loader.ModLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * A loader-agnostic interface for managing platform-specific implementations of certain loader-specific features,
 * ranging from methods that provide predicates for loader detection to methods that allow for more performant ways of
 * executing certain tasks, such as class-loading, environment-detection, etc.
 * <br></br>
 * Methods provided here are a mix of template-default utilities included in the MultiLoader template, as well as more
 * sophisticated helpers aimed at more specific tasks.
 */
public interface PlatformManager {

    /**
     * Gets the {@link ModLoader} representation of the current platform. This is an OOP'd variant of the otherwise
     * template-default method included in the MultiLoader template.
     *
     * @return The {@link ModLoader} representation of the current platform.
     */
    ModLoader getPlatform();

    /**
     * Checks if a mod with the given id is loaded. This is a template-default method included in the MultiLoader
     * template.
     *
     * @param modId The mod to check if it is loaded.
     *
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment. This is a template-default method included in the
     * MultiLoader template.
     *
     * @return {@code true} if in a development environment, {@code false} otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Discovers all (mod) classes that are annotated with the specified annotation type and compiles them into a
     * {@link List}. Take note that this method <b>loads</b> (valid) discovered classes.
     *
     * @param annotationTypeClazz The annotation type class.
     * @param validModIds An optional whitelist of valid mod IDs to scan for annotated classes. Leaving this empty or
     *                    {@code null} will result in a scan for annotated classes from all mods.
     *
     * @return A {@link List} of (loaded) classes annotated with the specified annotation type. May be empty.
     */
    List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz, @Nullable List<String> validModIds);

    /**
     * Overloaded variant of {@link #discoverAnnotatedClasses(Class, List)}. Will scan for annotated classes from all
     * mods.
     *
     * @param annotationTypeClazz The annotation type class.
     *
     * @return A {@link List} of (loaded) classes annotated with the specified annotation type. May be empty.
     */
    default List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz) {
        return discoverAnnotatedClasses(annotationTypeClazz, null);
    }

    /**
     * Gets all loaded mods and converts them into their respective {@link ModData} representation before pooling them
     * into a {@link Set}. Different loaders have different implementations of {@link ModData}.
     *
     * @return A {@link Set} of all loaded mods, represented as {@link ModData} objects.
     */
    Set<ModData> getModData();

    /**
     * Gets the {@link GamePathWrapper} representing path-related operations for a given loader.
     *
     * @return The {@link GamePathWrapper} of the current platform.
     */
    GamePathWrapper getGamePathWrapper();

    /**
     * Gets the {@link EnvironmentType} representation of the current environment. This is an OOP'd variant of the
     * otherwise template-default method included in the MultiLoader template.
     *
     * @return The {@link EnvironmentType} representation of the current environment.
     */
    default EnvironmentType getEnvironmentType() {
        return isDevelopmentEnvironment() ? EnvironmentType.DEVELOPMENT : EnvironmentType.PRODUCTION;
    }
}
