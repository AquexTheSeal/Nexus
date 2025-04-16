package com.mememan.nexus.client.general;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

public class ClientDataEntry {
    private static final ObjectArrayList<ClientDataEntry> MAPPED_CDES = new ObjectArrayList<>();

    private ClientDataEntry() {

    }

    public static class CDEBuilder {
        @NotNull
        private final ClientDataEntry ownerEntry;

        private CDEBuilder(@NotNull ClientDataEntry ownerEntry) {
            this.ownerEntry = ownerEntry;
        }

        public ClientDataEntry build() {
            return ownerEntry;
        }
    }
}
