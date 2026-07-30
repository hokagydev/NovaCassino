package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CasinoListener implements Listener {

    private final NovaCassino plugin;

    public CasinoListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    // 1. Открытие меню ставок при клике в районе станции
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if (station.getCenterLocation().getWorld().equals(player.getWorld())) {
                if (station.getCenterLocation().distance(player.getLocation()) <= station.getRadius() + 1) {
                    if (event.getAction().name().contains("RIGHT_CLICK")) {
                        openBetMenu(player, station);
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    // 2. Создание GUI-меню выбора ставки
    public void openBetMenu(Player player, CasinoStation station) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("🎰 Ставка на станцию #" + station.getId(), NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        gui.setItem(11, createBetItem(Material.GOLD_NUGGET, "Маленькая ставка", 100));
        gui.setItem(13, createBetItem(Material.GOLD_INGOT, "Средняя ставка", 500));
        gui.setItem(15, createBetItem(Material.GOLD_BLOCK, "Крупная ставка", 2500));

        player.openInventory(gui);
    }

    private ItemStack createBetItem(Material mat, String name, double amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD));
            meta.lore(java.util.List.of(
                    Component.text("Поставить: ", NamedTextColor.GRAY).append(Component.text(amount + "$", NamedTextColor.GREEN)),
                    Component.text("Нажмите для подтверждения", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    // 3. Обработка клика в меню и списание денег
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().toString().contains("Ставка на станцию")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        double bet = 0;
        if (clicked.getType() == Material.GOLD_NUGGET) bet = 100;
        else if (clicked.getType() == Material.GOLD_INGOT) bet = 500;
        else if (clicked.getType() == Material.GOLD_BLOCK) bet = 2500;

        if (bet > 0) {
            player.closeInventory();
            if (VaultHook.hasEconomy()) {
                double balance = VaultHook.getEconomy().getBalance(player);
                if (balance < bet) {
                    player.sendMessage(Component.text("У вас недостаточно средств! Требуется: " + bet + "$", NamedTextColor.RED));
                    return;
                }
                VaultHook.getEconomy().withdrawPlayer(player, bet);
                player.sendMessage(Component.text("Ваша ставка ", NamedTextColor.GREEN)
                        .append(Component.text(bet + "$", NamedTextColor.GOLD))
                        .append(Component.text(" принята! Рулетка запускается...", NamedTextColor.GREEN)));
                
                // TODO: Здесь вызывается анимация крутящегося шарика/рулетки
            } else {
                player.sendMessage(Component.text("Экономика (Vault) недоступна на сервере!", NamedTextColor.RED));
            }
        }
    }
}
