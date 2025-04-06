package com.mememan.nexus.internal.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.asm.ClassFinder;
import com.mememan.nexus.asm.annotations.RegistrarEntry;
import com.mememan.nexus.platform.NexusServices;
import com.mememan.nexus.platform.services.Registrar;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge-specific implementation of {@link Registrar}.
 */
public class ForgeRegistrar implements Registrar {
    private static final Object2ObjectLinkedOpenHashMap<ResourceKey<?>, DeferredRegister<?>> CACHED_REGISTRIES = new Object2ObjectLinkedOpenHashMap<>();
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
            Class[] dependenciesForA = annotA.dependencies();
            Class[] dependenciesForB = annotB.dependencies();


            if (dependenciesForA != null) {
                for (Class dependency : dependenciesForA) {
                    if (dependency == null || dependency.getName().equals(classA)) continue;

                    ClassFinder.forName(dependency.getName());
                }
            }

            if (dependenciesForB != null) {
                for (Class dependency : dependenciesForB) {
                    if (dependency == null || dependency.getName().equals(classB)) continue;

                    ClassFinder.forName(dependency.getName());
                }
            }

            return priorityA > priorityB
                    ? -1
                    : priorityA == priorityB
                    ? classA.compareTo(classB)
                    : 1;
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
    public <V, T extends V> RegistryObject<T> registerObject(ResourceLocation objId, Supplier<T> objSup, Registry<V> targetRegistry) {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus(); // Should not be null at the time this method is called

        ResourceKey<? extends Registry<V>> targetRegistryKey = targetRegistry.key();

        DeferredRegister<V> existingDefReg = (DeferredRegister<V>) CACHED_REGISTRIES.computeIfAbsent(targetRegistryKey, defReg -> {
            DeferredRegister<V> cachedDefReg = DeferredRegister.create(targetRegistryKey, objId.getNamespace());
            cachedDefReg.register(modBus);
            return cachedDefReg;
        });
        return existingDefReg.register(objId.getPath(), objSup);
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

    public static ImmutableMap<ResourceKey<?>, DeferredRegister<?>> getCachedRegistries() {
        return ImmutableMap.copyOf(CACHED_REGISTRIES);
    }
}
