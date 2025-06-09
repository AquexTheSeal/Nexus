package com.mememan.nexus.datagen;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Basic data-holding {@code record} used to store information regarding the general parameters under which data generation
 * should take place for a given mod by its mod ID. Only useful for datagen.
 *
 * @param modId The parent mod ID under which the specified data should be configured/stored.
 * @param enableDatagen Whether Nexus API should generate any data for the specified mod ID. If disabled, both native
 *                      Nexus datagen and providers added through Nexus API under the specified modId are disabled.
 * @param providerTypesToFullyValidate A {@link Set} of {@linkplain ProviderType ProviderTypes} for which all entries under
 *                                     the parent mod ID should be validated (i.e. checked for presence).
 * @param disabledProviderTypes A {@link Set} of {@linkplain ProviderType ProviderTypes} to be excluded by Nexus API
 *                              from datagen.
 */
public record ModDatagenConfig(@NotNull String modId, boolean enableDatagen, Set<ProviderType> providerTypesToFullyValidate, Set<ProviderType> disabledProviderTypes) {
}
