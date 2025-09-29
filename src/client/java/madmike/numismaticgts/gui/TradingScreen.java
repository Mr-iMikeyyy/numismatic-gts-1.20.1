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

package madmike.numismaticgts.gui;

import com.glisco.numismaticoverhaul.ModComponents;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import madmike.numismaticgts.NumismaticGTSComponents;
import madmike.numismaticgts.data.Offer;
import madmike.numismaticgts.net.packets.BuyOfferC2SPacket;
import madmike.numismaticgts.net.packets.RemoveOfferC2SPacket;
import madmike.numismaticgts.util.CurrencyUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

import static madmike.numismaticgts.util.CurrencyUtil.formatPrice;


public class TradingScreen extends BaseOwoScreen<FlowLayout> {

    public TradingScreen() {
        super(Text.literal("Trading Terminal"));
    }

    public record TradingScreenTab(String name, UUID id) { }

    private TradingScreenTab currentTab;
    private final UUID myOffersTabID = UUID.randomUUID();
    private final UUID allTabID = UUID.randomUUID();

    private FlowLayout offerListContainer;
    private FlowLayout tabBarContents;
    private TextBoxComponent searchBox;
    private FlowLayout walletContainer;
    private boolean onlyAffordable = false;
    private long walletAmount;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);

        tabBarContents = Containers.horizontalFlow(Sizing.content(), Sizing.content()).gap(4);
        List<TradingScreenTab> tabs = buildTabs();

        if (!tabs.isEmpty()) {
            for (TradingScreenTab tab : tabs) {
                tabBarContents.child(Components.button(Text.literal(tab.name()), b -> switchTab(tab)));
            }
        } else {
            tabBarContents.child(Components.label(Text.literal("No tabs to show")));
        }

        ScrollContainer<FlowLayout> tabBarScroll =
                Containers.horizontalScroll(Sizing.fill(100), Sizing.content(), tabBarContents);
        rootComponent.child(tabBarScroll);

        walletContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        walletContainer.child(buildWallet());
        rootComponent.child(walletContainer);

        searchBox = Components.textBox(Sizing.fill(100));
        searchBox.setSuggestion("Search by item or seller...");
        searchBox.onChanged().subscribe(query -> refresh());
        rootComponent.child(searchBox);

        CheckboxComponent onlyAffordableCheckbox =
                Components.checkbox(Text.literal("Only show affordable"))
                        .onChanged(b -> {
                            onlyAffordable = b;
                            refresh();
                        })
                        .checked(onlyAffordable);
        rootComponent.child(onlyAffordableCheckbox);

        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100)).gap(10);
        offerListContainer = Containers.verticalFlow(Sizing.content(), Sizing.fill(100)).gap(4);
        ScrollContainer<FlowLayout> scrollOffers =
                Containers.verticalScroll(Sizing.fill(60), Sizing.fill(100), offerListContainer);
        mainContent.child(scrollOffers);
        rootComponent.child(mainContent);

        if (!tabs.isEmpty()) {
            switchTab(tabs.get(0));
        }
    }

    public Component buildWallet() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            long value = ModComponents.CURRENCY.get(client.player).getValue();
            walletAmount = value;
            Text text = formatPrice(value);
            return Components.label(text);
        }
        return null;
    }

    public void rebuildWallet() {
        walletContainer.clearChildren();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            long value = ModComponents.CURRENCY.get(client.player).getValue();
            walletAmount = value;
            Text text = formatPrice(value);
            walletContainer.child(Components.label(text));
        }
    }

    private List<TradingScreenTab> buildTabs() {
        List<TradingScreenTab> tabs = new ArrayList<>();

        // Always add "My Offers" and "All"
        tabs.add(new TradingScreenTab("My Offers", myOffersTabID));
        tabs.add(new TradingScreenTab("All", allTabID));

        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return tabs;

        Scoreboard sb = world.getScoreboard();
        // sellerId -> list<Offer>
        Map<UUID, List<Offer>> offersBySeller = NumismaticGTSComponents.OFFERS.get(sb).getOffers();
        if (offersBySeller.isEmpty()) return tabs;

        // Resolve names via the PlayerNamesComponent (server-synced cache)
        var names = NumismaticGTSComponents.PLAYER_NAMES.get(sb);

        // Optional: sort tabs by display name
        List<UUID> sellerIds = new ArrayList<>(offersBySeller.keySet());
        sellerIds.sort(Comparator.comparing(id -> names.resolve(id).toLowerCase(Locale.ROOT)));

        for (UUID sellerId : sellerIds) {
            String name = names.resolve(sellerId); // falls back to UUID string if unknown
            tabs.add(new TradingScreenTab(name, sellerId));
        }

        return tabs;
    }

    private void switchTab(TradingScreenTab tab) {
        offerListContainer.clearChildren();
        currentTab = tab;

        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return;

        Scoreboard sb = world.getScoreboard();
        Map<UUID, List<Offer>> offersBySeller = NumismaticGTSComponents.OFFERS.get(sb).getOffers();
        if (offersBySeller.isEmpty()) return;

        Predicate<Offer> matchesSearch = offer -> {
            String query = searchBox.getMessage().getString().toLowerCase().trim();
            String itemName = offer.getItem().getName().getString().toLowerCase();
            boolean matchesQuery = itemName.contains(query);
            boolean isAffordable = !onlyAffordable || offer.getPrice() <= walletAmount;
            return matchesQuery && isAffordable;
        };

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        UUID me = player.getUuid();

        // "My Offers" tab
        if (tab.id().equals(myOffersTabID)) {
            offersBySeller
                    .getOrDefault(me, java.util.Collections.emptyList())
                    .stream()
                    .filter(matchesSearch)
                    .forEach(offer ->
                            offerListContainer.child(
                                    createOfferRow(offer, CurrencyUtil.formatPrice(offer.getPrice()), false, true)
                            )
                    );
            return;
        }

        // "All" tab
        if (tab.id().equals(allTabID)) {
            offersBySeller.values().stream()
                    .flatMap(java.util.List::stream)
                    .filter(o -> !o.getSellerId().equals(me))
                    .filter(matchesSearch)
                    .forEach(offer ->
                            offerListContainer.child(
                                    createOfferRow(offer, CurrencyUtil.formatPrice(offer.getPrice()), true, false)
                            )
                    );
            return;
        }

        // Per-seller tab
        boolean isSelf = tab.id().equals(me);
        offersBySeller
                .getOrDefault(tab.id(), java.util.Collections.emptyList())
                .stream()
                .filter(matchesSearch)
                .forEach(offer ->
                        offerListContainer.child(
                                createOfferRow(offer, CurrencyUtil.formatPrice(offer.getPrice()), !isSelf, isSelf)
                        )
                );
    }

    private FlowLayout createOfferRow(Offer offer, Text priceText, boolean showBuyButton, boolean showRemoveButton) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20)).gap(6);
        row.child(Components.item(offer.getItem()).showOverlay(true).setTooltipFromStack(true));
        row.child(Components.label(priceText).horizontalTextAlignment(HorizontalAlignment.CENTER));
        if (showBuyButton)
            row.child(Components.button(Text.literal("Buy"), b -> BuyOfferC2SPacket.send(offer.getOfferId())));
        if (showRemoveButton) {
            row.child(Components.button(Text.literal("Remove").formatted(Formatting.RED),
                    b -> RemoveOfferC2SPacket.send(offer.getOfferId())));
        }
        return row;
    }

    public void refresh() {
        if (currentTab != null) {
            switchTab(currentTab);
            rebuildWallet();
        }
    }
}
