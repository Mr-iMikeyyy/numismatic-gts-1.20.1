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

package madmike.numismaticgts;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class NumismaticGTSConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("numismatic-gts.toml");

    public static int maxStoreSlotsPerPlayer;
    public static int startingStoreSlotsPerPlayer;

    public static void load() {
        CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_PATH).autosave().build();
        config.load();

        config.setComment("maxStoreSlotsPerPlayer", "Maximum number of item slots one can earn in the store");
        maxStoreSlotsPerPlayer = config.getOrElse("maxStoreSlotsPerPlayer", 30);

        config.setComment("startingStoreSlotsPerPlayer", "Number of store slots players start with");
        startingStoreSlotsPerPlayer = config.getOrElse("startingStoreSlotsPerPlayer", 5);
    }
}
