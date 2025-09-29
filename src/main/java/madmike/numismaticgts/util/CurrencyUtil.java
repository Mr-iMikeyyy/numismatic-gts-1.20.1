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

package madmike.numismaticgts.util;

import com.glisco.numismaticoverhaul.item.MoneyBagItem;
import com.glisco.numismaticoverhaul.item.NumismaticOverhaulItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CurrencyUtil {

    public static final int BRONZE_PER_SILVER = 100;
    public static final int SILVER_PER_GOLD = 100;
    public static final int BRONZE_PER_GOLD = BRONZE_PER_SILVER * SILVER_PER_GOLD;

    // Converts gold, silver, bronze to total bronze
    public static long toTotalBronze(int gold, int silver, int bronze) {
        return (long) gold * BRONZE_PER_GOLD + (long) silver * BRONZE_PER_SILVER + bronze;
    }

    // Converts total bronze to gold/silver/bronze
    public static CoinBreakdown fromTotalBronze(long totalBronze) {
        int gold = (int) (totalBronze / BRONZE_PER_GOLD);
        totalBronze %= BRONZE_PER_GOLD;

        int silver = (int) (totalBronze / BRONZE_PER_SILVER);
        int bronze = (int) (totalBronze % BRONZE_PER_SILVER);

        return new CoinBreakdown(gold, silver, bronze);
    }

    public static long getValueOfItemStack(ItemStack stack) {
        Item item = stack.getItem();
        int count = stack.getCount();

        if (item == NumismaticOverhaulItems.BRONZE_COIN) {
            return count;
        } else if (item == NumismaticOverhaulItems.SILVER_COIN) {
            return count * 100L;
        } else if (item == NumismaticOverhaulItems.GOLD_COIN) {
            return count * 10_000L;
        } else if (item instanceof MoneyBagItem) {
            NbtCompound nbt = stack.getOrCreateNbt();
            if (nbt.contains("Value")) {
                return nbt.getLong("Value");
            }
        }

        return 0;
    }

    public static Text formatPrice(long price) {
        CoinBreakdown coins = fromTotalBronze(price);
        String priceString = "G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze();

        return Text.literal(priceString);
    }

    // Simple record to hold breakdown
    public record CoinBreakdown(int gold, int silver, int bronze) {}
}
