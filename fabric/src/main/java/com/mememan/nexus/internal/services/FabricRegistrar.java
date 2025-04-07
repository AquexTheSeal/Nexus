package com.mememan.nexus.internal.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.asm.ClassFinder;
import com.mememan.nexus.asm.annotations.RegistrarEntry;
import com.mememan.nexus.platform.NexusServices;
import com.mememan.nexus.platform.services.Registrar;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric-specific implementation of {@link Registrar}.
 */
public class FabricRegistrar implements Registrar {
    private static final Multimap<ResourceKey<? extends Registry<?>>, ObjectObjectMutablePair<ResourceKey<?>, Function<? extends BootstapContext<?>, ? extends Supplier<?>>>> CACHED_DATAPACK_OBJECT_ENTRIES = ArrayListMultimap.create(); // Slower put() than HashMultiMap, but we need to allow duplicates for leniency
    private static RegistrySetBuilder DATAPACK_REGISTRY_SET_BUILDER;

    @Override
    public void setupRegistrar() {
        long startTime = System.currentTimeMillis();

        NexusServices.PLATFORM_MANAGER.discoverAnnotatedClasses(RegistrarEntry.class, (classA, classB) -> {
            Class<?> uninitializedClassA = ClassFinder.forNameNoInit(classA);
            Class<?> uninitializedClassB = ClassFinder.forNameNoInit(classB);
            RegistrarEntry annotA = uninitializedClassA.getAnnotation(RegistrarEntry.class); // We don't care about initializing the annotation itself since it doesn't do anything
            RegistrarEntry annotB = uninitializedClassB.getAnnotation(RegistrarEntry.class);
            int priorityA = annotA.priority();
            int priorityB = annotB.priority();

            return priorityA > priorityB
                    ? -1
                    : priorityA == priorityB
                    ? classA.compareTo(classB)
                    : 1;
        }, (sortedClassName) -> {
            Class<?> uninitializedTargetClass = ClassFinder.forNameNoInit(sortedClassName);
            RegistrarEntry targetAnnotation = uninitializedTargetClass.getAnnotation(RegistrarEntry.class);
            Class<?>[] dependencies = targetAnnotation.dependencies();

            if (dependencies != null) {
                for (Class<?> dependency : dependencies) {
                    if (dependency == null || dependency.getName().equals(sortedClassName)) continue;

                    ClassFinder.forName(dependency.getName());
                }
            }
        });

        CACHED_DATAPACK_OBJECT_ENTRIES.asMap().forEach((registryKey, objSupMappingFuncs) -> {
            getDatapackRegistrySetBuilder().add(tCastRegistryKey(registryKey), b -> objSupMappingFuncs.forEach(mappedObjKey -> {
                b.register(tCastObjectKey(mappedObjKey.left()), tCastObjSupMappingFunc(mappedObjKey.right(), b).get());
            }));
        });

        long endTime = System.currentTimeMillis();
        NexusConstants.LOGGER.info("Registrar setup took {} ms", endTime - startTime);
    }

    @Override
    public <V, T extends V> Supplier<T> registerObject(ResourceLocation objId, Supplier<T> objSup, Registry<V> targetRegistry) {
        T targetObject = Registry.register(targetRegistry, objId, objSup.get()); // Must store in a local field beforehand cuz... for some reason it's null if inlined
        return () -> targetObject;
    }

    @Override
    public <T> Supplier<ResourceKey<T>> registerDatapackObject(ResourceLocation objId, Function<BootstapContext<T>, Supplier<T>> objSupMappingFunc, ResourceKey<Registry<T>> targetDatapackRegistry) {
        ResourceKey<T> targetObject = ResourceKey.create(targetDatapackRegistry, objId);

        if (objSupMappingFunc != null) {
            CACHED_DATAPACK_OBJECT_ENTRIES.put(targetDatapackRegistry, ObjectObjectMutablePair.of(targetObject, objSupMappingFunc)); // Need to use this approach since RSBs don't support stacking registration calls
        }

        return () -> targetObject;
    }

    protected <T> Supplier<T> tCastObjSupMappingFunc(Function<? extends BootstapContext<?>, ? extends Supplier<?>> objSupMappingFunc, BootstapContext<T> bootstapContext) { // I love wildcard casts
        return ((Function<BootstapContext<T>, Supplier<T>>) objSupMappingFunc).apply(bootstapContext);
    }

    protected <T> ResourceKey<T> tCastObjectKey(ResourceKey<?> objectKey) {
        return (ResourceKey<T>) objectKey;
    }

    protected <T> ResourceKey<Registry<T>> tCastRegistryKey(ResourceKey<? extends Registry<?>> registryKey) {
        return (ResourceKey<Registry<T>>) registryKey;
    }

    public static RegistrySetBuilder getDatapackRegistrySetBuilder() {
        return DATAPACK_REGISTRY_SET_BUILDER == null ? DATAPACK_REGISTRY_SET_BUILDER = new RegistrySetBuilder() : DATAPACK_REGISTRY_SET_BUILDER;
    }
}
