package com.mememan.nexus.platform;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.platform.services.NetworkManager;
import com.mememan.nexus.platform.services.PlatformManager;

import java.util.ServiceLoader;

/**
 * Centralized service loader for Nexus API. This is the primary access points for all platform-agnostic services.
 * <br></br>
 * Dependant mods should ensure that, when using these services, they do things under their own namespaces/Mod IDs
 * where appropriate (e.g. object registration).
 * <br></br>
 * Nexus API internally handles service-action segregation where applicable for these services, so end-developers
 * shouldn't worry about namespace collisions.
 */
public class NexusServices {
    /**
     * The central service for managing platform-specific tasks handled on a per-loader basis.
     */
    public static final PlatformManager PLATFORM_MANAGER = loadService(PlatformManager.class);
    /**
     * The service responsible for handling all types of object registration (save for packet registration, which is
     * handled by {@link #NETWORK_MANAGER}).
     */
 //   public static final Registrar REGISTRAR = loadService(Registrar.class);
    /**
     * The service responsible for handling packet registration and interaction across sides (Client/Server).
     */
    public static final NetworkManager NETWORK_MANAGER = loadService(NetworkManager.class);

    /**
     * Internal service loader method for loading platform-agnostic services.
     *
     * @param clazz The platform {@code class} (usually an {@code interface}) to load.
     * @return The loaded service instance.
     *
     * @param <T> The object type of the service to load.
     */
    private static <T> T loadService(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        NexusConstants.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);

        return loadedService;
    }
}