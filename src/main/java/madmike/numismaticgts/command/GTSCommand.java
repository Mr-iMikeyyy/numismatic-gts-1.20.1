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

package madmike.numismaticgts.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import madmike.cc.logic.BusyPlayers;
import madmike.numismaticgts.command.exe.StatsExe;
import madmike.numismaticgts.command.exe.TopExe;
import madmike.numismaticgts.command.exe.UpgradeExe;
import madmike.numismaticgts.components.NumismaticGTSComponents;
import madmike.numismaticgts.data.Offer;
import madmike.numismaticgts.util.CurrencyUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.Collections;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GTSCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> tradeCommand = literal("trade").executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendMessage(Text.literal("""
                            §6====== Trade Command Help ======
                            
                            §e/trade upgrade §7- Increase the max slots and price available for you to sell
                            §e/trade stats §7- View your seller stats
                            §e/trade top §7- View the top performing sellers
                            §e/trade sell <gold> <silver> <bronze> §7- Sell the item you are holding
                            """), false);
                }
                return 1;
            });

            tradeCommand.then(literal("upgrade")
                    .executes(UpgradeExe::execute)
            );

            tradeCommand.then(literal("stats")
                    .executes(StatsExe::execute)
            );

            tradeCommand.then(literal("top")
                    .executes(TopExe::execute)
            );

            tradeCommand.then(literal("sell")
                    .then(argument("gold", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) {
                                    ctx.getSource().sendError(Text.literal("Only players can use this command"));
                                    return 0;
                                }
                                int gold = IntegerArgumentType.getInteger(ctx, "gold");
                                long price = CurrencyUtil.toTotalBronze(gold, 0, 0);
                                return handleSellCommand(player, price, ctx.getSource().getServer());
                            })
                            .then(argument("silver", IntegerArgumentType.integer(0))
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                        if (player == null) {
                                            ctx.getSource().sendError(Text.literal("Only players can use this command"));
                                            return 0;
                                        }
                                        int gold = IntegerArgumentType.getInteger(ctx, "gold");
                                        int silver = IntegerArgumentType.getInteger(ctx, "silver");
                                        long price = CurrencyUtil.toTotalBronze(gold, silver, 0);
                                        return handleSellCommand(player, price, ctx.getSource().getServer());
                                    })
                                    .then(argument("bronze", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                if (player == null) {
                                                    ctx.getSource().sendError(Text.literal("Only players can use this command"));
                                                    return 0;
                                                }
                                                int gold = IntegerArgumentType.getInteger(ctx, "gold");
                                                int silver = IntegerArgumentType.getInteger(ctx, "silver");
                                                int bronze = IntegerArgumentType.getInteger(ctx, "bronze");
                                                long price = CurrencyUtil.toTotalBronze(gold, silver, bronze);
                                                return handleSellCommand(player, price, ctx.getSource().getServer());
                                            })
                                    )
                            )
                    )
            );

            dispatcher.register(tradeCommand);
        });
    }

    public static int handleSellCommand(ServerPlayerEntity player, long price, MinecraftServer server) {

        if (BusyPlayers.isBusy(player.getUuid())) {
            player.sendMessage(Text.literal("You are busy doing something").formatted(Formatting.RED));
            return 0;
        }

        if (price <= 0) {
            player.sendMessage(Text.literal("Price needs to be larger than 0").formatted(Formatting.RED), false);
            return 0;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("You're not holding any item to sell.").formatted(Formatting.RED), false);
            return 0;
        }

        if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
            if (stack.getItem() instanceof TravelersBackpackItem) {
                player.sendMessage(Text.literal("Not allowed to sell back packs.").formatted(Formatting.RED), false);
                return 0;
            }
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ShulkerBoxBlock) {
                player.sendMessage(Text.literal("Not allowed to sell shulker boxes.").formatted(Formatting.RED), false);
                return 0;
            }
        }

        var offersComp = NumismaticGTSComponents.OFFERS.get(server.getScoreboard());

        // Count used slots for this player with the new map shape (sellerId -> List<Offer>)
        long usedSlots = offersComp.getOffers()
                .getOrDefault(player.getUuid(), Collections.emptyList())
                .size();

        int unlocked = NumismaticGTSComponents.STORE_SLOTS.get(player).getUnlockedSlots();

        if (unlocked <= usedSlots) {
            player.sendMessage(Text.literal("You don't have any available sell slots left.").formatted(Formatting.RED), false);
            return 0;
        }

        // Remove the held stack from the player's hand and list it
        ItemStack listedItem = stack.copy();
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);

        Offer offer = new Offer(
                UUID.randomUUID(),
                player.getUuid(),
                listedItem,
                price
        );

        offersComp.addOffer(offer);

        var bd = CurrencyUtil.fromTotalBronze(price);
        player.sendMessage(Text.literal(String.format(
                "Listed item for %d gold, %d silver, %d bronze.",
                bd.gold(), bd.silver(), bd.bronze()
        )).formatted(Formatting.GOLD), false);

        return 1;
    }
}
