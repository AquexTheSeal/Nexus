package com.mememan.nexus.template;

import com.mememan.nexus.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class MyPacket { // Ensure that you properly segregate side-specific objects. If you need a reference to Minecraft, for instance, use ClientUtil#getClient() or your own utility method in a class that isn't initialized on both sides
    private final int someInt;
    private final String someString;

    public MyPacket(int someInt, String someString) {
        this.someInt = someInt;
        this.someString = someString;
    }

    public MyPacket(FriendlyByteBuf buf) { // ALT: You can use this overloaded constructor for decoding instead
        this(buf.readInt(), buf.readUtf());
    }

    public static MyPacket decode(FriendlyByteBuf buf) {
        return new MyPacket(buf.readInt(), buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.someInt);
        buf.writeUtf(this.someString);
    }

    public static PacketContext handle(MyPacket myPacketObj) {
        return (nullablePlayerOwner, currentLevel, currentConnection, currentSide) -> {
            // ... (Do stuff)
        };
    }
}
