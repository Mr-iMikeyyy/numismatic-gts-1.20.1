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

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OfflineSalesComponent implements Component {

    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    // sellerId -> total profit
    private final Map<UUID, Long> offlineSales = new HashMap<>();

    public OfflineSalesComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        offlineSales.clear();

        if (tag.contains("OfflineSales", NbtElement.COMPOUND_TYPE)) {
            NbtCompound salesTag = tag.getCompound("OfflineSales");
            Set<String> keys = salesTag.getKeys();
            for (String key : keys) {
                try {
                    UUID id = UUID.fromString(key);
                    long total = salesTag.getLong(key);
                    offlineSales.put(id, total);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid UUID keys
                }
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtCompound salesTag = new NbtCompound();
        for (Map.Entry<UUID, Long> entry : offlineSales.entrySet()) {
            salesTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("OfflineSales", salesTag);
    }

    /* ---------------- Public API ---------------- */

    /** Add profit to a seller's total (accumulates). */
    public void addSale(UUID sellerID, long profit) {
        offlineSales.merge(sellerID, profit, Long::sum);
        // If you want clients to see updated totals immediately:
        // OPAPCComponents.OFFLINE_SALES.sync(scoreboard);
    }

    /** Get the seller's total offline sales (0 if none). */
    public long getSales(UUID sellerID) {
        return offlineSales.getOrDefault(sellerID, 0L);
    }

    /** True if we have a (non-zero or zero) entry for this seller. */
    public boolean hasSales(UUID sellerID) {
        return offlineSales.containsKey(sellerID);
    }

    /** Remove the seller's entry and sync. */
    public void clearSales(UUID sellerID) {
        offlineSales.remove(sellerID);
    }

    /** Expose the full map (mutable). Consider returning an unmodifiable view if needed. */
    public Map<UUID, Long> getAllOfflineSales() {
        return offlineSales;
    }
}
