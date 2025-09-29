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

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TotalSalesComponent implements ComponentV3 {
    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    // sellerId -> total profit
    private final Map<UUID, Long> offlineSales = new HashMap<>();

    public TotalSalesComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        offlineSales.clear();
        if (nbt.contains("TotalSales", NbtElement.COMPOUND_TYPE)) {
            NbtCompound sales = nbt.getCompound("TotalSales");
            Set<String> keys = sales.getKeys();
            for (String key : keys) {
                try {
                    UUID id = UUID.fromString(key);
                    long total = sales.getLong(key);
                    offlineSales.put(id, total);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid UUID keys
                }
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtCompound sales = new NbtCompound();
        for (Map.Entry<UUID, Long> e : offlineSales.entrySet()) {
            sales.putLong(e.getKey().toString(), e.getValue());
        }
        nbt.put("TotalSales", sales);
    }

    /* ---------------- Public API (optional but useful) ---------------- */

    /** Add profit to a seller's total. */
    public void addSale(UUID sellerId, long amount) {
        offlineSales.merge(sellerId, amount, Long::sum);
    }

    /** Get a seller's total sales (0 if none). */
    public long getSales(UUID sellerId) {
        return offlineSales.getOrDefault(sellerId, 0L);
    }

    /** Remove a seller's entry. */
    public void clearSeller(UUID sellerId) {
        offlineSales.remove(sellerId);
    }

    /** Expose the full map (mutable). */
    public Map<UUID, Long> getAllSales() {
        return offlineSales;
    }
}
