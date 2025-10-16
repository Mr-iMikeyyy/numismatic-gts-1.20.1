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

import com.glisco.numismaticoverhaul.ModComponents;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import madmike.numismaticgts.NumismaticGTSComponents;
import madmike.numismaticgts.NumismaticGTSConfig;
import madmike.numismaticgts.components.scoreboard.PlayerNamesComponent;
import madmike.numismaticgts.data.Offer;
import madmike.numismaticgts.util.CurrencyUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                            
                            §e/trade upgrade §7- Increase the max slots available for you to sell
                            §e/trade profile §7- View your seller profile
                            §e/trade top §7- View the top performing sellers
                            §e/trade sell <gold> <silver> <bronze> §7- Sell the item you are holding
                            """), false);
                }
                return 1;
            });

            tradeCommand.then(literal("upgrade").executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) {
                    ctx.getSource().sendError(Text.literal("Only players can use this command."));
                    return 0;
                }

                // If your component class is still named UnlockedStoreSlotsComponent, swap the type below.
                var unlockedSlotsComponent = NumismaticGTSComponents.STORE_SLOTS.get(player);
                int unlockedSlots = unlockedSlotsComponent.getUnlockedSlots();

                if (unlockedSlots >= NumismaticGTSConfig.maxStoreSlotsPerPlayer) {
                    player.sendMessage(Text.literal("You’ve reached the maximum number of unlocked slots.").formatted(Formatting.GRAY), false);
                    return 0;
                }

                var wallet = ModComponents.CURRENCY.get(player);
                int cost = (unlockedSlots + 1) * 10_000; // 1 gold = 10,000 bronze

                if (wallet.getValue() >= cost) {
                    wallet.modify(-cost);
                    unlockedSlotsComponent.increment(1);
                    player.sendMessage(Text.literal("Upgraded your available sell slots by 1! It is now " + (unlockedSlots + 1)).formatted(Formatting.GOLD), false);
                } else {
                    var needed = CurrencyUtil.fromTotalBronze(cost);
                    player.sendMessage(
                            Text.literal("Not enough funds to upgrade. You need G: " + needed.gold()
                                    + ", S: " + needed.silver()
                                    + ", B: " + needed.bronze()).formatted(Formatting.RED),
                            false
                    );
                }
                return 1;
            }));

            tradeCommand.then(literal("profile").executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) {
                    ctx.getSource().sendError(Text.literal("Only players can use this command."));
                    return 0;
                }

                long total = NumismaticGTSComponents.TOTAL_SALES
                        .get(ctx.getSource().getServer().getScoreboard())
                        .getSales(player.getUuid());

                var coins = CurrencyUtil.fromTotalBronze(total);
                player.sendMessage(Text.literal(
                        "Your total sales are: G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze() + "."
                ), false);

                return 1;
            }));

            tradeCommand.then(literal("top").executes(ctx -> {
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
            }));

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
        if (price <= 0) {
            player.sendMessage(Text.literal("Price needs to be larger than 0").formatted(Formatting.RED), false);
            return 0;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("You're not holding any item to sell.").formatted(Formatting.RED), false);
            return 0;
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
        player.getMainHandStack().setCount(0);

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
