package madmike.numismaticgts.net.packets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import static madmike.numismaticgts.net.TradePacketIds.CLIENT_READY;

public class ClientReadyPacket {
    public static void send() {
        ClientPlayNetworking.send(CLIENT_READY, PacketByteBufs.empty());
    }
}
