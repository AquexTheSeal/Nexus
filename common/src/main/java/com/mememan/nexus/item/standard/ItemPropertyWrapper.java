package com.mememan.nexus.item.standard;

import com.google.common.collect.ImmutableMap;
import com.mememan.nexus.client.item.WrappedClampedItemPropertyFunction;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import com.mememan.nexus.item.data.ItemModelDefinition;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A wrapper {@code class} used primarily to store information referenced in datagen to simplify creating data entries
 * for items, as well as the modification of hardcoded item settings.
 */
public class ItemPropertyWrapper {
    private static final Object2ObjectLinkedOpenHashMap<Supplier<Item>, ItemPropertyWrapper> MAPPED_IPWS = new Object2ObjectLinkedOpenHashMap<>();
    @Nullable
    private final ResourceLocation itemRegName;
    private final Supplier<Item> parentItem;
    private final boolean isTemplate;
    @Nullable
    private IPWBuilder builder;

    private ItemPropertyWrapper(@Nullable ResourceLocation itemRegName, Supplier<Item> parentItem) {
        this.itemRegName = itemRegName;
        this.parentItem = parentItem;
        this.isTemplate = false;
    }

    private ItemPropertyWrapper(Supplier<Item> parentItem) {
        this(null, parentItem);
    }

    private ItemPropertyWrapper() {
        this.itemRegName = null;
        this.parentItem = null;
        this.isTemplate = true; // Otherwise can't set with constructor overloading (Laziness:tm:)
    }

    /**
     * Creates a new {@link ItemPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to directly create an IPW during a registration call rather
     * than after it/without storing it.
     *
     * @param itemRegName The registry name of the {@code parentItem}. Used when access to the {@code parentItem} returns
     *                    an air delegate (I.E. It's too early to access the parent item).
     * @param parentItem The parent {@code Supplier<Item>} stored in the newly-initialized IPW instance.
     *
     * @return A new {@link ItemPropertyWrapper} instance.
     *
     * @apiNote You shouldn't have to use this method for 95% of cases since you'd be inlining a registration call for
     * {@link #parentItem}, see {@link #create(Supplier)} instead.
     *
     * @see #create(Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(ItemPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     */
    public static ItemPropertyWrapper create(ResourceLocation itemRegName, Supplier<Item> parentItem) {
        return new ItemPropertyWrapper(itemRegName, parentItem);
    }

    /**
     * Creates a new {@link ItemPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to create an IPW instance with a stored registration call,
     * such that its parent {@code Supplier<Item>} is not an air delegate.
     *
     * @param parentItem The parent {@code Supplier<Item>} stored in the newly-initialized IPW instance.
     *
     * @return A new {@link ItemPropertyWrapper} instance.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(ItemPropertyWrapper, Supplier)
     */
    public static ItemPropertyWrapper create(Supplier<Item> parentItem) {
        return new ItemPropertyWrapper(parentItem);
    }

    /**
     * Creates a new {@link ItemPropertyWrapper} instance as a template. Template IPWs are not stored in
     * {@link #MAPPED_IPWS} and do not store a parent {@link Item}. They're particularly useful for re-using across
     * multiple {@linkplain Item Items}.
     *
     * @return A new {@link ItemPropertyWrapper} instance, set as a template.
     *
     * @see #of(ItemPropertyWrapper, Supplier)
     * @see #isTemplate()
     * @see #ofTemplate(ItemPropertyWrapper)
     */
    public static ItemPropertyWrapper createTemplate() {
        return new ItemPropertyWrapper();
    }

    /**
     * Creates a new {@link ItemPropertyWrapper} instance as a template, inheriting data from the provided IPW template.
     * Template IPWs are not stored in {@link #MAPPED_IPWS} and do not store a parent {@link Item}. They're particularly
     * useful for re-using across multiple {@linkplain Item Items}.
     *
     * @param parentTemplateWrapper The parent {@link ItemPropertyWrapper} template from which {@link #builder()} data
     *                              should be copied.
     *
     * @return A new {@link ItemPropertyWrapper} instance, set as a template, inheriting from the provided IPW template.
     * If the provided IPW template is {@code null}, returns {@link #createTemplate()}.
     *
     * @see #createTemplate()
     * @see #isTemplate()
     */
    public static ItemPropertyWrapper ofTemplate(ItemPropertyWrapper parentTemplateWrapper) {
        if (parentTemplateWrapper != null) {
            ItemPropertyWrapper newTemplateWrapper = new ItemPropertyWrapper();

            return copyProperties(parentTemplateWrapper, newTemplateWrapper);
        } else return createTemplate();
    }

    /**
     * Creates a new {@link ItemPropertyWrapper} instance based on the provided {@link ItemPropertyWrapper}. If the
     * provided IPW instance is {@code null}, returns {@link #create(Supplier)}. You'd typically use this if you have an
     * IPW template you want multiple registered {@linkplain Item Items} to inherit from.
     *
     * @param parentWrapper The parent {@link ItemPropertyWrapper} instance from which {@link #builder()} should be
     *                      copied.
     * @param newItem The new registry entry to use for the newly constructed IPW instance.
     *
     * @return A new {@link ItemPropertyWrapper} instance with copied properties based on the provided IPW, or an
     * entirely new/clean instance if the provided IPW is {@code null}.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #createTemplate()
     */
    public static ItemPropertyWrapper of(ItemPropertyWrapper parentWrapper, Supplier<Item> newItem) {
        if (parentWrapper != null) {
            ItemPropertyWrapper newWrapper = new ItemPropertyWrapper(newItem);

            return copyProperties(parentWrapper, newWrapper);
        } else return create(newItem);
    }

    /**
     * Creates a new {@link ItemPropertyWrapper} instance from an existing {@link ItemPropertyWrapper} instance based
     * on the provided {@code Supplier<Item>}. If no such existing IPW instance exists, returns {@link #create(Supplier)}.
     *
     * @param parentItem The parent {@code Supplier<Item>} stored in {@link #MAPPED_IPWS}. Copies its IPW instance's
     *                   {@link IPWBuilder} properties if it exists, or creates a clean new IPW instance if it doesn't.
     * @param newItem The new registry entry to use for the newly constructed IPW instance.
     *
     * @return A new {@link ItemPropertyWrapper} instance with copied properties based on the provided
     * {@code Supplier<Item>}, or an entirely new/clean instance if no such IPW exists.
     *
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(ItemPropertyWrapper, Supplier)
     */
    public static ItemPropertyWrapper of(Supplier<Item> parentItem, Supplier<Item> newItem) {
        if (MAPPED_IPWS.containsKey(parentItem)) {
            ItemPropertyWrapper originalWrapper = MAPPED_IPWS.get(parentItem);
            ItemPropertyWrapper newWrapper = new ItemPropertyWrapper(newItem);

            return copyProperties(originalWrapper, newWrapper);
        } else return create(newItem);
    }

    /**
     * Overloaded variant of {@link #of(Supplier, Supplier)} that copies the parent item's {@link Item.Properties}.
     *
     * @param newItemRegName The new registry name by which the newly constructed {@code Supplier<Item>} instance will
     *                       be stored.
     * @param parentItem The parent {@code Supplier<Item>} stored in {@link #MAPPED_IPWS}. Must already be a registered
     *                   {@link Item}.
     *
     * @return A new {@link ItemPropertyWrapper} instance with copied properties (including {@link Item.Properties})
     * based on the provided {@code Supplier<Item>}, or an entirely new/clean instance if no such IPW exists.
     *
     * @see #of(ItemPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     */
    public static ItemPropertyWrapper of(ResourceLocation newItemRegName, Supplier<Item> parentItem) {
        Item parentAsItem = parentItem.get();
        Item.Properties copiedProperties = new Item.Properties()
                .rarity(parentAsItem.getRarity(parentAsItem.getDefaultInstance()))
                .stacksTo(parentAsItem.getMaxStackSize());

        if (parentAsItem.getMaxDamage() > 0) copiedProperties.durability(parentAsItem.getMaxDamage()); // Needed to avoid conflict between stack size and durability
        if (parentAsItem.hasCraftingRemainingItem()) copiedProperties.craftRemainder(parentAsItem.getCraftingRemainingItem());
        if (parentAsItem.getFoodProperties() != null) copiedProperties.food(parentAsItem.getFoodProperties());
        if (parentAsItem.isFireResistant()) copiedProperties.fireResistant();
        if (parentAsItem.requiredFeatures() != FeatureFlags.VANILLA_SET) copiedProperties.requiredFeatures = parentAsItem.requiredFeatures(); // Direct setting cuz the builder method isn't actually a builder method

        return of(parentItem, NexusServices.REGISTRAR.registerObject(newItemRegName, () -> new Item(copiedProperties), BuiltInRegistries.ITEM));
    }

    /**
     * Shortcut utility method centered around copying builder properties over from one IPW instance to another.
     *
     * @param from The IPW instance to copy properties from.
     * @param to The IPW instance to copy properties to.
     *
     * @return The provided IPW instance with copied properties.
     */
    public static ItemPropertyWrapper copyProperties(ItemPropertyWrapper from, ItemPropertyWrapper to) {
        if (from.builder == null) return to;
        return to.builder()
                .withCustomName(from.builder.manuallyLocalizedItemName)
                .withCustomSeparatorWords(from.builder.definedSeparatorWords)
                .withLocalization(from.builder.itemTranslationFunc)
                .withSetTags(List.copyOf(from.builder.parentTags))
                .withCustomModelDefinitions(from.builder.imdMappingFunc)
                .withRecipe(from.builder.recipeBuilderFunction)
                .withSetParentCreativeModeTabs(List.copyOf(from.builder.parentTabs))
                .asCompostable(from.builder.itemCompostingMappingFunc)
                .asFuel(from.builder.itemFuelMappingFunc)
                .literalTranslation(from.builder.literalTranslation)
                .bypassDefaultTranslation(from.builder.bypassDefaultTranslation)
                .withSetModelPredicates(Map.copyOf(from.builder.itemModelPredicates))
                .excludeFromNativeDatagen(from.builder.excludeFromNativeDatagen)
                .requiresSetDatagenEntries(Map.copyOf(from.builder.mappedProviderRequisites))
                .build(); // Direct setting of the builder would copy the entire object itself, which would in-turn overwrite it if any calls are made to the copied IPW afterward
    }

    /**
     * Constructs a builder chain in which certain datagen properties can be assigned and re-built with in this
     * ItemPropertyWrapper instance. Also sets this IPW instance's {@link #builder} to the newly-constructed
     * {@link IPWBuilder} instance.
     * <br></br>
     * <b>NOTE: THIS WILL OVERRIDE {@link #builder} ENTIRELY EVEN IF IT'S NOT {@code null} (e.g. you're inheriting from a
     * template, see {@link #of(ItemPropertyWrapper, Supplier)}).</b>
     *
     * @return A new {@link IPWBuilder} instance from the {@link #builder} field.
     *
     * @see #cachedBuilder()
     */
    public IPWBuilder builder() {
        return this.builder = new IPWBuilder(this, parentItem);
    }

    /**
     * Gets the cached {@link IPWBuilder} instance from the {@link #builder} if it exists. May be {@code null}. Useful
     * for overriding specific properties after having copied another IPW instance/already set an IPWBuilder.
     *
     * @return The cached {@link IPWBuilder} instance, or {@code null} if the {@link #builder} is {@code null}.
     *
     * @see #of(ResourceLocation, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #builder()
     */
    @Nullable
    public IPWBuilder cachedBuilder() {
        return builder;
    }

    /**
     * Gets the parent {@code Supplier<Item>} of this IPW instance.
     *
     * @return The parent {@code Supplier<Item>} stored in this IPW instance.
     */
    public Supplier<Item> getParentItem() {
        return parentItem;
    }

    /**
     * Gets the manually localized item name from the {@link #builder()} if the builder exists.
     *
     * @return The manually localized item name, or an empty {@code String} if the {@link #builder()} is {@code null}.
     */
    public String getManuallyLocalizedItemName() {
        return builder == null ? "" : builder.manuallyLocalizedItemName;
    }

    /**
     * Gets whether this IPW instance bypasses default translation corrections.
     *
     * @return Whether this IPW instance bypasses default translation corrections.
     */
    public boolean hasLiteralTranslation() {
        return builder != null && builder.literalTranslation;
    }

    /**
     * Gets whether this IPW instance bypasses default translation altogether.
     *
     * @return Whether this IPW instance bypasses default translation altogether.
     */
    public boolean bypassDefaultTranslation() {
        return builder != null && builder.bypassDefaultTranslation;
    }

    /**
     * Gets the defined separator words from the {@link #builder()} if the builder exists.
     *
     * @return The defined separator words, or an empty {@link ObjectArrayList} if the {@link #builder()} is {@code null}.
     */
    public List<String> getDefinedSeparatorWords() {
        return builder == null ? ObjectArrayList.of() : builder.definedSeparatorWords;
    }

    /**
     * Gets the localization {@code Function<String, String>} from the {@link #builder()} if the builder exists, and it
     * is defined within said builder. May be {@code null}.
     *
     * @return The manual localization builder function, or {@code null} if the {@link #builder()} is {@code null} || it
     * isn't defined within said builder.
     */
    @Nullable
    public Function<String, String> getItemTranslationFunc() {
        return builder == null ? null : builder.itemTranslationFunc;
    }

    /**
     * Gets the defined parent {@linkplain Supplier<TagKey> Tags} from the {@link #builder()} if the builder exists.
     *
     * @return The defined parent {@linkplain Supplier<TagKey> Tags}, or an empty {@link ObjectArrayList} if the
     * {@link #builder()} is {@code null}.
     */
    public List<Supplier<TagKey<Item>>> getParentTags() {
        return builder == null ? ObjectArrayList.of() : builder.parentTags;
    }

    /**
     * Gets the {@code Function<Supplier<Item>, List<ItemModelDefinition>>} from the {@link #builder()} if the builder
     * exists, and it is defined within said builder. May be {@code null}.
     *
     * @return The item model builder function, or {@code null} if the {@link #builder()} is {@code null} || it isn't
     * defined within said builder.
     */
    @Nullable
    public Function<Supplier<Item>, List<ItemModelDefinition>> getIMDMappingFunction() {
        return builder == null ? null : builder.imdMappingFunc;
    }

    /**
     * Gets the {@code Function<Consumer<FinishedRecipe>, Consumer<Supplier<Item>>>} from the {@link #builder()} if the
     * builder exists, and it is defined within said builder. May be {@code null}.
     *
     * @return The recipe builder function, or {@code null} if the {@link #builder()} is {@code null} || it isn't
     * defined within said builder.
     */
    @Nullable
    public Function<Consumer<FinishedRecipe>, Consumer<Supplier<Item>>> getRecipeMappingFunction() {
        return builder == null ? null : builder.recipeBuilderFunction;
    }

    /**
     * Gets the {@link List} of parent {@linkplain CreativeModeTab CreativeModeTabs} from the {@link #builder()} if the
     * builder exists, and it is defined within said builder. May be empty.
     *
     * @return The {@link List} of parent {@linkplain CreativeModeTab CreativeModeTabs}, or an empty
     * {@link ObjectArrayList} if the {@link #builder()} is {@code null}.
     */
    public List<Supplier<CreativeModeTab>> getParentCreativeModeTabs() {
        return builder == null ? ObjectArrayList.of() : builder.parentTabs;
    }

    /**
     * Gets the composting {@code Function<Supplier<Item>, Float>} from the {@link #builder()} if the builder exists,
     * and it is defined within said builder. May be {@code null}.
     *
     * @return The item composting builder function, or {@code null} if the {@link #builder()} is {@code null} || it
     * isn't defined within said builder.
     */
    @Nullable
    public Function<Supplier<Item>, Float> getCompostingMappingFunc() {
        return builder == null ? null : builder.itemCompostingMappingFunc;
    }

    /**
     * Gets the fuel {@code Function<Supplier<Item>, Integer>} from the {@link #builder()} if the builder exists, and
     * it is defined within said builder. May be {@code null}.
     *
     * @return The fuel builder function, or {@code null} if the {@link #builder()} is {@code null} || it isn't defined
     * within said builder.
     */
    @Nullable
    public Function<Supplier<Item>, Integer> getItemFuelMappingFunc() {
        return builder == null ? null : builder.itemFuelMappingFunc;
    }

    /**
     * Gets the {@link Object2ObjectOpenHashMap} of item model predicates from the {@link #builder()} if the builder
     * exists, and it is defined within said builder.
     *
     * @return The {@link Object2ObjectOpenHashMap} of item model predicates, or an empty {@link Object2ObjectOpenHashMap}
     * if the {@link #builder()} is {@code null}.
     */
    public Object2ObjectOpenHashMap<ResourceLocation, WrappedClampedItemPropertyFunction> getItemModelPredicates() {
        return builder == null ? new Object2ObjectOpenHashMap<>() : builder.itemModelPredicates;
    }

    /**
     * Whether data present in {@link #builder()} (if not {@code null}) should automatically be handled/generated by
     * Nexus API.
     *
     * @return {@code true} if {@link #builder} isn't {@code null} and {@link IPWBuilder#excludeFromNativeDatagen} is set
     * to {@code true}, {@code false} otherwise.
     */
    public boolean excludeFromNativeDatagen() {
        return builder != null && builder.excludeFromNativeDatagen;
    }

    /**
     * Gets a {@link Map} (usually {@link Object2BooleanOpenHashMap}) specifying the {@linkplain ProviderType ProviderTypes}
     * for which this IPW instance requires data to present for generation. May be empty if {@link #builder} is {@code null}
     * or the underlying {@link Map} is also empty.
     *
     * @return The {@link Map} representing different {@linkplain ProviderType ProviderTypes} and their requirements for
     * datagen. May be empty.
     */
    public Map<ProviderType, Boolean> getProviderTypeRequisites() {
        return builder == null ? new Object2BooleanOpenHashMap<>() : builder.mappedProviderRequisites;
    }

    /**
     * Whether this IPW instance is a template. Templates are not stored in {@link #getMappedIpws()} and have no parent
     * {@link Item}.
     *
     * @return Whether this IPW instance is a template.
     *
     * @see #of(ItemPropertyWrapper, Supplier)
     * @see #createTemplate()
     */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
     * Gets an immutable view (via {@link ImmutableMap}) of {@link #MAPPED_IPWS}.
     *
     * @return An immutable view (via {@link ImmutableMap}) of {@link #MAPPED_IPWS}.
     */
    public static ImmutableMap<Supplier<Item>, ItemPropertyWrapper> getMappedIpws() {
        return ImmutableMap.copyOf(MAPPED_IPWS);
    }

    /**
     * A builder class used to construct certain item-related data for datagen and other data related to hardcoded item
     * settings, such as fuel-ability.
     */
    public static class IPWBuilder {
        private final ItemPropertyWrapper ownerWrapper;
        private final Supplier<Item> itemParent;
        private String manuallyLocalizedItemName = "";
        private List<String> definedSeparatorWords = ObjectArrayList.of();
        private final List<Supplier<TagKey<Item>>> parentTags = ObjectArrayList.of();
        @Nullable
        private Function<Consumer<FinishedRecipe>, Consumer<Supplier<Item>>> recipeBuilderFunction;
        @Nullable
        private Function<Supplier<Item>, List<ItemModelDefinition>> imdMappingFunc;
        private final List<Supplier<CreativeModeTab>> parentTabs = new ObjectArrayList<>();
        @Nullable
        private Function<Supplier<Item>, Float> itemCompostingMappingFunc;
        @Nullable
        private Function<Supplier<Item>, Integer> itemFuelMappingFunc;
        @Nullable
        private Function<String, String> itemTranslationFunc;
        private boolean literalTranslation = false;
        private boolean bypassDefaultTranslation = false;
        private final Object2ObjectOpenHashMap<ResourceLocation, WrappedClampedItemPropertyFunction> itemModelPredicates = new Object2ObjectOpenHashMap<>();
        private boolean excludeFromNativeDatagen = false;
        private final Map<ProviderType, Boolean> mappedProviderRequisites = new Object2BooleanOpenHashMap<>();

        private IPWBuilder(ItemPropertyWrapper ownerWrapper, Supplier<Item> itemParent) {
            this.ownerWrapper = ownerWrapper;
            this.itemParent = itemParent;
        }

        /**
         * Assigns a custom translation key for datagen. By default, a basic regex algorithm is used to automatically localize
         * the item name into something more legible (I.E. The names you see in-game). This property is simply an override
         * mechanic which aims to give the end-developer more control over the resulting name instead of being forced to rely on
         * the aforementioned algorithm.
         * <p></p>
         * The algorithm in question, in a nutshell, works as follows (the code block below is purely demonstrative of the
         * localization process and has nothing to do with how the algorithm is actually written):
         * <pre>
         *     {@code
         *      public class AlgorithmExampleDescriptor {
         *
         *          public static void main(String[] args) {
         *              // Input
         *              String unlocalizedName = "item.mymodid.my_item"; // The registry name/initial un-localized name
         *
         *              // Steps
         *              AlgorithmLanguageProvider.validateNullity(unlocalizedName); // Checks whether the provided 'unlocalizedName' is empty/all whitespaces/you get the point
         *              AlgorithmLanguageProvider.validateRegex(unlocalizedName); // Checks whether the provided 'unlocalizedName' has the signature registry name separator character "."
         *              AlgorithmLanguageProvider.formatCaps(unlocalizedName); // Output: "Item.Mymodid.My_Item" <-- Capitalizes the first letter of each word based on regex-checks for special separators ("." and "_") (First character all the way to the left is always capitalized (duh), not that it matters)
         *              AlgorithmLanguageProvider.formatSeparators(unlocalizedName); // Output: "Item.Mymodid.My_Item" <-- Any defined "separator" Strings are lowercased, see #withCustomSeparatorWords(List). In this case, there aren't any, so this step does nothing
         *              AlgorithmLanguageProvider.formatSpecialSeparators(unlocalizedName); // Output: "My Item" <-- All characters preceding the last "." are substringed/removed, and then any "_" characters are replaced with whitespaces
         *
         *              // End result
         *              System.out.println(unlocalizedName); // Output: "My Item"
         *          }
         *      }
         *     }
         * </pre>
         *
         * @param manuallyLocalizedItemName The name override used to localize the parent {@linkplain Item Item's}
         *                                  registry name.
         *
         * @return {@code this} (builder method).
         *
         * @see #withCustomSeparatorWords(List)
         * @see #withLocalization(Function)
         * @see #literalTranslation(boolean)
         * @see #bypassDefaultTranslation(boolean)
         */
        public IPWBuilder withCustomName(String manuallyLocalizedItemName) {
            this.manuallyLocalizedItemName = manuallyLocalizedItemName;
            return this;
        }

        /**
         * Assigns a {@link List} of custom separator words which are lowercased during the algorithm's de-localization
         * process. This is ignored if {@link #manuallyLocalizedItemName} is defined, {@link #literalTranslation} is
         * {@code true}, or if {@link #itemTranslationFunc} is non-null.
         *
         * @param definedSeparatorWords The {@link List} of custom separator words to lowercase while the algorithm is
         *                              running.
         *
         * @return {@code this} (builder method).
         *
         * @apiNote The default entries for this are {"Of", "And"}. This {@link List} is appended to the default
         * separator definitions rather than replacing them.
         *
         * @see #withCustomName(String)
         * @see #withLocalization(Function)
         * @see #literalTranslation(boolean)
         * @see #bypassDefaultTranslation(boolean)
         */
        public IPWBuilder withCustomSeparatorWords(List<String> definedSeparatorWords) {
            this.definedSeparatorWords = definedSeparatorWords;
            return this;
        }

        /**
         * Marks this builder as using literal translations, meaning that corrections (like the one seen in the example
         * provided by {@link #withCustomName(String)}) are not applied.
         *
         * @param literalTranslation Whether to use literal translations.
         *
         * @return {@code this} (builder method).
         *
         * @apiNote Useless on items (for now) since the translation algorithm always produces a literal translation
         * (unless {@link #getItemTranslationFunc()} isn't {@code null}, in which case calling this cancels the
         * translation function out, e.g. if you're inheriting from another template, and you don't want to have it. You
         * could also just set the function to {@code null}. Whatever floats your boat, really).
         *
         * @see #withCustomName(String)
         * @see #withLocalization(Function)
         * @see #literalTranslation()
         * @see #bypassDefaultTranslation(boolean)
         */
        public IPWBuilder literalTranslation(boolean literalTranslation) {
            this.literalTranslation = literalTranslation;
            return this;
        }

        /**
         * Whether this IPWBuilder instance should skip the translation process altogether.
         * <br></br>
         * Note that data won't be generated for this instance (NPEs may be thrown too, based on the validation policy
         * for your mod) unless {@link #literalTranslation(boolean)} is marked as {@code true} or {@link #withCustomName(String)}
         * is set to a non-{@code null} value.
         *
         * @return {@code this} (builder method).
         *
         * @see #literalTranslation(boolean)
         * @see #withCustomName(String)
         * @see #bypassDefaultTranslation()
         */
        public IPWBuilder bypassDefaultTranslation(boolean bypassDefaultTranslation) {
            this.bypassDefaultTranslation = bypassDefaultTranslation;
            return this;
        }

        /**
         * Overloaded variant of {@link #bypassDefaultTranslation(boolean)}, marking this builder to be skipped by the
         * default localization algorithm Nexus API employs. See the base variant for more info.
         *
         * @return {@link #bypassDefaultTranslation(boolean)}
         *
         * @see #literalTranslation(boolean)
         * @see #withCustomName(String)
         * @see #bypassDefaultTranslation(boolean)
         */
        public IPWBuilder bypassDefaultTranslation() {
            return bypassDefaultTranslation(true);
        }

        /**
         * A custom {@link Function} to apply miscellaneous modifications to the resulting localized item name. This is
         * influenced by {@link #withCustomName(String)} and {@link #literalTranslation(boolean)}, where applicable.
         *
         * @param itemTranslationFunc The {@link Function} responsible for directly modifying the resulting localized
         *                            item name.
         *
         * @return {@code this} (builder method).
         *
         * @apiNote To be exact, {@link #withCustomName(String)} and {@link #literalTranslation(boolean)} both cancel
         * this function out if set.
         *
         * @see #withCustomName(String)
         * @see #literalTranslation(boolean)
         */
        public IPWBuilder withLocalization(Function<String, String> itemTranslationFunc) {
            this.itemTranslationFunc = itemTranslationFunc;
            return this;
        }

        /**
         * Overloaded variant of {@link #literalTranslation(boolean)} which marks this builder as using literal
         * translations.
         *
         * @return {@code this} (builder method).
         *
         * @see #literalTranslation(boolean)
         */
        public IPWBuilder literalTranslation() {
            return literalTranslation(true);
        }

        /**
         * Tags this IPWBuilder's parent {@link Item} with the provided {@link TagKey<Item>}.
         *
         * @param parentItemTag The {@code TagKey<Item>} with which this IPW's parent {@link Item} will be tagged. May
         *                      only be of type {@link Item}.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder withTag(Supplier<TagKey<Item>> parentItemTag) {
            this.parentTags.add(parentItemTag);
            return this;
        }

        /**
         * Tags this IPWBuilder's parent {@link Item} with the provided {@linkplain TagKey<Item> Tags}. Appends to the
         * existing list.
         *
         * @param parentItemTags The {@linkplain TagKey<Item> TagKeys} with which this IPW's parent {@link Item} will be
         *                       tagged. May only be of type {@link Item}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withSetTags(List)
         */
        public IPWBuilder withTags(List<Supplier<TagKey<Item>>> parentItemTags) {
            this.parentTags.addAll(parentItemTags);
            return this;
        }

        /**
         * Tags this IPWBuilder's parent item with the provided {@linkplain TagKey<Item> Tags}. Overwrites the existing
         * {@link List}.
         *
         * @param parentItemTags The {@linkplain TagKey<Item> TagKeys} with which this IPW's parent {@link Item} will be
         *                       tagged. May only be of type {@link Item}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withTags(List)
         */
        public IPWBuilder withSetTags(List<Supplier<TagKey<Item>>> parentItemTags) {
            this.parentTags.clear();
            this.parentTags.addAll(parentItemTags);
            return this;
        }

        /**
         * Sets the {@link #imdMappingFunc} of this IPWBuilder. This is used in order to generate models for the parent
         * IPW's {@link Item}.
         *
         * @param imdMappingFunc The mapping {@link Function} used to build this IPWBuilder's parent item's model in
         *                       datagen.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder withCustomModelDefinitions(Function<Supplier<Item>, List<ItemModelDefinition>> imdMappingFunc) {
            this.imdMappingFunc = imdMappingFunc;
            return this;
        }

        /**
         * Defines a custom mapping function representing the parent {@linkplain Item Item's} recipe. IPWBuilders
         * accepting more than 1 recipe function assume that each recipe has a unique recipe ID, and thus recipes are
         * generated under that constraint.
         *
         * @param recipeBuilderFunction The mapping function accepting a representation of the parent
         *                              {@linkplain Item Item's} recipe. Input is the recipe, output is the parent IPW's
         *                              {@link Item}.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder withRecipe(Function<Consumer<FinishedRecipe>, Consumer<Supplier<Item>>> recipeBuilderFunction) {
            this.recipeBuilderFunction = recipeBuilderFunction;
            return this;
        }

        /**
         * Appends a parent {@link CreativeModeTab} for the parent IPW's {@link Item} to show up in.
         *
         * @param parentTab The {@link CreativeModeTab} under which the parent IPW's {@link Item} will be listed/show up.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder withParentCreativeModeTab(Supplier<CreativeModeTab> parentTab) {
            this.parentTabs.add(parentTab);
            return this;
        }

        /**
         * Appends a {@link List} of parent {@linkplain CreativeModeTab CreativeModeTabs} for the parent IPW's
         * {@link Item} to show up in.
         *
         * @param parentTabs A {@link List} of {@linkplain CreativeModeTab CreativeModeTabs} under which the parent
         *                   IPW's {@link Item} will be listed/show up.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder withParentCreativeModeTabs(List<Supplier<CreativeModeTab>> parentTabs) {
            this.parentTabs.addAll(parentTabs);
            return this;
        }

        /**
         * Sets (does NOT append) a {@link List} of parent {@linkplain CreativeModeTab CreativeModeTabs} for the parent
         * IPW's {@link Item} to show up in.
         *
         * @param parentTabs A {@link List} of {@linkplain CreativeModeTab CreativeModeTabs} under which the parent
         *                   IPW's {@link Item} will be listed/show up.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder withSetParentCreativeModeTabs(List<Supplier<CreativeModeTab>> parentTabs) {
            this.parentTabs.clear();
            this.parentTabs.addAll(parentTabs);
            return this;
        }

        /**
         * Defines a custom mapping function representing the parent {@linkplain Item Item's} composting chance.
         *
         * @param itemCompostingMappingFunc The mapping function accepting a representation of the parent
         *                                  {@linkplain Item Item's} composting, with the output {@link Float} value
         *                                  representing the composting chance. {@code null}/0 values are ignored.
         *                                  Negative values are abs'd.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder asCompostable(Function<Supplier<Item>, Float> itemCompostingMappingFunc) {
            this.itemCompostingMappingFunc = itemCompostingMappingFunc;
            return this;
        }

        /**
         * Defines a custom mapping function representing the parent {@linkplain Item Item's} cook time as a fuel.
         *
         * @param itemFuelMappingFunc The mapping function accepting a representation of the parent
         *                            {@linkplain Item Item's} cook time (in ticks), with the output {@link Integer}
         *                            value representing the composting chance. {@code null}/0 values are ignored.
         *                            Negative values are abs'd.
         *
         * @return {@code this} (builder method).
         */
        public IPWBuilder asFuel(Function<Supplier<Item>, Integer> itemFuelMappingFunc) {
            this.itemFuelMappingFunc = itemFuelMappingFunc;
            return this;
        }

        /**
         * Defines a {@link WrappedClampedItemPropertyFunction} to be properly registered for use on the client for use in
         * texture overrides.
         *
         * @param modelOverrideID A {@link ResourceLocation} representing the name of the model predicate.
         * @param modelOverrideFunction The {@link WrappedClampedItemPropertyFunction} to register. Must be registered if you
         *                              plan on adding any custom texture overrides. This should preferably be a
         *                              {@code static} constant stored somewhere.
         *
         * @return {@code this} (builder method).
         *
         * @see #withModelPredicates(Map)
         * @see #withSetModelPredicates(Map)
         */
        public IPWBuilder withModelPredicate(ResourceLocation modelOverrideID, WrappedClampedItemPropertyFunction modelOverrideFunction) {
            itemModelPredicates.put(modelOverrideID, modelOverrideFunction); // No need for putIfAbsent, we can just do value overrides instead
            return this;
        }

        /**
         * Defines a {@link Map} of {@link WrappedClampedItemPropertyFunction} objects to be properly registered for use on the
         * client in terms of texture overrides, mapped to key {@link ResourceLocation} objects representing their names.
         * This method appends to the existing {@link Map}.
         *
         * @param modelOverrideFunctions A {@link Map} of {@link WrappedClampedItemPropertyFunction} objects to register,
         *                               mapped to their key names. Must be registered if you plan on adding any custom
         *                               texture overrides. This should preferably be a {@code static} constant stored
         *                               somewhere.
         *
         * @return {@code this} (builder method).
         *
         * @see #withModelPredicate(ResourceLocation, WrappedClampedItemPropertyFunction)
         * @see #withSetModelPredicates(Map)
         */
        public IPWBuilder withModelPredicates(Map<ResourceLocation, WrappedClampedItemPropertyFunction> modelOverrideFunctions) {
            itemModelPredicates.putAll(modelOverrideFunctions);
            return this;
        }

        /**
         * Defines a {@link Map} of {@link WrappedClampedItemPropertyFunction} objects to be properly registered for use on the
         * client in terms of texture overrides, mapped to key {@link ResourceLocation} objects representing their names.
         * This method overrides the existing {@link Map}.
         *
         * @param modelOverrideFunctions A {@link Map} of {@link WrappedClampedItemPropertyFunction} objects to register,
         *                               mapped to their key names. Must be registered if you plan on adding any custom
         *                               texture overrides. This should preferably be a {@code static} constant stored
         *                               somewhere.
         *
         * @return {@code this} (builder method).
         *
         * @see #withModelPredicate(ResourceLocation, WrappedClampedItemPropertyFunction)
         * @see #withModelPredicates(Map) (Map)
         */
        public IPWBuilder withSetModelPredicates(Map<ResourceLocation, WrappedClampedItemPropertyFunction> modelOverrideFunctions) {
            itemModelPredicates.clear();
            itemModelPredicates.putAll(modelOverrideFunctions);
            return this;
        }

        /**
         * Determines whether this IPWBuilder instance should be entirely excluded from Nexus' native datagen.
         * <br></br>
         * Fundamentally, all this does is flag this instance as not needing a data entry to be mapped to it. You may
         * choose to generate data for it yourself if needed, since Nexus won't handle datagen for this particular object.
         * <br></br>
         * If a item-specific data provider has {@link ModDataProvider#validateAllEntries()} set to {@code true}, this
         * instance (and its children, so long as this value isn't modified) will still be excluded from datagen, and thus
         * an exception won't be thrown for it.
         *
         * @param excludeFromNativeDatagen Whether this instance's data should be passed into Nexus' native datagen for
         *                                 data generation.
         *
         * @return {@code this} (builder method).
         *
         * @see #excludeFromNativeDatagen()
         * @see #requiresDatagenEntry(ProviderType, boolean)
         */
        public IPWBuilder excludeFromNativeDatagen(boolean excludeFromNativeDatagen) {
            this.excludeFromNativeDatagen = excludeFromNativeDatagen;
            return this;
        }

        /**
         * Determines whether this IPWBuilder instance is required to generate necessary item-related data based on the
         * {@link ProviderType} passed in.
         * <br></br>
         * By default, unmapped providers will not require an entry for this IPWBuilder to be generated unless
         * {@link ModDataProvider#validateAllEntries()} is set to {@code true}.
         * <br></br>
         * Mapping the related provider passed in here to {@code requiresDatagenEntry}, set to {@code true}, will flag
         * this IPWBuilder instance for requiring related data regardless of what
         * {@link ModDataProvider#validateAllEntries()} is set to.
         *
         * @param targetProviderType The {@link ProviderType} to modify the data entry requirement for.
         * @param requiresDatagenEntry Whether this IPWBuilder should require data related to the specified
         *                             {@code targetProviderType} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public IPWBuilder requiresDatagenEntry(ProviderType targetProviderType, boolean requiresDatagenEntry) {
            mappedProviderRequisites.put(targetProviderType, requiresDatagenEntry);
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}.
         *
         * @param targetProviderTypes The {@link List} of {@linkplain ProviderType ProviderTypes} to modify the data
         *                            entry requirements for.
         * @param requiresDatagenEntry Whether this IPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public IPWBuilder requiresDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
            targetProviderTypes.forEach(type -> requiresDatagenEntry(type, requiresDatagenEntry));
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}. Overrides the existing
         * {@link Map}.
         *
         * @param targetProviderTypes The {@link List} of {@linkplain ProviderType ProviderTypes} to modify the data
         *                            entry requirements for.
         * @param requiresDatagenEntry Whether this IPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public IPWBuilder requiresSetDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
            mappedProviderRequisites.clear();
            targetProviderTypes.forEach(type -> requiresDatagenEntry(type, requiresDatagenEntry));
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}. Overrides the existing
         * {@link Map}.
         *
         * @param mappedProviderRequisites The {@link Map} of provider requisites to override the existing {@link Map}
         *                                 with.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public IPWBuilder requiresSetDatagenEntries(Map<ProviderType, Boolean> mappedProviderRequisites) {
            this.mappedProviderRequisites.clear();
            this.mappedProviderRequisites.putAll(mappedProviderRequisites);
            return this;
        }

        /**
         * Builds a new {@link ItemPropertyWrapper} using this builder's data. Also maps the owner
         * {@link ItemPropertyWrapper} to the parent {@linkplain Item} if the owner is not a template.
         *
         * @return The newly data-populated {@link ItemPropertyWrapper}.
         *
         * @see ItemPropertyWrapper#isTemplate()
         */
        public ItemPropertyWrapper build() {
            if (!ownerWrapper.isTemplate) MAPPED_IPWS.putIfAbsent(ownerWrapper.itemRegName == null ? ownerWrapper.parentItem : () -> BuiltInRegistries.ITEM.get(ownerWrapper.itemRegName), ownerWrapper);
            return ownerWrapper;
        }
    }
}
