package com.mememan.nexus.template;

import com.mememan.nexus.asm.annotations.DatagenRegistrarEntry;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.ModDatagenConfig;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.platform.NexusServices;

import java.util.Map;
import java.util.Set;

@DatagenRegistrarEntry
public class TestDatagenRegistrar {
    public static final ModDatagenConfig NEXUS = NexusServices.DATA_GENERATOR.registerConfigForMod(new ModDatagenConfig("nexus", true, Map.of(NexusProviderTypes.RECIPE_PROVIDER, DuplicateDataPolicy.EXCLUDE_WARN, NexusProviderTypes.LANGUAGE_PROVIDER, DuplicateDataPolicy.OVERRIDE_WARN), Set.of(), Set.of()));
}
