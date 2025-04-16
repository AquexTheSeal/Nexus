package com.mememan.nexus.datagen;

import net.minecraft.data.DataProvider;

/**
 * The core {@code class} responsible for handling the creation of all types of data providers (native and custom).
 * <br></br>
 * All default provider types functionally inherit and expose the relevant data/methods required to modify them to some
 * degree. Custom data providers can be defined if needed (see the references below).
 */
public final class CentralDataGenerator<DP extends DataProvider> {
    public static final CentralDataGenerator<? extends DataProvider> INSTANCE = new CentralDataGenerator<>();

    private CentralDataGenerator() {

    }


}
