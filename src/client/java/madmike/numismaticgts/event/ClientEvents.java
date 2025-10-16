package madmike.numismaticgts.event;

import madmike.numismaticgts.net.packets.ClientReadyPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import static madmike.numismaticgts.net.TradePacketIds.CLIENT_READY;

public class ClientEvents {
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientReadyPacket.send();
        });
    }
}
