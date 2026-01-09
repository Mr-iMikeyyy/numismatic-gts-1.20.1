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

package madmike.numismaticgts.command.exe;

import com.glisco.numismaticoverhaul.ModComponents;
import com.mojang.brigadier.context.CommandContext;
import madmike.config.NumismaticGTSConfig;
import madmike.numismaticgts.components.NumismaticGTSComponents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UpgradeExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Only players can use this command."));
            return 0;
        }

        var unlockedSlotsComponent = NumismaticGTSComponents.STORE_SLOTS.get(player);
        int unlockedSlots = unlockedSlotsComponent.getUnlockedSlots();

        if (unlockedSlots >= NumismaticGTSConfig.maxStoreSlotsPerPlayer) {
            player.sendMessage(Text.literal("You’ve reached the maximum number of unlocked slots.").formatted(Formatting.GRAY));
            return 0;
        }

        var wallet = ModComponents.CURRENCY.get(player);
        int cost = (unlockedSlots + 1) * 10_000; // 1 gold = 10,000 bronze

        if (wallet.getValue() >= cost) {
            wallet.modify(-cost);
            unlockedSlotsComponent.increment(1);
            player.sendMessage(Text.literal("Upgraded your available sell slots by 1! It is now " + (unlockedSlots + 1)).formatted(Formatting.GOLD));
        } else {
            player.sendMessage(Text.literal("Not enough funds to upgrade. You need G: " + unlockedSlots + 1));
        }
        return 1;
    }
}
