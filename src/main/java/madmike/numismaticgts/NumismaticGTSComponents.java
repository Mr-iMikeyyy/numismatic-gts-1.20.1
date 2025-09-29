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

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import madmike.numismaticgts.components.player.StoreSlotsComponent;
import madmike.numismaticgts.components.scoreboard.OffersComponent;
import madmike.numismaticgts.components.scoreboard.OfflineSalesComponent;
import madmike.numismaticgts.components.scoreboard.PlayerNamesComponent;
import madmike.numismaticgts.components.scoreboard.TotalSalesComponent;
import net.minecraft.util.Identifier;

public final class NumismaticGTSComponents implements ScoreboardComponentInitializer, EntityComponentInitializer {

    public NumismaticGTSComponents() {}

    private static Identifier id(String path) {
        return new Identifier(NumismaticGTS.MOD_ID, path);
    }

    /* -------------------------------------------------------
     * Player-scoped Components (attached to ServerPlayer)
     * ----------------------------------------------------- */

    public static final ComponentKey<StoreSlotsComponent> STORE_SLOTS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("store_slots"), StoreSlotsComponent.class);


    /* -------------------------------------------------------
     * Scoreboard-scoped Components (server-wide/stateful)
     * ----------------------------------------------------- */

    public static final ComponentKey<OffersComponent> OFFERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("offers"), OffersComponent.class);

    public static final ComponentKey<OfflineSalesComponent> OFFLINE_SALES =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("offline_sales"), OfflineSalesComponent.class);

    public static final ComponentKey<TotalSalesComponent> TOTAL_SALES =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("total_sales"), TotalSalesComponent.class);

    public static final ComponentKey<PlayerNamesComponent> PLAYER_NAMES =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("player_names"), PlayerNamesComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry entityComponentFactoryRegistry) {
        entityComponentFactoryRegistry.registerForPlayers(STORE_SLOTS, StoreSlotsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry scoreboardComponentFactoryRegistry) {
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(OFFERS, OffersComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(OFFLINE_SALES, OfflineSalesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(TOTAL_SALES, TotalSalesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(PLAYER_NAMES, PlayerNamesComponent::new);
    }
}


