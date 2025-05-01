package com.mememan.nexus.template;

import com.mememan.nexus.platform.NexusServices;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class SomeItem extends Item {

    public SomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) { // Validate that we're not sending a packet to the server from the server (duh)
            NexusServices.NETWORK_MANAGER.sendToServer(new MyPacket(2, "A string that signals something to the server or whatever"));
        }
        return super.useOn(ctx);
    }
}