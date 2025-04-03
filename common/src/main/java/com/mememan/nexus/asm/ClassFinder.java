package com.mememan.nexus.asm;

import com.mememan.nexus.NexusConstants;
import org.jetbrains.annotations.ApiStatus;

/**
 * Pseudo-utility class that provides convenient class-loading methods. This should NOT be used directly by dependant
 * mods.
 */
@ApiStatus.Internal
public final class ClassFinder {

    /**
     * A utility method that wraps {@link Class#forName(String)} in a {@code try-catch} block.
     *
     * @param targetClassName The name of the class to load.
     *
     * @return The loaded class, or {@code null} if no such class exists.
     */
    public static Class<?> forName(String targetClassName) {
        try {
            NexusConstants.LOGGER.debug("Loading & Initializing Class: {}", targetClassName);
            return Class.forName(targetClassName);
        } catch (ClassNotFoundException | ExceptionInInitializerError e) {
            NexusConstants.LOGGER.error(e instanceof ClassNotFoundException ? "Failed to load/initialize: {}, no such class was found." : "Failed to initialize: {}", targetClassName, e);
            return null;
        }
    }

    public static Class<?> forNameNoInit(String targetClassName) {
        try {
            NexusConstants.LOGGER.debug("Loading Class (No Initialization): {}", targetClassName);
            return Class.forName(targetClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException | ExceptionInInitializerError e) {
            NexusConstants.LOGGER.error(e instanceof ClassNotFoundException ? "Failed to load: {}, no such class was found." : "Failed to load: {}", targetClassName, e);
            return null;
        }
    }
}
