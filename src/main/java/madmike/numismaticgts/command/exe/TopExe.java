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

import com.mojang.brigadier.context.CommandContext;
import madmike.numismaticgts.components.NumismaticGTSComponents;
import madmike.numismaticgts.components.scoreboard.PlayerNamesComponent;
import madmike.numismaticgts.util.CurrencyUtil;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Only players can use this command"));
            return 0;
        }

        Scoreboard sb = player.getScoreboard();

        var salesComp = NumismaticGTSComponents.TOTAL_SALES.get(sb);

        List<Map.Entry<UUID, Long>> top = salesComp.getAllSales().entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(10)
                .toList();

        if (top.isEmpty()) {
            player.sendMessage(Text.literal("No sellers found."), false);
            return 1;
        }

        PlayerNamesComponent pnc = NumismaticGTSComponents.PLAYER_NAMES.get(sb);

        player.sendMessage(Text.literal("Top 10 Sellers:"), false);
        for (int i = 0; i < top.size(); i++) {
            var entry = top.get(i);
            String name = pnc.resolve(entry.getKey());
            String priceStr = CurrencyUtil.formatPrice(entry.getValue()).getString();
            String line = String.format("%d. %s - %s", i + 1, name, priceStr);
            player.sendMessage(Text.literal(line), false);
        }
        return 1;
    }
}
