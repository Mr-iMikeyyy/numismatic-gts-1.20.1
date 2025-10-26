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

package madmike.numismaticgts.components.scoreboard;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import madmike.numismaticgts.NumismaticGTSComponents;
import madmike.numismaticgts.data.Offer;
import madmike.numismaticgts.net.packets.TradeScreenRefreshS2CSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;


public class OffersComponent implements ComponentV3, AutoSyncedComponent {

    /** sellerId -> list of offers from that seller */
    private final Map<UUID, List<Offer>> offers = new HashMap<>();
    private final Scoreboard provider;
    private final MinecraftServer server;

    public OffersComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    /** Returns the backing map (sellerId -> offers). */
    public Map<UUID, List<Offer>> getOffers() {
        return offers;
    }

    /** Convenience: flattened view of all offers. */
    public Collection<Offer> getAllOffersFlat() {
        return offers.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    /** Add an offer under its seller's list. */
    public void addOffer(Offer offer) {
        offers.computeIfAbsent(offer.getSellerId(), k -> new ArrayList<>()).add(offer);
        NumismaticGTSComponents.OFFERS.sync(provider);
        TradeScreenRefreshS2CSender.sendRefreshToAll(server);
    }

    /**
     * Remove an offer by offerId (used when an item is SOLD).
     * Does NOT return the item to the seller.
     */
    public void removeOffer(UUID offerId) {
        OwnerOffer found = findOfferWithOwner(offerId);
        if (found == null) return;

        UUID sellerId = found.sellerId();
        Offer offer = found.offer();

        // Clear the stored item stack to avoid desync/ghost copies
        offer.getItem().setCount(0);

        // Remove from seller's list; drop empty lists
        List<Offer> list = offers.get(sellerId);
        if (list != null) {
            list.removeIf(o -> o.getOfferId().equals(offerId));
            if (list.isEmpty()) offers.remove(sellerId);
        }

        // Sync the scoreboard component to all clients
        NumismaticGTSComponents.OFFERS.sync(provider);

        // Notify clients to refresh the UI
        TradeScreenRefreshS2CSender.sendRefreshToAll(server);
    }

    /**
     * Retract an offer by offerId (returns true if removed).
     */
    public void retractOffer(UUID offerId) {
        OwnerOffer found = findOfferWithOwner(offerId);
        if (found == null) return;

        UUID sellerId = found.sellerId();
        Offer offer = found.offer();

        // Give item back to the seller if online
        ServerPlayerEntity seller = server.getPlayerManager().getPlayer(sellerId);
        if (seller != null) {
            seller.giveItemStack(offer.getItem().copy());
        }

        // Remove from seller's list; drop empty lists
        List<Offer> list = offers.get(sellerId);
        if (list != null) {
            list.removeIf(o -> o.getOfferId().equals(offerId));
            if (list.isEmpty()) offers.remove(sellerId);
        }

        NumismaticGTSComponents.OFFERS.sync(provider);
        TradeScreenRefreshS2CSender.sendRefreshToAll(server);
    }

    /** Buy an offer by id. Handles payment, item transfer, and seller credit/offline sales. */
    public void buyOffer(UUID offerId, ServerPlayerEntity buyer) {
        OwnerOffer found = findOfferWithOwner(offerId);
        if (found == null) {
            buyer.sendMessage(Text.literal("Offer not found.").formatted(Formatting.RED), false);
            return;
        }

        UUID sellerId = found.sellerId();
        Offer offer = found.offer();
        long price = offer.getPrice();
        CurrencyComponent buyerWallet = ModComponents.CURRENCY.get(buyer);

        if (buyerWallet.getValue() < price) {
            buyer.sendMessage(Text.literal("You can't afford this").formatted(Formatting.RED), false);
            return;
        }

        // Try to give item to buyer
        ItemStack stack = offer.getItem().copy();
        boolean inserted = buyer.getInventory().insertStack(stack);
        if (!inserted) {
            buyer.sendMessage(Text.literal("Not enough inventory space."), false);
            return;
        }

        // charge buyer
        buyerWallet.modify(-price);

        // credit seller (online or offline)
        ServerPlayerEntity seller = server.getPlayerManager().getPlayer(sellerId);
        if (seller != null) {
            ModComponents.CURRENCY.get(seller).modify(price);
        } else {
            NumismaticGTSComponents.OFFLINE_SALES.get(provider).addSale(sellerId, price);
        }

        // track total sales
        NumismaticGTSComponents.TOTAL_SALES.get(provider).addSale(sellerId, price);

        // remove the purchased listing
        removeOffer(offerId);

        NumismaticGTSComponents.OFFERS.sync(provider);

        buyer.sendMessage(Text.literal("Purchase successful!").formatted(Formatting.GOLD), false);
    }

    /** Get an offer by id (null if not found). */
    public Offer getOffer(UUID offerId) {
        var found = findOfferWithOwner(offerId);
        return found == null ? null : found.offer();
    }

    /** NBT: store as a flat list under "Offers"; rebuild seller->list on read. */
    @Override
    public void readFromNbt(NbtCompound tag) {
        offers.clear();
        NbtList list = tag.getList("Offers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound offerTag = list.getCompound(i);
            Offer offer = Offer.fromNbt(offerTag);
            offers.computeIfAbsent(offer.getSellerId(), k -> new ArrayList<>()).add(offer);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (List<Offer> sellerOffers : offers.values()) {
            for (Offer offer : sellerOffers) {
                list.add(offer.toNbt());
            }
        }
        tag.put("Offers", list);
    }

    /* ---------------- Internals ---------------- */

    private OwnerOffer findOfferWithOwner(UUID offerId) {
        for (var entry : offers.entrySet()) {
            for (Offer o : entry.getValue()) {
                if (o.getOfferId().equals(offerId)) {
                    return new OwnerOffer(entry.getKey(), o);
                }
            }
        }
        return null;
    }

    /** Small helper to return both the sellerId and the Offer. */
    private record OwnerOffer(UUID sellerId, Offer offer) {}
}

