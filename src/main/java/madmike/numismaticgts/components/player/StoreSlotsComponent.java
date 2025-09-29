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

package madmike.numismaticgts.components.player;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public class StoreSlotsComponent implements ComponentV3 {

    private final PlayerEntity player;
    private int unlockedSlots = 5;

    public StoreSlotsComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int slots) {
        this.unlockedSlots = slots;
    }

    public void increment(int amount) {
        this.unlockedSlots += amount;
    }

    public void reset() {
        this.unlockedSlots = 5;
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        this.unlockedSlots = nbt.getInt("UnlockedSlots");
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        nbt.putInt("UnlockedSlots", this.unlockedSlots);
    }
}
