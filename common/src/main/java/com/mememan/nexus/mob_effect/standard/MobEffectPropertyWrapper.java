package com.mememan.nexus.mob_effect.standard;

import com.google.common.collect.ImmutableMap;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.Nullable;

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

        public MEPWBuilder(MobEffectPropertyWrapper ownerWrapper, Supplier<MobEffect> mobEffectParent) {
            this.ownerWrapper = ownerWrapper;
            this.mobEffectParent = mobEffectParent;
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
