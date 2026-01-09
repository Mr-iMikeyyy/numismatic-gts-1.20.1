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
import madmike.numismaticgts.util.CurrencyUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class StatsExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Only players can use this command."));
            return 0;
        }

        long total = NumismaticGTSComponents.TOTAL_SALES
                .get(ctx.getSource().getServer().getScoreboard())
                .getSales(player.getUuid());

        var coins = CurrencyUtil.fromTotalBronze(total);
        player.sendMessage(Text.literal("Your total sales are: G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze() + "."));

        return 1;
    }
}
