package com.mememan.nexus.platform.services;

import com.mememan.nexus.Nexus;
import com.mememan.nexus.asm.annotations.RegistrarEntry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A loader-agnostic {@code interface} used for dynamically delegating object registration without needing multiple
 * separate methods, classes, or redundant loader-specific setup.
 * <br></br>
 * Supports standard, datapack, and special vanilla registry types. Additionally covers custom registry types extending
 * from any of the 3 aforementioned types.
 * <br></br>
 * Dependant mods are responsible for storing their registered objects within their own collections. The average
 * registrar {@code class} should look something like this:
 * <pre>
 *     {@code
 *          @RegistrarEntry // Optional, you can use bootstrap methods or some other way to statically initialize this class
 *          public class MyModBlocks {
 *              private static final ObjectArrayList<Supplier<Block>> BLOCKS = new ObjectArrayList<>(); // Collection type can vary based on your use-case, but this is generally how you'd do it for a standard registry
 *              private static final ObjectArrayList<Supplier<Item>> BLOCK_ITEMS = new ObjectArrayList<>(); // If your blocks are going to have their own items, you should also store those separately
 *
 *              public static final Supplier<Block> EXAMPLE_BLOCK = BlockPropertyWrapper.of(BlockPropertyWrappers.BASIC_BLOCK, registerBlock())
 *                  .cachedBuilder()
 *                  .withParentCreativeModeTab(YourCMTRegistrarClass.YOUR_BLOCK_TAB)
 *                  .build()
 *                  .getParentBlock();
 *
 *              // All methods below are optional; you can register your objects however you want so long as you're ordering everything correctly (Not attempting to access objects before they're registered via NexusServices.REGISTRAR.registerObject(...), etc.)
 *              // Nexus API offers shortcut utility methods that allow for flexibility based on your needs inside of the com.mememan.nexus.template subpackages
 *
 *              private static Supplier<Block> registerBlock(String id, Supplier<Block> blockSup, Item.Properties blockItemProperties) {
 *                  Supplier<Block> registeredBlock = registerItemlessBlock(id, blockSup);
 *                  registerBlockItem(id, () -> new BlockItem(registeredBlock.get(), blockItemProperties));
 *                  return registeredBlock;
 *              }
 *
 *              private static Supplier<Block> registerBlock(String id, Supplier<Block> blockSup, Supplier<Item> itemSup) {
 *                  Supplier<Block> registeredBlock = registerItemlessBlock(id, blockSup);
 *                  registerBlockItem(id, itemSup);
 *                  return registeredBlock;
 *              }
 *
 *              private static Supplier<Block> registerItemlessBlock(String id, Supplier<Block> blockSup) {
 *                  Supplier<Block> registeredBlockSup = NexusServices.REGISTRAR.registerObject(NexusConstants.prefix(id), blockSup, BuiltInRegistries.BLOCK); // Otherwise reference to the block sup is null cuz it needs to be registered beforehand
 *                  BLOCKS.add(registeredBlockSup);
 *                  return registeredBlockSup;
 *              }
 *
 *              private static Supplier<Item> registerBlockItem(String id, Supplier<Item> itemSup) {
 *                  Supplier<Item> registeredItemSup = NexusServices.REGISTRAR.registerObject(NexusConstants.prefix(id), itemSup, BuiltInRegistries.ITEM); // Otherwise reference to the item sup is null cuz it needs to be registered beforehand
 *                  BLOCK_ITEMS.add(registeredItemSup);
 *                  return registeredItemSup;
 *              }
 *
 *              // You would typically want others to have read-only access to your registered objects
 *              // Note that others modifying your custom collections won't actually affect objects you've registered to the game (I.E. If they, for instance, try BLOCKS.remove(EXAMPLE_BLOCK), it won't actually remove the block from the game)
 *
 *              public static ImmutableList<Supplier<Block>> getBlocks() {
 *                  return BLOCKS;
 *              }
 *
 *              public static ImmutableList<Supplier<Item>> getBlockItems() {
 *                  return BLOCK_ITEMS;
 *              }
 *          }
 *     }
 * </pre>
 *
 * For more information, see the references below.
 */
public interface Registrar {

    /**
     * Main method for this service interface, called in {@link Nexus} in order to load it and its loader-specific
     * implementations accordingly.
     * <br></br>
     * Functionally speaking, all this method does is properly load and cache registry information on startup. It also
     * handles loading all classes annotated with {@link RegistrarEntry}.
     * <br></br>
     * Dependant mods may choose to opt out of this auto-loading feature either by simply not annotating their classes
     * with {@link RegistrarEntry}. It should, however, be noted that mods not using this annotation will have to
     * statically-initialize their classes in some way (bootstrap methods, custom annotation discovery, etc.) in order
     * for object registration to actually occur.
     * <br></br>
     * Should <b>NOT</b> be called anywhere else!
     */
    @ApiStatus.Internal
    void setupRegistrar();

    /**
     * Attempts to register an object to the specified {@linkplain Registry targetRegistry}.
     * <br></br>
     * Generally, any registries available in the {@link BuiltInRegistries} class can be used for this method. This
     * could include custom registry types.
     *
     * @param objId The id of the object to register, following Minecraft's regex naming conventions/constraints
     *              (<code>[a-z0-9_.-]</code>). Duplicate exceptions and other edge-cases are handled accordingly
     *              within the target mod-loader's registry implementation.
     * @param objSup The object to register. Has to be valid (e.g. non-{@code null}, matching the target registry's
     *               type, etc.) for the target registry.
     * @param targetRegistry The target {@link Registry} to register the specified object to.
     *
     * @return The <code>objSup</code> that was registered.
     *
     * @param <V> The parent object type of {@code <T>} (So if {@code targetRegistry} is {@link BuiltInRegistries#ITEM},
     *           {@code <V>} would be of type {@link Item}, which makes {@code <T>} any object type extending
     *           {@link Item}).
     * @param <T> The object type to register (e.g. ({@code extends}) {@link Item} or {@link Attribute}).
     *
     * @see BuiltInRegistries
     */
    <V, T extends V> Supplier<T> registerObject(final ResourceLocation objId, final Supplier<T> objSup, Registry<V> targetRegistry);

    /**
     * Attempts to register a datapack object to the specified {@linkplain ResourceKey<Registry<T>> targetRegistry}.
     * <br></br>
     * Generally, any datapack registries available in the {@link Registries} class can be used for this method. This
     * could include custom datapack registries. Datapack registries are {@linkplain Registry Registries} that store any
     * form of CODECs for de/serializing data from/to JSON files pertaining to their respective object types.
     *
     * @param objId The id of the object to register, following Minecraft's regex naming conventions/constraints
     *              (<code>[a-z0-9_.-]</code>). Duplicate exceptions and other edge-cases are handled accordingly
     *              within the target mod-loader's registry implementation.
     *              <br></br>
     *              Mind that the {@link ResourceLocation} reference passed in must point to an existing and valid JSON
     *              file within the datapack registry's target directory (except in the case of datagen, in which case
     *              this is used to generate the JSON file itself).
     * @param objSupMappingFunc The actual object pertaining to the registered {@link ResourceKey<Registry<T>>}.
     *                          Is a {@link Function} that takes a {@link BootstapContext} instance as input and returns
     *                          the object to register.
     * @param targetDatapackRegistry The target datapack registry to register the specified object to.
     *
     * @return The {@link ResourceKey} of the object that was registered.
     *
     * @param <T> The object type to register, doubles as the registry's generic type.
     *
     * @apiNote The output of {@code objSupMappingFunc} isn't returned since registration fields should store references
     * to the registered {@link ResourceKey<Registry<T>>} for later access utilising {@link RegistryAccess} (commonly
     * found in {@link Level} instances), as per MC's datapack value-storing conventions.
     *
     * @see Registries
     * @see ServerLevel#registryAccess()
     */
    <T> Supplier<ResourceKey<T>> registerDatapackObject(final ResourceLocation objId, Function<BootstapContext<T>, Supplier<T>> objSupMappingFunc, final ResourceKey<Registry<T>> targetDatapackRegistry);
}
