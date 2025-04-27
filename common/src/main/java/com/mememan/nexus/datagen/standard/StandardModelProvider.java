package com.mememan.nexus.datagen.standard;

import net.minecraft.data.PackOutput;
import net.minecraft.data.models.ModelProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Base {@code class} for all standard modded model providers (block + item models, blockstates).
 */
public abstract class StandardModelProvider extends ModelProvider implements ModDataProvider {
    protected final String modId;

    public StandardModelProvider(PackOutput targetPackOutput, String modId) {
        super(targetPackOutput);
        this.modId = modId;
    }



    @Override
    public @NotNull String getModId() {
        return modId;
    }
}
