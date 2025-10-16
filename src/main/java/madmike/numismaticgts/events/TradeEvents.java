/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package madmike.numismaticgts.events;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import madmike.numismaticgts.NumismaticGTSComponents;
import madmike.numismaticgts.components.scoreboard.OfflineSalesComponent;
import madmike.numismaticgts.util.CurrencyUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static madmike.numismaticgts.net.TradePacketIds.CLIENT_READY;

public class TradeEvents {
    public static void onClientReady(ServerPlayerEntity player, MinecraftServer server) {
        UUID playerId = player.getUuid();

        String name = player.getGameProfile().getName();
        if (name == null || name.isEmpty()) name = player.getName().getString(); // fallback to display name
        NumismaticGTSComponents.PLAYER_NAMES.get(server.getScoreboard()).put(playerId, name);

        OfflineSalesComponent offlineSales = NumismaticGTSComponents.OFFLINE_SALES.get(server.getScoreboard());
        long totalProfit = offlineSales.getSales(playerId); // now a single long

        if (totalProfit > 0) {
            // Credit the player's wallet
            CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
            wallet.modify(totalProfit);

            // Notify player
            String coinsStr = CurrencyUtil.formatPrice(totalProfit).getString();
            player.sendMessage(
                    Text.literal("You made " + coinsStr + " while you were away!").formatted(Formatting.GOLD),
                    false
            );

            // Clear the stored offline sales total
            offlineSales.clearSales(playerId);
        }
    }
}
