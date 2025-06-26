package com.mememan.nexus.mob_effect.standard;

import com.google.common.collect.ImmutableMap;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class MobEffectPropertyWrapper {
    private static final Object2ObjectLinkedOpenHashMap<Supplier<MobEffect>, MobEffectPropertyWrapper> MAPPED_MEPWS = new Object2ObjectLinkedOpenHashMap<>();
    @Nullable
    private final ResourceLocation mobEffectRegName;
    private final Supplier<MobEffect> parentMobEffect;
    private final boolean isTemplate;
    @Nullable
    private MEPWBuilder builder;

    private MobEffectPropertyWrapper(@Nullable ResourceLocation mobEffectRegName, Supplier<MobEffect> parentMobEffect) {
        this.mobEffectRegName = mobEffectRegName;
        this.parentMobEffect = parentMobEffect;
        this.isTemplate = false;
    }

    private MobEffectPropertyWrapper(Supplier<MobEffect> parentMobEffect) {
        this(null, parentMobEffect);
    }

    private MobEffectPropertyWrapper() {
        this.mobEffectRegName = null;
        this.parentMobEffect = null;
        this.isTemplate = true; // Otherwise can't set with constructor overloading (Laziness:tm:)
    }

    /**
     * Creates a new {@link MobEffectPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to directly create an MEPW during a registration call rather
     * than after it/without storing it.
     *
     * @param mobEffectRegName The registry name of the {@code parentMobEffect}. Used when access to
     *                           the {@code parentMobEffect} returns an air delegate (I.E. It's too early to access
     *                           the parent mob effect, see {@link BuiltInRegistries#MOB_EFFECT}).
     * @param parentMobEffect The parent {@code Supplier<MobEffect>} stored in the newly-initialized MEPW instance.
     *
     * @return A new {@link MobEffectPropertyWrapper} instance.
     *
     * @apiNote You shouldn't have to use this method for 95% of cases since you'd be inlining a registration call for
     * {@link #parentMobEffect}, see {@link #create(Supplier)} instead.
     *
     * @see #create(Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(MobEffectPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     */
    public static MobEffectPropertyWrapper create(ResourceLocation mobEffectRegName, Supplier<MobEffect> parentMobEffect) {
        return new MobEffectPropertyWrapper(mobEffectRegName, parentMobEffect);
    }

    /**
     * Creates a new {@link MobEffectPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to create an MEPW instance with a stored registration call,
     * such that its parent {@code Supplier<MobEffect>} is not a luck effect delegate (see {@link BuiltInRegistries#MOB_EFFECT}).
     *
     * @param parentMobEffect The parent {@code Supplier<MobEffect>} stored in the newly-initialized MEPW instance.
     *
     * @return A new {@link MobEffectPropertyWrapper} instance.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(MobEffectPropertyWrapper, Supplier)
     */
    public static MobEffectPropertyWrapper create(Supplier<MobEffect> parentMobEffect) {
        return new MobEffectPropertyWrapper(parentMobEffect);
    }

    /**
     * Creates a new {@link MobEffectPropertyWrapper} instance as a template. Template MEPWs are not stored in
     * {@link #MAPPED_MEPWS} and do not store a parent {@link MobEffect}. They're particularly useful for re-using across
     * multiple {@linkplain MobEffect MobEffects}.
     *
     * @return A new {@link MobEffectPropertyWrapper} instance, set as a template.
     *
     * @see #of(MobEffectPropertyWrapper, Supplier)
     * @see #isTemplate()
     * @see #ofTemplate(MobEffectPropertyWrapper)
     */
    public static MobEffectPropertyWrapper createTemplate() {
        return new MobEffectPropertyWrapper();
    }

    /**
     * Creates a new {@link MobEffectPropertyWrapper} instance as a template, inheriting data from the provided MEPW
     * template. Template MEPWs are not stored in {@link #MAPPED_MEPWS} and do not store a parent {@link MobEffect}.
     * They're particularly useful for re-using across multiple {@linkplain MobEffect MobEffects}.
     *
     * @param parentTemplateWrapper The parent {@link MobEffectPropertyWrapper} template from which {@link #builder()}
     *                              data should be copied.
     *
     * @return A new {@link ItemPropertyWrapper} instance, set as a template, inheriting from the provided MEPW template.
     * If the provided MEPW template is {@code null}, returns {@link #createTemplate()}.
     *
     * @see #createTemplate()
     * @see #isTemplate()
     */
    public static MobEffectPropertyWrapper ofTemplate(MobEffectPropertyWrapper parentTemplateWrapper) {
        if (parentTemplateWrapper != null) {
            MobEffectPropertyWrapper newTemplateWrapper = new MobEffectPropertyWrapper();

            return copyProperties(parentTemplateWrapper, newTemplateWrapper);
        } else return createTemplate();
    }

    /**
     * Creates a new {@link MobEffectPropertyWrapper} instance based on the provided {@link MobEffectPropertyWrapper}.
     * If the provided MEPW instance is {@code null}, returns {@link #create(Supplier)}. You'd typically use this if you
     * have an MEPW template you want multiple registered {@linkplain MobEffect MobEffects} to inherit from.
     *
     * @param parentWrapper The parent {@link MobEffectPropertyWrapper} instance from which {@link #builder()} should
     *                      be copied.
     * @param newMobEffect The new registry entry to use for the newly constructed MEPW instance.
     *
     * @return A new {@link MobEffectPropertyWrapper} instance with copied properties based on the provided MEPW, or an
     * entirely new/clean instance if the provided MEPW is {@code null}.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #createTemplate()
     */
    public static MobEffectPropertyWrapper of(MobEffectPropertyWrapper parentWrapper, Supplier<MobEffect> newMobEffect) {
        if (parentWrapper != null) {
            MobEffectPropertyWrapper newWrapper = new MobEffectPropertyWrapper(newMobEffect);

            return copyProperties(parentWrapper, newWrapper);
        } else return create(newMobEffect);
    }

    /**
     * Creates a new {@link MobEffectPropertyWrapper} instance from an existing {@link MobEffectPropertyWrapper}
     * instance based on the provided {@code Supplier<MobEffect>}. If no such existing MEPW instance exists, returns
     * {@link #create(Supplier)}.
     *
     * @param parentMobEffect The parent {@code Supplier<MobEffect>} stored in {@link #MAPPED_MEPWS}. Copies its MEPW
     *                        instance's {@link MEPWBuilder} properties if it exists, or creates a clean new MEPW instance
     *                        if it doesn't.
     * @param newMobEffect The new registry entry to use for the newly constructed MEPW instance.
     *
     * @return A new {@link MobEffectPropertyWrapper} instance with copied properties based on the provided
     * {@code Supplier<MobEffect>}, or an entirely new/clean instance if no such MEPW exists.
     *
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(MobEffectPropertyWrapper, Supplier)
     */
    public static MobEffectPropertyWrapper of(Supplier<MobEffect> parentMobEffect, Supplier<MobEffect> newMobEffect) {
        if (MAPPED_MEPWS.containsKey(parentMobEffect)) {
            MobEffectPropertyWrapper originalWrapper = MAPPED_MEPWS.get(parentMobEffect);
            MobEffectPropertyWrapper newWrapper = new MobEffectPropertyWrapper(newMobEffect);

            return copyProperties(originalWrapper, newWrapper);
        } else return create(newMobEffect);
    }

    /**
     * Overloaded variant of {@link #of(Supplier, Supplier)} that copies the parent mob effect's
     * {@linkplain MobEffect#getCategory() category} and {@linkplain MobEffect#getColor() color}, as well as the
     * {@linkplain MobEffect#getAttributeModifiers() attribute modifiers}.
     *
     * @param newMobEffectRegName The new registry name by which the newly constructed {@code Supplier<MobEffect>} instance
     *                            will be stored.
     * @param parentMobEffect The parent {@code Supplier<MobEffect>} stored in {@link #MAPPED_MEPWS}. Must already be
     *                        a registered {@link MobEffect}.
     *
     * @return A new {@link MobEffectPropertyWrapper} instance with copied properties (rarity, slots, category)
     * based on the provided {@code Supplier<MobEffect>}, or an entirely new/clean instance if no such MEPW exists.
     *
     * @see #of(MobEffectPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     */
    public static MobEffectPropertyWrapper of(ResourceLocation newMobEffectRegName, Supplier<MobEffect> parentMobEffect) {
        MobEffect parentAsMobEffect = parentMobEffect.get();
        MobEffect newEffect = new MobEffect(parentAsMobEffect.getCategory(), parentAsMobEffect.getColor());

        if (!parentAsMobEffect.getAttributeModifiers().isEmpty()) parentAsMobEffect.getAttributeModifiers().forEach(newEffect.getAttributeModifiers()::put);

        return of(parentMobEffect, NexusServices.REGISTRAR.registerObject(newMobEffectRegName, () -> new MobEffect(parentAsMobEffect.getCategory(), parentAsMobEffect.getColor()), BuiltInRegistries.MOB_EFFECT));
    }

    /**
     * Shortcut utility method centered around copying builder properties over from one MEPW instance to another.
     *
     * @param from The MEPW instance to copy properties from.
     * @param to The MEPW instance to copy properties to.
     *
     * @return The provided MEPW instance with copied properties.
     */
    public static MobEffectPropertyWrapper copyProperties(MobEffectPropertyWrapper from, MobEffectPropertyWrapper to) {
        if (from.builder == null) return to;
        return to.builder()
                .excludeFromNativeDatagen(from.builder.excludeFromNativeDatagen)
                .literalTranslation(from.builder.literalTranslation)
                .withCustomSeparatorWords(List.copyOf(from.builder.definedSeparatorWords))
                .withCustomName(from.builder.manuallyLocalizedMobEffectName)
                .withLocalization(from.builder.mobEffectTranslationFunc)
                .bypassDefaultTranslation(from.builder.bypassDefaultTranslation)
                .withSetTags(List.copyOf(from.builder.parentTags))
                .requiresSetDatagenEntries(Map.copyOf(from.builder.mappedProviderRequisites))
                .build(); // Direct setting of the builder would copy the entire object itself, which would in-turn overwrite it if any calls are made to the copied MEPW afterward
    }

    /**
     * Constructs a builder chain in which certain datagen properties can be assigned and re-built with in this
     * MobEffectPropertyWrapper instance. Also sets this MEPW instance's {@link #builder} to the newly-constructed
     * {@link MEPWBuilder} instance.
     * <br></br>
     * <b>NOTE: THIS WILL OVERRIDE {@link #builder} ENTIRELY EVEN IF IT'S NOT {@code null} (e.g. you're inheriting from
     * a template, see {@link #of(MobEffectPropertyWrapper, Supplier)}).</b>
     *
     * @return A new {@link MEPWBuilder} instance from the {@link #builder} field.
     *
     * @see #cachedBuilder()
     */
    public MEPWBuilder builder() {
        return this.builder = new MEPWBuilder(this, parentMobEffect);
    }

    /**
     * Gets the cached {@link MEPWBuilder} instance from the {@link #builder} if it exists. May be {@code null}. Useful
     * for overriding specific properties after having copied another MEPW instance/already set an MEPWBuilder.
     *
     * @return The cached {@link MEPWBuilder} instance, or {@code null} if the {@link #builder} is {@code null}.
     *
     * @see #of(ResourceLocation, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #builder()
     */
    @Nullable
    public MEPWBuilder cachedBuilder() {
        return builder;
    }

    /**
     * Gets the parent {@code Supplier<MobEffect>} of this MEPW instance.
     *
     * @return The parent {@code Supplier<MobEffect>} stored in this MEPW instance.
     */
    public Supplier<MobEffect> getParentMobEffect() {
        return parentMobEffect;
    }

    /**
     * Gets the manually localized mob effect name from the {@link #builder()} if the builder exists.
     *
     * @return The manually localized mob effect name, or an empty {@code String} if the {@link #builder()} is {@code null}.
     */
    public String getManuallyLocalizedMobEffectName() {
        return builder == null ? "" : builder.manuallyLocalizedMobEffectName;
    }

    /**
     * Gets whether this MEPW instance bypasses default translation corrections.
     *
     * @return Whether this MEPW instance bypasses default translation corrections.
     */
    public boolean hasLiteralTranslation() {
        return builder != null && builder.literalTranslation;
    }

    /**
     * Gets whether this MEPW instance bypasses default translation altogether.
     *
     * @return Whether this MEPW instance bypasses default translation altogether.
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
    public Function<String, String> getMobEffectTranslationFunc() {
        return builder == null ? null : builder.mobEffectTranslationFunc;
    }

    /**
     * Gets the defined parent {@linkplain Supplier<TagKey< MobEffect >> Tags} from the {@link #builder()} if the builder exists.
     *
     * @return The defined parent {@linkplain Supplier<TagKey<MobEffect>> Tags}, or an empty {@link ObjectArrayList} if the
     * {@link #builder()} is {@code null}.
     */
    public List<Supplier<TagKey<MobEffect>>> getParentMobEffectTags() {
        return builder == null ? ObjectArrayList.of() : builder.parentTags;
    }

    /**
     * Whether data present in {@link #builder()} (if not {@code null}) should automatically be handled/generated by
     * Nexus API.
     *
     * @return {@code true} if {@link #builder} isn't {@code null} and {@link MEPWBuilder#excludeFromNativeDatagen} is set
     * to {@code true}, {@code false} otherwise.
     */
    public boolean excludeFromNativeDatagen() {
        return builder != null && builder.excludeFromNativeDatagen;
    }

    /**
     * Gets a {@link Map} (usually {@link Object2BooleanOpenHashMap}) specifying the {@linkplain ProviderType ProviderTypes}
     * for which this MEPW instance requires data to present for generation. May be empty if {@link #builder} is {@code null}
     * or the underlying {@link Map} is also empty.
     *
     * @return The {@link Map} representing different {@linkplain ProviderType ProviderTypes} and their requirements for
     * datagen. May be empty.
     */
    public Map<ProviderType, Boolean> getProviderTypeRequisites() {
        return builder == null ? new Object2BooleanOpenHashMap<>() : builder.mappedProviderRequisites;
    }

    /**
     * Whether this MEPW instance is a template. Templates are not stored in {@link #getMappedMepws()} ()} and have no
     * parent {@link MobEffect}.
     *
     * @return Whether this MEPW instance is a template.
     *
     * @see #of(MobEffectPropertyWrapper, Supplier)
     * @see #createTemplate()
     */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
     * Gets an immutable view (via {@link ImmutableMap}) of {@link #MAPPED_MEPWS}.
     *
     * @return An immutable view (via {@link ImmutableMap}) of {@link #MAPPED_MEPWS}.
     */
    public static ImmutableMap<Supplier<MobEffect>, MobEffectPropertyWrapper> getMappedMepws() {
        return ImmutableMap.copyOf(MAPPED_MEPWS);
    }

    /**
     * A builder class used to construct certain mob effect-related data for datagen and other data related to hardcoded
     * mob effect settings, such as effect name.
     */
    public static class MEPWBuilder {
        private final MobEffectPropertyWrapper ownerWrapper;
        private final Supplier<MobEffect> mobEffectParent;
        private String manuallyLocalizedMobEffectName = "";
        private List<String> definedSeparatorWords = ObjectArrayList.of();
        private final List<Supplier<TagKey<MobEffect>>> parentTags = ObjectArrayList.of();
        @Nullable
        private Function<String, String> mobEffectTranslationFunc;
        private boolean literalTranslation = false;
        private boolean bypassDefaultTranslation = false;
        private boolean excludeFromNativeDatagen = false;
        private final Map<ProviderType, Boolean> mappedProviderRequisites = new Object2BooleanOpenHashMap<>();
        private final Map<Component, String> mobEffectTooltips = new Object2ObjectOpenHashMap<>();

        public MEPWBuilder(MobEffectPropertyWrapper ownerWrapper, Supplier<MobEffect> mobEffectParent) {
            this.ownerWrapper = ownerWrapper;
            this.mobEffectParent = mobEffectParent;
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
         *              String unlocalizedName = "effect.mymodid.my_mob_effect"; // The registry name/initial unlocalized name
         *
         *              // Steps
         *              AlgorithmLanguageProvider.validateNullity(unlocalizedName); // Checks whether the provided 'unlocalizedName' is empty/all whitespaces/you get the point
         *              AlgorithmLanguageProvider.validateRegex(unlocalizedName); // Checks whether the provided 'unlocalizedName' has the signature registry name separator character "."
         *              AlgorithmLanguageProvider.formatCaps(unlocalizedName); // Output: "Effect.Mymodid.My_Mob_Effect" <-- Capitalizes the first letter of each word based on regex-checks for special separators ("." and "_") (First character all the way to the left is always capitalized (duh), not that it matters)
         *              AlgorithmLanguageProvider.formatSeparators(unlocalizedName); // Output: "Effect.Mymodid.My_Mob_Effect" <-- Any defined "separator" Strings are lowercased, see #withCustomSeparatorWords(List)
         *              AlgorithmLanguageProvider.formatSpecialSeparators(unlocalizedName); // Output: "My Mob Effect" <-- All characters preceding the last "." are substringed/removed, and then any "_" characters are replaced with whitespaces
         *
         *              // End result
         *              System.out.println(unlocalizedName); // Output: "My Mob Effect"
         *          }
         *      }
         *     }
         * </pre>
         *
         * @param manuallyLocalizedMobEffectName The name override used to localize the parent
         * {@linkplain MobEffect Mob Effect's} registry name.
         *
         * @return {@code this} (builder method).
         *
         * @see #withCustomSeparatorWords(List)
         * @see #withLocalization(Function)
         * @see #literalTranslation(boolean)
         * @see #bypassDefaultTranslation(boolean)
         */
        public MEPWBuilder withCustomName(String manuallyLocalizedMobEffectName) {
            this.manuallyLocalizedMobEffectName = manuallyLocalizedMobEffectName;
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
        public MEPWBuilder literalTranslation(boolean literalTranslation) {
            this.literalTranslation = literalTranslation;
            return this;
        }

        /**
         * A custom {@link Function} to apply miscellaneous modifications to the resulting localized block name. This is
         * influenced by {@link #withCustomName(String)} and {@link #literalTranslation(boolean)}, where applicable.
         *
         * @param mobEffectTranslationFunc The {@link Function} responsible for directly modifying the resulting localized
         *                                 mob effect name.
         *
         * @return {@code this} (builder method).
         *
         * @see #withCustomName(String)
         * @see #literalTranslation(boolean)
         */
        public MEPWBuilder withLocalization(Function<String, String> mobEffectTranslationFunc) {
            this.mobEffectTranslationFunc = mobEffectTranslationFunc;
            return this;
        }

        /**
         * Overloaded variant of {@link #literalTranslation(boolean)} which marks this builder as using literal translations.
         *
         * @return {@code this} (builder method).
         *
         * @see #literalTranslation(boolean)
         */
        public MEPWBuilder literalTranslation() {
            return literalTranslation(true);
        }

        /**
         * Whether this MEPWBuilder instance should skip the translation process altogether.
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
        public MEPWBuilder bypassDefaultTranslation(boolean bypassDefaultTranslation) {
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
        public MEPWBuilder bypassDefaultTranslation() {
            return bypassDefaultTranslation(true);
        }

        /**
         * Assigns a {@link List} of custom separator words which are lowercased during the algorithm's de-localization
         * process. This is ignored if {@link #manuallyLocalizedMobEffectName} is defined, {@link #literalTranslation} is
         * {@code true}, or if {@link #mobEffectTranslationFunc} is non-null.
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
        public MEPWBuilder withCustomSeparatorWords(List<String> definedSeparatorWords) {
            this.definedSeparatorWords = definedSeparatorWords;
            return this;
        }

        /**
         * Tags this MEPWBuilder's parent {@link MobEffect} with the provided {@link TagKey<MobEffect>}.
         *
         * @param parentMobEffectTag The {@code TagKey<MobEffect>} with which this IPW's parent {@link MobEffect} will
         *                           be tagged. May only be of type {@link MobEffect}.
         *
         * @return {@code this} (builder method).
         */
        public MEPWBuilder withTag(Supplier<TagKey<MobEffect>> parentMobEffectTag) {
            this.parentTags.add(parentMobEffectTag);
            return this;
        }

        /**
         * Tags this MEPWBuilder's parent {@link MobEffect} with the provided {@linkplain TagKey<MobEffect> Tags}. Appends
         * to the existing list.
         *
         * @param parentMobEffectTags The {@linkplain TagKey<MobEffect> TagKeys} with which this IPW's parent {@link MobEffect}
         *                            will be tagged. May only be of type {@link MobEffect}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withSetTags(List)
         */
        public MEPWBuilder withTags(List<Supplier<TagKey<MobEffect>>> parentMobEffectTags) {
            this.parentTags.addAll(parentMobEffectTags);
            return this;
        }

        /**
         * Tags this MEPWBuilder's parent MobEffect with the provided {@linkplain TagKey<MobEffect> Tags}. Overwrites
         * the existing {@link List}.
         *
         * @param parentMobEffectTags The {@linkplain TagKey<MobEffect> TagKeys} with which this IPW's parent {@link MobEffect}
         *                            will be tagged. May only be of type {@link MobEffect}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withTags(List)
         */
        public MEPWBuilder withSetTags(List<Supplier<TagKey<MobEffect>>> parentMobEffectTags) {
            this.parentTags.clear();
            this.parentTags.addAll(parentMobEffectTags);
            return this;
        }

        /**
         * Determines whether this MEPWBuilder instance should be entirely excluded from Nexus' native datagen.
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
        public MEPWBuilder excludeFromNativeDatagen(boolean excludeFromNativeDatagen) {
            this.excludeFromNativeDatagen = excludeFromNativeDatagen;
            return this;
        }

        /**
         * Determines whether this MEPWBuilder instance is required to generate necessary block-related data based on the
         * {@link ProviderType} passed in.
         * <br></br>
         * By default, unmapped providers will not require an entry for this MEPWBuilder to be generated unless
         * {@link ModDataProvider#validateAllEntries()} is set to {@code true}.
         * <br></br>
         * Mapping the related provider passed in here to {@code requiresDatagenEntry}, set to {@code true}, will flag
         * this MEPWBuilder instance for requiring related data regardless of what
         * {@link ModDataProvider#validateAllEntries()} is set to.
         *
         * @param targetProviderType The {@link ProviderType} to modify the data entry requirement for.
         * @param requiresDatagenEntry Whether this MEPWBuilder should require data related to the specified
         *                             {@code targetProviderType} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public MEPWBuilder requiresDatagenEntry(ProviderType targetProviderType, boolean requiresDatagenEntry) {
            mappedProviderRequisites.put(targetProviderType, requiresDatagenEntry);
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}.
         *
         * @param targetProviderTypes The {@link List} of {@linkplain ProviderType ProviderTypes} to modify the data
         *                            entry requirements for.
         * @param requiresDatagenEntry Whether this MEPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public MEPWBuilder requiresDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
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
         * @param requiresDatagenEntry Whether this MEPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public MEPWBuilder requiresSetDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
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
        public MEPWBuilder requiresSetDatagenEntries(Map<ProviderType, Boolean> mappedProviderRequisites) {
            this.mappedProviderRequisites.clear();
            this.mappedProviderRequisites.putAll(mappedProviderRequisites);
            return this;
        }

        /**
         * Builds a new {@link MobEffectPropertyWrapper} using this builder's data. Also maps the owner
         * {@link MobEffectPropertyWrapper} to the parent {@link MobEffect} if the owner is not a template.
         *
         * @return The newly data-populated {@link MobEffectPropertyWrapper}.
         *
         * @see MobEffectPropertyWrapper#isTemplate()
         */
        public MobEffectPropertyWrapper build() {
            if (!ownerWrapper.isTemplate) MAPPED_MEPWS.putIfAbsent(ownerWrapper.mobEffectRegName == null ? ownerWrapper.parentMobEffect : () -> BuiltInRegistries.MOB_EFFECT.get(ownerWrapper.mobEffectRegName), ownerWrapper);
            return ownerWrapper;
        }
    }
}
