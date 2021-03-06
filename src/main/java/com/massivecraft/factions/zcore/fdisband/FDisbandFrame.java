package com.massivecraft.factions.zcore.fdisband;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.ItemBuilder;
import com.massivecraft.factions.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Factions - Developed by Driftay.
 * All rights reserved 2020.
 * Creation Date: 1/18/2020
 */
public class FDisbandFrame {

    private Gui gui;

    public FDisbandFrame(Faction faction) {
        this.gui = new Gui(FactionsPlugin.getInstance(), 1, "Confirm Disband");
    }

    public void buildGUI(FPlayer fPlayer, Faction faction) {
        int i;
        PaginatedPane pane = new PaginatedPane(0, 0, 9, this.gui.getRows());
        List<GuiItem> GUIItems = new ArrayList<>();
        ItemStack confirm = buildConfirmDummyItem(faction);
        ItemStack deny = buildDenyDummyItem();

        for (i = 0; i < 5; ++i) {
            GUIItems.add(new GuiItem(confirm, (e) -> {
                e.setCancelled(true);
                fPlayer.getPlayer().setMetadata("disband_confirm", new FixedMetadataValue(FactionsPlugin.getInstance(), System.currentTimeMillis()));
                fPlayer.getPlayer().closeInventory();
                fPlayer.getPlayer().performCommand("f disband");
            }));
        }

        //Separator
        FileConfiguration config = FactionsPlugin.getInstance().getConfig();

        Material material = Material.getMaterial(config.getString("f-disband-gui.separation-item.Type"));
        String name = config.getString("f-disband-gui.separation-item.Name").replaceAll("%faction%", faction.getTag());
        List<String> separatorLore = config.getStringList("f-disband-gui.separation-item.Lore");

        ItemStack separatorItem = new ItemBuilder(material, 0)
                .name(name)
                .lore(separatorLore)
                .build();

        GUIItems.set(4, new GuiItem(separatorItem, (e) -> e.setCancelled(true)));
        //End Separator

        for (i = 5; i < 10; ++i) {
            GUIItems.add(new GuiItem(deny, (e) -> {
                e.setCancelled(true);
                fPlayer.getPlayer().closeInventory();
            }));
        }
        pane.populateWithGuiItems(GUIItems);
        gui.addPane(pane);
        gui.update();
        gui.show(fPlayer.getPlayer());
    }


    private ItemStack buildConfirmDummyItem(Faction faction) {
        ConfigurationSection config = FactionsPlugin.getInstance().getConfig().getConfigurationSection("f-disband-gui.confirm-item");
        ItemStack item = XMaterial.matchXMaterial(config.getString("Type")).get().parseItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String s : config.getStringList("Lore")) {
                lore.add(FactionsPlugin.getInstance().color(s).replace("{faction}", faction.getTag()));
            }
            meta.setLore(lore);
            meta.setDisplayName(FactionsPlugin.getInstance().color(Objects.requireNonNull(config.getString("Name"))));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildDenyDummyItem() {
        ConfigurationSection config = FactionsPlugin.getInstance().getConfig().getConfigurationSection("f-disband-gui.deny-item");
        ItemStack item = XMaterial.matchXMaterial(config.getString("Type")).get().parseItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(FactionsPlugin.getInstance().colorList(config.getStringList("Lore")));
            meta.setDisplayName(FactionsPlugin.getInstance().color(config.getString("Name")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String args) {
        return ChatColor.translateAlternateColorCodes('&', args);
    }
}
