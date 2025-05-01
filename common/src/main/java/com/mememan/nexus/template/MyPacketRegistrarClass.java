package com.mememan.nexus.template;

import com.mememan.nexus.asm.annotations.NetworkRegistrarEntry;
import com.mememan.nexus.network.BasePacket;
import com.mememan.nexus.network.NetworkSide;
import com.mememan.nexus.platform.NexusServices;
import net.minecraft.resources.ResourceLocation;

@NetworkRegistrarEntry // Optional; you can use bootstrap methods or some other way to statically initialize this class
public class MyPacketRegistrarClass {

    // NetworkSide here is pretty important, since registration/active method calls made to NetworkServices#NETWORK_MANAGER behave based on it
    public static final BasePacket<MyPacket> MY_PACKET = registerPacket(new BasePacket<>(new ResourceLocation("my_modid", "my_packet"), MyPacket.class, MyPacket::encode, MyPacket::decode, MyPacket::handle, NetworkSide.C2S));

    private static <MSGT> BasePacket<MSGT> registerPacket(BasePacket<MSGT> packet) {
        return NexusServices.NETWORK_MANAGER.registerPacket(packet); // Actually register the packet
    }
}