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

package madmike.numismaticgts.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class Offer {

    private final UUID offerId;
    private final UUID sellerId;
    private final ItemStack item;
    private final long price;

    public Offer(UUID offerId, UUID sellerId, ItemStack item, long price) {
        this.offerId = offerId;
        this.sellerId = sellerId;
        this.item = item;
        this.price = price;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("OfferID", this.offerId);
        nbt.putUuid("Seller", this.sellerId);
        nbt.put("Item", this.item.writeNbt(new NbtCompound()));
        nbt.putLong("Price", this.price);
        return nbt;
    }

    public static Offer fromNbt(NbtCompound nbt) {
        UUID offerId = nbt.getUuid("OfferID");
        UUID seller = nbt.getUuid("Seller");
        ItemStack item = ItemStack.fromNbt(nbt.getCompound("Item"));
        long price = nbt.getLong("Price");
        return new Offer(offerId, seller, item, price);
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeUuid(offerId);
        buf.writeUuid(sellerId);
        buf.writeItemStack(item);
        buf.writeLong(price);
    }

    public static Offer readFromBuf(PacketByteBuf buf) {
        UUID offerId = buf.readUuid();
        UUID seller = buf.readUuid();
        ItemStack item = buf.readItemStack();
        long price = buf.readLong();
        return new Offer(offerId, seller, item, price);
    }

    public UUID getOfferId() {
        return this.offerId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public ItemStack getItem() {
        return item;
    }

    public long getPrice() {
        return price;
    }
}
