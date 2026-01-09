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
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import madmike.numismaticgts.components.NumismaticGTSComponents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class PlayerNamesComponent implements ComponentV3, AutoSyncedComponent {

    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    /** UUID -> last known profile name */
    private final Map<UUID, String> playerNames = new HashMap<>();

    public PlayerNamesComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    /* ---------------- NBT persistence ---------------- */

    @Override
    public void readFromNbt(NbtCompound nbt) {
        playerNames.clear();
        if (nbt.contains("Names", NbtElement.COMPOUND_TYPE)) {
            NbtCompound names = nbt.getCompound("Names");
            Set<String> keys = names.getKeys();
            for (String k : keys) {
                try {
                    UUID id = UUID.fromString(k);
                    String name = names.getString(k);
                    if (!name.isEmpty()) playerNames.put(id, name);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid UUID keys
                }
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtCompound names = new NbtCompound();
        for (Map.Entry<UUID, String> e : playerNames.entrySet()) {
            names.putString(e.getKey().toString(), e.getValue() == null ? "" : e.getValue());
        }
        nbt.put("Names", names);
    }

    /* ---------------- Sync (CCA AutoSyncedComponent) ---------------- */

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        // Scoreboard-wide cache: safe to sync with everyone
        return true;
    }

    @Override
    public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(playerNames.size());
        for (Map.Entry<UUID, String> e : playerNames.entrySet()) {
            buf.writeUuid(e.getKey());
            buf.writeString(e.getValue() == null ? "" : e.getValue());
        }
    }

    @Override
    public void applySyncPacket(PacketByteBuf buf) {
        playerNames.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUuid();
            // Minecraft usernames are max 16 chars, but we accept whatever we sent
            String name = buf.readString(32);
            if (!name.isEmpty()) playerNames.put(id, name);
        }
    }

    /* ---------------- Public API ---------------- */

    /** Put/update a cached name and sync if it changed. */
    public void put(UUID id, String name) {
        String prev = playerNames.put(id, name);
        if (!Objects.equals(prev, name)) {
            NumismaticGTSComponents.PLAYER_NAMES.sync(scoreboard);
        }
    }

    /** Remove a cached name and sync if present. */
    public void remove(UUID id) {
        if (playerNames.remove(id) != null) {
            NumismaticGTSComponents.PLAYER_NAMES.sync(scoreboard);
        }
    }

    /** Get a cached name (may be null if unknown). */
    public String get(UUID id) {
        return playerNames.get(id);
    }

    /** Resolve a name: cache → online player → user cache → UUID string. Also caches what it learns. */
    public String resolve(UUID id) {
        String cached = playerNames.get(id);
        if (cached != null && !cached.isEmpty()) return cached;

        // Online?
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
        if (online != null) {
            String name = online.getGameProfile().getName();
            put(id, name);
            return name;
        }

        // Known offline?
        var opt = server.getUserCache().getByUuid(id);
        if (opt.isPresent() && opt.get().getName() != null) {
            String name = opt.get().getName();
            put(id, name);
            return name;
        }

        // Fallback
        return id.toString();
    }

}
