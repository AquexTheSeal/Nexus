# Nexus API: A Comprehensive Overview

## Introduction

Nexus API is a sophisticated platform-agnostic library designed to streamline Minecraft mod development across multiple mod loaders. Created to address the challenges of cross-platform compatibility, Nexus API provides a unified interface that abstracts away the implementation differences between mod loaders like Fabric and Forge. This allows developers to write code once and deploy it across multiple platforms with minimal platform-specific adjustments.

The primary goal of Nexus API is to reduce boilerplate code and eliminate compatibility issues that typically arise when developing for multiple mod loaders. By providing a consistent set of services and utilities, Nexus API enables developers to focus on creating mod content rather than dealing with the intricacies of different loader implementations.

## Project Architecture and Design

### Core Design Philosophy

Nexus API is built around a service-oriented architecture that leverages Java's ServiceLoader mechanism to dynamically load platform-specific implementations of core services. This design allows the API to maintain a clean separation between the interface that mod developers interact with and the platform-specific implementations that handle the actual work.

The architecture consists of three main components:

1. **Common Module**: Contains platform-agnostic interfaces, abstract classes, and utility methods that define the API's functionality.
2. **Platform-Specific Modules**: Separate modules for Fabric and Forge that implement the interfaces defined in the common module.
3. **Service Discovery System**: Uses Java's ServiceLoader to dynamically load the appropriate implementation based on the current mod loader.

### Service-Oriented Architecture

At the heart of Nexus API is the `NexusServices` class, which serves as the central access point for all platform-agnostic services. This class uses the ServiceLoader pattern to load the appropriate implementation of each service based on the current mod loader. The main services provided include:

1. **PLATFORM_MANAGER**: Manages platform-specific tasks and provides methods for detecting the environment, discovering annotated classes, and accessing mod metadata.
2. **REGISTRAR**: Handles object registration for various Minecraft registries, supporting both standard and datapack registries.
3. **NETWORK_MANAGER**: Manages packet registration and client/server communication.

Each service is defined by an interface in the common module and implemented separately in the Fabric and Forge modules. This approach allows Nexus API to provide a consistent interface while using platform-specific implementations behind the scenes.

### Annotation-Based Discovery System

Nexus API includes a sophisticated annotation-based discovery system that allows for automatic loading and initialization of annotated classes. This system is particularly useful for registering mod components like blocks, items, and entities without requiring explicit registration calls.

The `@RegistrarEntry` annotation, for example, marks classes that should be automatically discovered and loaded during the registration phase. The annotation supports priority values and dependencies, allowing for fine-grained control over the initialization order.

## Key Features and Functionality

### Platform-Agnostic Registration

One of the core features of Nexus API is its platform-agnostic registration system. The `Registrar` service provides methods for registering objects to both standard registries (via `BuiltInRegistries`) and datapack registries (via `Registries`). This allows mod developers to register their content using a consistent API regardless of the target platform.

The registration system supports various Minecraft registry types, including blocks, items, entities, and more. It also handles the complexities of datapack registries, which require special handling for JSON serialization and deserialization.

### Networking Abstraction

Nexus API provides a platform-agnostic networking system through the `NetworkManager` service. This service handles packet registration and client/server communication, abstracting away the differences between Fabric's and Forge's networking implementations.

The networking system supports automatic discovery of network packets through annotations, simplifying the process of setting up mod communication channels.

### Environment Detection and Utilities

The `PlatformManager` service offers various utilities for detecting the current environment, including methods to:
- Determine the current mod loader (Fabric, Forge)
- Check if specific mods are loaded
- Detect if the environment is development or production
- Access game paths and server instances
- Determine the current environment side (client, server)

These utilities help mod developers write code that adapts to different environments without requiring platform-specific branches.

### Property Wrappers

Nexus API includes property wrapper classes that simplify the creation and manipulation of Minecraft objects like blocks and items. These wrappers provide a fluent API for setting properties and behaviors, reducing the amount of boilerplate code needed to create custom content.

For example, the `BlockPropertyWrapper` and `ItemPropertyWrapper` classes allow developers to create and configure blocks and items with a clean, chainable API.

### Data Generation Support

The API includes support for platform-agnostic data generation, which is particularly useful for generating resources like models, textures, and recipes. This feature leverages Forge's data generation system but makes it available on both platforms.

## MultiLoader Support

### Cross-Platform Compatibility

Nexus API achieves cross-platform compatibility through several mechanisms:

1. **Service Loader Pattern**: Uses Java's ServiceLoader to dynamically load platform-specific implementations.
2. **Common Interfaces**: Defines platform-agnostic interfaces that are implemented separately for each platform.
3. **Abstraction Layers**: Provides abstraction layers for platform-specific features like networking and registration.
4. **Environment Detection**: Includes utilities for detecting the current platform and environment.

This approach allows mod developers to write code once and deploy it across multiple platforms with minimal changes.

### Platform-Specific Implementations

While Nexus API provides a unified interface, it still needs to handle platform-specific details behind the scenes. This is achieved through separate implementations for each supported platform:

- **Fabric Implementation**: Implements the API's services using Fabric's registry and networking systems.
- **Forge Implementation**: Implements the same services using Forge's equivalent systems.

These implementations are loaded dynamically at runtime based on the current platform, allowing the API to provide a consistent interface while still leveraging platform-specific features.

## Build System and Project Organization

### Build Configuration

Nexus API uses Gradle as its build system, with a multi-module setup that separates common code from platform-specific implementations. The build configuration includes:

- **Root Project**: Contains common build settings and dependencies.
- **Common Module**: Contains platform-agnostic code.
- **Fabric Module**: Contains Fabric-specific implementations.
- **Forge Module**: Contains Forge-specific implementations.

The build system is configured to produce separate artifacts for each platform, allowing mod developers to include only the relevant version in their projects.

### Dependency Management

Nexus API manages dependencies through Gradle, with separate configurations for each platform. The project depends on:

- **Minecraft**: The core Minecraft game.
- **Fabric Loader/API**: For the Fabric implementation.
- **Forge**: For the Forge implementation.
- **Optional Dependencies**: Various optional dependencies like JEI, AppleSkin, and ModMenu.

The build system is configured to handle these dependencies appropriately for each platform, ensuring that the correct versions are used.

### Project Structure

The project follows a standard Java package structure, with packages organized by functionality:

- **com.mememan.nexus**: Root package containing core classes.
- **com.mememan.nexus.platform**: Contains platform-agnostic services and interfaces.
- **com.mememan.nexus.platform.services**: Defines the core service interfaces.
- **com.mememan.nexus.internal.services**: Contains platform-specific implementations of services.
- **com.mememan.nexus.block/item**: Contains utilities for working with blocks and items.
- **com.mememan.nexus.loader**: Contains classes related to mod loader detection and compatibility.
- **com.mememan.nexus.util**: Contains various utility classes.

This structure helps maintain a clean separation between different components of the API, making it easier to navigate and maintain.

## Usage Examples and Integration Points

### Basic Usage

To use Nexus API in a mod, developers typically follow these steps:

1. Add Nexus API as a dependency in their build script.
2. Initialize the API in their mod's entry point.
3. Use the services provided by `NexusServices` to register content and interact with the game.

For example, to register a block using Nexus API:

```java
// Register a block
Supplier<Block> myBlock = NexusServices.REGISTRAR.registerObject(
    new ResourceLocation("mymod", "my_block"),
    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)),
    BuiltInRegistries.BLOCK
);
```

### Advanced Usage with Annotations

For more complex scenarios, developers can use Nexus API's annotation system to automatically discover and register content:

```java
@RegistrarEntry
public class MyModBlocks {
    public static final Supplier<Block> MY_BLOCK = registerBlock(
        "my_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE))
    );

    private static <B extends Block> Supplier<B> registerBlock(String id, Supplier<B> blockSup) {
        return NexusServices.REGISTRAR.registerObject(
            new ResourceLocation("mymod", id),
            blockSup,
            BuiltInRegistries.BLOCK
        );
    }
}
```

### Integration with Other Mods

Nexus API is designed to work alongside other mods and libraries. It provides utilities for detecting if specific mods are loaded and for interacting with their APIs when available.

The API also supports optional dependencies like JEI, AppleSkin, and ModMenu, allowing mods to enhance their functionality when these mods are present without requiring them.

## Performance Considerations

Nexus API is designed with performance in mind, particularly for startup-related tasks. The API uses various optimization techniques to minimize its impact on game performance:

- **Caching**: Extensively caches results to avoid redundant computations.
- **Lazy Loading**: Uses lazy loading for services and resources to defer initialization until needed.
- **Efficient Data Structures**: Uses FastUtil collections for improved performance.
- **Startup Optimization**: Focuses on optimizing startup-related tasks to minimize the impact on game launch time.

According to the project's documentation, the startup impact is negligible in lightweight modpacks and adds less than 20 seconds to heavier modpacks with 390+ mods.

## Conclusion

Nexus API represents a significant advancement in Minecraft mod development, offering a comprehensive solution to the challenges of cross-platform compatibility. By providing a unified interface for common modding tasks, Nexus API enables developers to create mods that work seamlessly across different mod loaders without sacrificing functionality or performance.

The API's service-oriented architecture, combined with its annotation-based discovery system and platform-agnostic utilities, makes it a powerful tool for streamlining mod development. Whether creating simple content mods or complex gameplay overhauls, developers can benefit from Nexus API's ability to abstract away platform-specific details and focus on creating engaging mod content.

As the Minecraft modding ecosystem continues to evolve, tools like Nexus API play a crucial role in promoting compatibility and collaboration between different platforms, ultimately benefiting both mod developers and players.