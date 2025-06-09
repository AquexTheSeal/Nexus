package com.mememan.nexus.datagen;

import com.mememan.nexus.loader.ModSide;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface ProviderType {

    /**
     *
     * @return
     */
    @NotNull
    ModSide getSide();
}
