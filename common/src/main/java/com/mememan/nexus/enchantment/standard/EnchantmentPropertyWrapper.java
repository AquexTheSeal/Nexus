package com.mememan.nexus.enchantment.standard;

import com.google.common.collect.ImmutableMap;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wrapper {@code class} used to store information referenced in datagen, as well as some other hardcoded enchantment-
 * oriented info, to simplify enchantment creation/registration.
 */
public class EnchantmentPropertyWrapper {
    private static final Object2ObjectLinkedOpenHashMap<Supplier<Enchantment>, EnchantmentPropertyWrapper> MAPPED_EPWS = new Object2ObjectLinkedOpenHashMap<>();
    @Nullable
    private final ResourceLocation enchantmentRegName;
    private final Supplier<Enchantment> parentEnchantment;
    private final boolean isTemplate;
    @Nullable
    private EPWBuilder builder;

    private EnchantmentPropertyWrapper(@Nullable ResourceLocation enchantmentRegName, Supplier<Enchantment> parentEnchantment) {
        this.enchantmentRegName = enchantmentRegName;
        this.parentEnchantment = parentEnchantment;
        this.isTemplate = false;
    }

    private EnchantmentPropertyWrapper(Supplier<Enchantment> parentEnchantment) {
        this(null, parentEnchantment);
    }

    private EnchantmentPropertyWrapper() {
        this.enchantmentRegName = null;
        this.parentEnchantment = null;
        this.isTemplate = true; // Otherwise can't set with constructor overloading (Laziness:tm:)
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to directly create an EPW during a registration call rather
     * than after it/without storing it.
     *
     * @param enchantmentRegName The registry name of the {@code parentEnchantment}. Used when access to
     *                           the {@code parentEnchantment} returns a fortune delegate (I.E. It's too early to access
     *                           the parent enchantment, see {@link BuiltInRegistries#ENCHANTMENT}).
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in the newly-initialized EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance.
     *
     * @apiNote You shouldn't have to use this method for 95% of cases since you'd be inlining a registration call for
     * {@link #parentEnchantment}, see {@link #create(Supplier)} instead.
     *
     * @see #create(Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     */
    public static EnchantmentPropertyWrapper create(ResourceLocation enchantmentRegName, Supplier<Enchantment> parentEnchantment) {
        return new EnchantmentPropertyWrapper(enchantmentRegName, parentEnchantment);
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to create an EPW instance with a stored registration call,
     * such that its parent {@code Supplier<Enchantment>} is not a fortune enchantment delegate (see {@link BuiltInRegistries#ENCHANTMENT}).
     *
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in the newly-initialized EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     */
    public static EnchantmentPropertyWrapper create(Supplier<Enchantment> parentEnchantment) {
        return new EnchantmentPropertyWrapper(parentEnchantment);
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance as a template. Template EPWs are not stored in
     * {@link #MAPPED_EPWS} and do not store a parent {@link Enchantment}. They're particularly useful for re-using across
     * multiple {@linkplain Enchantment Enchantments}.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance, set as a template.
     *
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #isTemplate()
     * @see #ofTemplate(EnchantmentPropertyWrapper)
     */
    public static EnchantmentPropertyWrapper createTemplate() {
        return new EnchantmentPropertyWrapper();
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance as a template, inheriting data from the provided EPW
     * template. Template EPWs are not stored in {@link #MAPPED_EPWS} and do not store a parent {@link Enchantment}.
     * They're particularly useful for re-using across multiple {@linkplain Enchantment Enchantments}.
     *
     * @param parentTemplateWrapper The parent {@link EnchantmentPropertyWrapper} template from which {@link #builder()}
     *                              data should be copied.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance, set as a template, inheriting from the provided EPW template.
     * If the provided EPW template is {@code null}, returns {@link #createTemplate()}.
     *
     * @see #createTemplate()
     * @see #isTemplate()
     */
    public static EnchantmentPropertyWrapper ofTemplate(EnchantmentPropertyWrapper parentTemplateWrapper) {
        if (parentTemplateWrapper != null) {
            EnchantmentPropertyWrapper newTemplateWrapper = new EnchantmentPropertyWrapper();

            return copyProperties(parentTemplateWrapper, newTemplateWrapper);
        } else return createTemplate();
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance based on the provided {@link EnchantmentPropertyWrapper}.
     * If the provided EPW instance is {@code null}, returns {@link #create(Supplier)}. You'd typically use this if you
     * have an EPW template you want multiple registered {@linkplain Enchantment Enchantments} to inherit from.
     *
     * @param parentWrapper The parent {@link EnchantmentPropertyWrapper} instance from which {@link #builder()} should
     *                      be copied.
     * @param newEnchantment The new registry entry to use for the newly constructed EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance with copied properties based on the provided EPW, or an
     * entirely new/clean instance if the provided EPW is {@code null}.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #createTemplate()
     */
    public static EnchantmentPropertyWrapper of(EnchantmentPropertyWrapper parentWrapper, Supplier<Enchantment> newEnchantment) {
        if (parentWrapper != null) {
            EnchantmentPropertyWrapper newWrapper = new EnchantmentPropertyWrapper(newEnchantment);

            return copyProperties(parentWrapper, newWrapper);
        } else return create(newEnchantment);
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance from an existing {@link EnchantmentPropertyWrapper}
     * instance based on the provided {@code Supplier<Enchantment>}. If no such existing EPW instance exists, returns
     * {@link #create(Supplier)}.
     *
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in {@link #MAPPED_EPWS}. Copies its EPW instance's
     *                   {@link EPWBuilder} properties if it exists, or creates a clean new EPW instance if it doesn't.
     * @param newEnchantment The new registry entry to use for the newly constructed EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance with copied properties based on the provided
     * {@code Supplier<Enchantment>}, or an entirely new/clean instance if no such EPW exists.
     *
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     */
    public static EnchantmentPropertyWrapper of(Supplier<Enchantment> parentEnchantment, Supplier<Enchantment> newEnchantment) {
        if (MAPPED_EPWS.containsKey(parentEnchantment)) {
            EnchantmentPropertyWrapper originalWrapper = MAPPED_EPWS.get(parentEnchantment);
            EnchantmentPropertyWrapper newWrapper = new EnchantmentPropertyWrapper(newEnchantment);

            return copyProperties(originalWrapper, newWrapper);
        } else return create(newEnchantment);
    }

    /**
     * Overloaded variant of {@link #of(Supplier, Supplier)} that copies the parent enchantment's
     * {@linkplain Enchantment#getRarity() rarity}, {@linkplain Enchantment#category category}, and
     * {@linkplain Enchantment#slots slots}. Note that this does not copy the provided enchantment's actual behaviour.
     *
     * @param newEnchantmentRegName The new registry name by which the newly constructed {@code Supplier<Enchantment>} instance
     *                              will be stored.
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in {@link #MAPPED_EPWS}. Must already be
     *                          a registered {@link Enchantment}.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance with copied properties (rarity, slots, category)
     * based on the provided {@code Supplier<Enchantment>}, or an entirely new/clean instance if no such EPW exists.
     *
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     */
    public static EnchantmentPropertyWrapper of(ResourceLocation newEnchantmentRegName, Supplier<Enchantment> parentEnchantment) {
        Enchantment parentAsEnchantment = parentEnchantment.get();

        return of(parentEnchantment, NexusServices.REGISTRAR.registerObject(newEnchantmentRegName, () -> new Enchantment(parentAsEnchantment.getRarity(), parentAsEnchantment.category, parentAsEnchantment.slots) {}, BuiltInRegistries.ENCHANTMENT));
    }

    /**
     * Shortcut utility method centered around copying builder properties over from one EPW instance to another.
     *
     * @param from The EPW instance to copy properties from.
     * @param to The EPW instance to copy properties to.
     *
     * @return The provided EPW instance with copied properties.
     */
    public static EnchantmentPropertyWrapper copyProperties(EnchantmentPropertyWrapper from, EnchantmentPropertyWrapper to) {
        if (from.builder == null) return to;
        return to.builder()
                .excludeFromNativeDatagen(from.builder.excludeFromNativeDatagen)
                .literalTranslation(from.builder.literalTranslation)
                .withCustomSeparatorWords(List.copyOf(from.builder.definedSeparatorWords))
                .withCustomName(from.builder.manuallyLocalizedEnchantmentName)
                .withLocalization(from.builder.enchantmentTranslationFunc)
                .bypassDefaultTranslation(from.builder.bypassDefaultTranslation)
                .withSetTags(List.copyOf(from.builder.parentTags))
                .requiresSetDatagenEntries(Map.copyOf(from.builder.mappedProviderRequisites))
                .build(); // Direct setting of the builder would copy the entire object itself, which would in-turn overwrite it if any calls are made to the copied EPW afterward
    }

    /**
     * Constructs a builder chain in which certain datagen properties can be assigned and re-built with in this
     * EnchantmentPropertyWrapper instance. Also sets this EPW instance's {@link #builder} to the newly-constructed
     * {@link EPWBuilder} instance.
     * <br></br>
     * <b>NOTE: THIS WILL OVERRIDE {@link #builder} ENTIRELY EVEN IF IT'S NOT {@code null} (e.g. you're inheriting from
     * a template, see {@link #of(EnchantmentPropertyWrapper, Supplier)}).</b>
     *
     * @return A new {@link EPWBuilder} instance from the {@link #builder} field.
     *
     * @see #cachedBuilder()
     */
    public EPWBuilder builder() {
        return this.builder = new EPWBuilder(this, parentEnchantment);
    }

    /**
     * Gets the cached {@link EPWBuilder} instance from the {@link #builder} if it exists. May be {@code null}. Useful
     * for overriding specific properties after having copied another EPW instance/already set an EPWBuilder.
     *
     * @return The cached {@link EPWBuilder} instance, or {@code null} if the {@link #builder} is {@code null}.
     *
     * @see #of(ResourceLocation, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #builder()
     */
    @Nullable
    public EPWBuilder cachedBuilder() {
        return builder;
    }

    /**
     * Gets the parent {@code Supplier<Enchantment>} of this EPW instance.
     *
     * @return The parent {@code Supplier<Enchantment>} stored in this EPW instance.
     */
    public Supplier<Enchantment> getParentEnchantment() {
        return parentEnchantment;
    }

    /**
     * Gets the manually localized enchantment name from the {@link #builder()} if the builder exists.
     *
     * @return The manually localized enchantment name, or an empty {@code String} if the {@link #builder()} is {@code null}.
     */
    public String getManuallyLocalizedEnchantmentName() {
        return builder == null ? "" : builder.manuallyLocalizedEnchantmentName;
    }

    /**
     * Gets whether this EPW instance bypasses default translation corrections.
     *
     * @return Whether this EPW instance bypasses default translation corrections.
     */
    public boolean hasLiteralTranslation() {
        return builder != null && builder.literalTranslation;
    }

    /**
     * Gets whether this EPW instance bypasses default translation altogether.
     *
     * @return Whether this EPW instance bypasses default translation altogether.
     */
    public boolean bypassDefaultTranslation() {
        return builder != null && builder.bypassDefaultTranslation;
    }

    /**
     * Gets the defined separator words from the {@link #builder()} if the builder exists.
     *
     * @return The defined separator words, or an empty {@link ObjectArrayList} if the {@link #builder()} is
     * {@code null}.
     */
    public List<String> getDefinedSeparatorWords() {
        return builder == null ? ObjectArrayList.of() : builder.definedSeparatorWords;
    }

    /**
     * Gets the localization {@code Function<String, String>} from the {@link #builder()} if the builder exists, and it
     * is defined within said builder. May be {@code null}.
     *
     * @return The {@code Function<String, String>}, or {@code null} if the {@link #builder()} is {@code null} || it
     * isn't defined within said builder.
     */
    @Nullable
    public Function<String, String> getEnchantmentTranslationFunc() {
        return builder == null ? null : builder.enchantmentTranslationFunc;
    }

    /**
     * Gets the defined parent {@linkplain Supplier<TagKey< Enchantment >> Tags} from the {@link #builder()} if the builder 
     * exists.
     *
     * @return The defined parent {@linkplain Supplier<TagKey<Enchantment>> Tags}, or an empty {@link ObjectArrayList} if 
     * the {@link #builder()} is {@code null}.
     */
    public List<Supplier<TagKey<Enchantment>>> getParentEnchantmentTags() {
        return builder == null ? ObjectArrayList.of() : builder.parentTags;
    }

    /**
     * Whether data present in {@link #builder()} (if not {@code null}) should automatically be handled/generated by
     * Nexus API.
     *
     * @return {@code true} if {@link #builder} isn't {@code null} and {@link EPWBuilder#excludeFromNativeDatagen} is set
     * to {@code true}, {@code false} otherwise.
     */
    public boolean excludeFromNativeDatagen() {
        return builder != null && builder.excludeFromNativeDatagen;
    }

    /**
     * Gets a {@link Map} (usually {@link Object2BooleanOpenHashMap}) specifying the {@linkplain ProviderType ProviderTypes}
     * for which this EPW instance requires data to present for generation. May be empty if {@link #builder} is {@code null}
     * or the underlying {@link Map} is also empty.
     *
     * @return The {@link Map} representing different {@linkplain ProviderType ProviderTypes} and their requirements for
     * datagen. May be empty.
     */
    public Map<ProviderType, Boolean> getProviderTypeRequisites() {
        return builder == null ? new Object2BooleanOpenHashMap<>() : builder.mappedProviderRequisites;
    }

    /**
     * Whether this EPW instance is a template. Templates are not stored in {@link #getMappedEpws()} and have no parent
     * {@link Enchantment}.
     *
     * @return Whether this EPW instance is a template.
     *
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #createTemplate()
     */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
     * Gets an immutable view (via {@link ImmutableMap}) of {@link #MAPPED_EPWS}.
     *
     * @return An immutable view (via {@link ImmutableMap}) of {@link #MAPPED_EPWS}.
     */
    public static ImmutableMap<Supplier<Enchantment>, EnchantmentPropertyWrapper> getMappedEpws() {
        return ImmutableMap.copyOf(MAPPED_EPWS);
    }

    /**
     * A builder class used to construct certain enchantment-related data for datagen and other data related to hardcoded
     * enchantment settings, such as enchantment name and level.
     */
    public static class EPWBuilder {
        private final EnchantmentPropertyWrapper ownerWrapper;
        private final Supplier<Enchantment> enchantmentParent;
        private String manuallyLocalizedEnchantmentName = "";
        private List<String> definedSeparatorWords = ObjectArrayList.of();
        private final List<Supplier<TagKey<Enchantment>>> parentTags = ObjectArrayList.of();
        @Nullable
        private Function<String, String> enchantmentTranslationFunc;
        private boolean literalTranslation = false;
        private boolean bypassDefaultTranslation = false;
        private boolean excludeFromNativeDatagen = false;
        private final Map<ProviderType, Boolean> mappedProviderRequisites = new Object2BooleanOpenHashMap<>();
        private final Map<Component, String> enchantmentTooltips = new Object2ObjectOpenHashMap<>();

        public EPWBuilder(EnchantmentPropertyWrapper ownerWrapper, Supplier<Enchantment> enchantmentParent) {
            this.ownerWrapper = ownerWrapper;
            this.enchantmentParent = enchantmentParent;
        }

        /**
         * Assigns a custom translation key for datagen. By default, a basic regex algorithm is used to automatically localize
         * the block name into something more legible (I.E. The names you see in-game). This property is simply an override
         * mechanic which aims to give the end-developer more control over the resulting name instead of being forced to rely on
         * the aforementioned algorithm.
         * <br></br>
         * The algorithm in question, in a nutshell, works as follows (the code block below is purely demonstrative of the
         * localization process and has nothing to do with how the algorithm is actually written):
         * <pre>
         *     {@code
         *      public class AlgorithmExampleDescriptor {
         *
         *          public static void main(String[] args) {
         *              // Input
         *              String unlocalizedName = "enchantment.mymodid.my_enchantment"; // The registry name/initial unlocalized name
         *
         *              // Steps
         *              AlgorithmLanguageProvider.validateNullity(unlocalizedName); // Checks whether the provided 'unlocalizedName' is empty/all whitespaces/you get the point
         *              AlgorithmLanguageProvider.validateRegex(unlocalizedName); // Checks whether the provided 'unlocalizedName' has the signature registry name separator character "."
         *              AlgorithmLanguageProvider.formatCaps(unlocalizedName); // Output: "Enchantment.Mymodid.My_Enchantment" <-- Capitalizes the first letter of each word based on regex-checks for special separators ("." and "_") (First character all the way to the left is always capitalized (duh), not that it matters)
         *              AlgorithmLanguageProvider.formatSeparators(unlocalizedName); // Output: "Enchantment.Mymodid.My_Enchantment" <-- Any defined "separator" Strings are lowercased, see #withCustomSeparatorWords(List)
         *              AlgorithmLanguageProvider.formatSpecialSeparators(unlocalizedName); // Output: "My Enchantment" <-- All characters preceding the last "." are substringed/removed, and then any "_" characters are replaced with whitespaces
         *
         *              // End result
         *              System.out.println(unlocalizedName); // Output: "My Enchantment"
         *          }
         *      }
         *     }
         * </pre>
         *
         * @param manuallyLocalizedEnchantmentName The name override used to localize the parent
         * {@linkplain Enchantment Enchantment's} registry name.
         *
         * @return {@code this} (builder method).
         *
         * @see #withCustomSeparatorWords(List)
         * @see #withLocalization(Function)
         * @see #literalTranslation(boolean)
         * @see #bypassDefaultTranslation(boolean)
         */
        public EPWBuilder withCustomName(String manuallyLocalizedEnchantmentName) {
            this.manuallyLocalizedEnchantmentName = manuallyLocalizedEnchantmentName;
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
         * @see #withCustomName(String)
         * @see #withLocalization(Function)
         * @see #literalTranslation()
         * @see #bypassDefaultTranslation(boolean)
         */
        public EPWBuilder literalTranslation(boolean literalTranslation) {
            this.literalTranslation = literalTranslation;
            return this;
        }

        /**
         * A custom {@link Function} to apply miscellaneous modifications to the resulting localized block name. This is
         * influenced by {@link #withCustomName(String)} and {@link #literalTranslation(boolean)}, where applicable.
         *
         * @param enchantmentTranslationFunc The {@link Function} responsible for directly modifying the resulting
         *                                   localized enchantment name.
         *
         * @return {@code this} (builder method).
         *
         * @see #withCustomName(String)
         * @see #literalTranslation(boolean)
         */
        public EPWBuilder withLocalization(Function<String, String> enchantmentTranslationFunc) {
            this.enchantmentTranslationFunc = enchantmentTranslationFunc;
            return this;
        }

        /**
         * Overloaded variant of {@link #literalTranslation(boolean)} which marks this builder as using literal translations.
         *
         * @return {@code this} (builder method).
         *
         * @see #literalTranslation(boolean)
         */
        public EPWBuilder literalTranslation() {
            return literalTranslation(true);
        }

        /**
         * Whether this EPWBuilder instance should skip the translation process altogether.
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
        public EPWBuilder bypassDefaultTranslation(boolean bypassDefaultTranslation) {
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
        public EPWBuilder bypassDefaultTranslation() {
            return bypassDefaultTranslation(true);
        }

        /**
         * Assigns a {@link List} of custom separator words which are lowercased during the algorithm's de-localization
         * process. This is ignored if {@link #manuallyLocalizedEnchantmentName} is defined, {@link #literalTranslation}
         * is {@code true}, or if {@link #enchantmentTranslationFunc} is non-null.
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
         */
        public EPWBuilder withCustomSeparatorWords(List<String> definedSeparatorWords) {
            this.definedSeparatorWords = definedSeparatorWords;
            return this;
        }

        /**
         * Tags this EPWBuilder's parent {@link Enchantment} with the provided {@link TagKey<Enchantment>}.
         *
         * @param parentEnchantmentTag The {@code TagKey<Enchantment>} with which this IPW's parent {@link Enchantment}
         *                             will be tagged. May only be of type {@link Enchantment}.
         *
         * @return {@code this} (builder method).
         */
        public EPWBuilder withTag(Supplier<TagKey<Enchantment>> parentEnchantmentTag) {
            this.parentTags.add(parentEnchantmentTag);
            return this;
        }

        /**
         * Tags this EPWBuilder's parent {@link Enchantment} with the provided {@linkplain TagKey<Enchantment> Tags}.
         * Appends to the existing list.
         *
         * @param parentEnchantmentTags The {@linkplain TagKey<Enchantment> TagKeys} with which this IPW's parent {@link Enchantment}
         *                              will be tagged. May only be of type {@link Enchantment}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withSetTags(List)
         */
        public EPWBuilder withTags(List<Supplier<TagKey<Enchantment>>> parentEnchantmentTags) {
            this.parentTags.addAll(parentEnchantmentTags);
            return this;
        }

        /**
         * Tags this EPWBuilder's parent Enchantment with the provided {@linkplain TagKey<Enchantment> Tags}. Overwrites
         * the existing {@link List}.
         *
         * @param parentEnchantmentTags The {@linkplain TagKey<Enchantment> TagKeys} with which this IPW's parent
         *                              {@link Enchantment} will be tagged. May only be of type {@link Enchantment}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withTags(List)
         */
        public EPWBuilder withSetTags(List<Supplier<TagKey<Enchantment>>> parentEnchantmentTags) {
            this.parentTags.clear();
            this.parentTags.addAll(parentEnchantmentTags);
            return this;
        }

        /**
         * Determines whether this EPWBuilder instance should be entirely excluded from Nexus' native datagen.
         * <br></br>
         * Fundamentally, all this does is flag this instance as not needing a data entry to be mapped to it. You may
         * choose to generate data for it yourself if needed, since Nexus won't handle datagen for this particular object.
         * <br></br>
         * If a block-specific data provider has {@link ModDataProvider#validateAllEntries()} set to {@code true}, this
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
        public EPWBuilder excludeFromNativeDatagen(boolean excludeFromNativeDatagen) {
            this.excludeFromNativeDatagen = excludeFromNativeDatagen;
            return this;
        }

        /**
         * Determines whether this EPWBuilder instance is required to generate necessary block-related data based on the
         * {@link ProviderType} passed in.
         * <br></br>
         * By default, unmapped providers will not require an entry for this EPWBuilder to be generated unless
         * {@link ModDataProvider#validateAllEntries()} is set to {@code true}.
         * <br></br>
         * Mapping the related provider passed in here to {@code requiresDatagenEntry}, set to {@code true}, will flag
         * this EPWBuilder instance for requiring related data regardless of what
         * {@link ModDataProvider#validateAllEntries()} is set to.
         *
         * @param targetProviderType The {@link ProviderType} to modify the data entry requirement for.
         * @param requiresDatagenEntry Whether this EPWBuilder should require data related to the specified
         *                             {@code targetProviderType} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public EPWBuilder requiresDatagenEntry(ProviderType targetProviderType, boolean requiresDatagenEntry) {
            mappedProviderRequisites.put(targetProviderType, requiresDatagenEntry);
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}.
         *
         * @param targetProviderTypes The {@link List} of {@linkplain ProviderType ProviderTypes} to modify the data
         *                            entry requirements for.
         * @param requiresDatagenEntry Whether this EPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public EPWBuilder requiresDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
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
         * @param requiresDatagenEntry Whether this EPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public EPWBuilder requiresSetDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
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
        public EPWBuilder requiresSetDatagenEntries(Map<ProviderType, Boolean> mappedProviderRequisites) {
            this.mappedProviderRequisites.clear();
            this.mappedProviderRequisites.putAll(mappedProviderRequisites);
            return this;
        }

        /**
         * Builds a new {@link EnchantmentPropertyWrapper} using this builder's data. Also maps the owner
         * {@link EnchantmentPropertyWrapper} to the parent {@link Enchantment} if the owner is not a template.
         *
         * @return The newly data-populated {@link EnchantmentPropertyWrapper}.
         *
         * @see EnchantmentPropertyWrapper#isTemplate()
         */
        public EnchantmentPropertyWrapper build() {
            if (!ownerWrapper.isTemplate) MAPPED_EPWS.putIfAbsent(ownerWrapper.enchantmentRegName == null ? ownerWrapper.parentEnchantment : () -> BuiltInRegistries.ENCHANTMENT.get(ownerWrapper.enchantmentRegName), ownerWrapper);
            return ownerWrapper;
        }
    }
}
