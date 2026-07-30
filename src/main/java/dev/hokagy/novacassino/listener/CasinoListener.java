package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CasinoListener implements Listener {

    private final NovaCassino plugin;
    private final Random random = new Random();
    // Карта активных игр, чтобы нельзя было запустить сразу несколько вращений на одной станции
    private final Map<Integer, Boolean> activeGames = new HashMap<>();

    public CasinoListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if (station.getCenterLocation().getWorld().equals(player.getWorld())) {
                if (station.getCenterLocation().distance(player.getLocation()) <= station.getRadius() + 1.5) {
                    if (event.getAction().name().contains("RIGHT_CLICK")) {
                        event.setCancelled(true);
                        if (activeGames.getOrDefault(station.getId(), false)) {
                            player.sendMessage(Component.text("На этой станции уже идет рулетка! Подождите завершения.", NamedTextColor.RED));
                            return;
                        }
                        openBetMenu(player, station);
                        return;
                    }
                }
            }
        }
    }

    public void openBetMenu(Player player, CasinoStation station) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("🎰 Станция #" + station.getId() + " | Ставка", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

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
                    Component.text("Нажмите для старта рулетки!", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().title().toString();
        if (!title.contains("Ставка") || !title.contains("Станция #")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Извлекаем ID станции из названия GUI
        int stationId = -1;
        try {
            String idStr = title.substring(title.indexOf("#") + 1, title.indexOf(" |"));
            stationId = Integer.parseInt(idStr.trim());
        } catch (Exception ignored) {}

        CasinoStation station = plugin.getCasinoManager().getStation(stationId);
        if (station == null) return;

        double bet = 0;
        if (clicked.getType() == Material.GOLD_NUGGET) bet = 100;
        else if (clicked.getType() == Material.GOLD_INGOT) bet = 500;
        else if (clicked.getType() == Material.GOLD_BLOCK) bet = 2500;

        if (bet > 0) {
            player.closeInventory();
            if (VaultHook.hasEconomy()) {
                double balance = VaultHook.getEconomy().getBalance(player);
                if (balance < bet) {
                    player.sendMessage(Component.text("У вас недостаточно средств! Баланс: " + balance + "$", NamedTextColor.RED));
                    return;
                }
                VaultHook.getEconomy().withdrawPlayer(player, bet);
                player.sendMessage(Component.text("Ставка ", NamedTextColor.GREEN)
                        .append(Component.text(bet + "$", NamedTextColor.GOLD))
                        .append(Component.text(" принята! Крутим рулетку...", NamedTextColor.GREEN)));

                // Запуск анимации вращения вокруг арены
                startSpinAnimation(player, station, bet);

            } else {
                player.sendMessage(Component.text("Экономика (Vault) недоступна на сервере!", NamedTextColor.RED));
            }
        }
    }

    // 🌟 ПОЛНОЦЕННАЯ 3D-АНИМАЦИЯ ВРАЩЕНИЯ С ЧАСТИЦАМИ И ОБНОВЛЕНИЕМ ГОЛОГРАММЫ
    private void startSpinAnimation(Player player, CasinoStation station, double bet) {
        activeGames.put(station.getId(), true);

        // Обновляем текст голограммы
        station.updateHologram(
                Component.text("🎰 КРУТИТСЯ РУЛЕТКА! 🎰", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                        .append(Component.text("\nСтавка: ", NamedTextColor.GRAY)).append(Component.text(bet + "$", NamedTextColor.GREEN))
        );

        // Создаем плавающий предмет в центре
        Location centerLoc = station.getCenterLocation();
        ItemDisplay centerStar = centerLoc.getWorld().spawn(centerLoc.clone().add(0, 1.2, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.NETHER_STAR));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        // Определение результата рулетки
        int outcome = random.nextInt(100);
        double multiplier = (outcome < 50) ? 0.0 : (outcome < 88 ? 2.0 : 5.0); // 50% - 0x, 38% - 2x, 12% - 5x

        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            int maxTicks = 120; // 6 секунд вращения

            @Override
            public void run() {
                ticks++;

                // Плавное замедление шарика
                double progress = (double) ticks / maxTicks;
                double speed = Math.max(0.02, (1.0 - Math.pow(progress, 1.5)) * 0.4);
                angle += speed;

                // Вычисление координат шарика на круге
                double radius = station.getRadius();
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location ballLoc = centerLoc.clone().add(x, 0.4, z);

                // --- ЧАСТИЦЫ И ЭФФЕКТЫ ---
                // 1. Светящийся след за шариком
                ballLoc.getWorld().spawnParticle(Particle.END_ROD, ballLoc, 4, 0.05, 0.05, 0.05, 0.01);
                ballLoc.getWorld().spawnParticle(Particle.DUST, ballLoc, 3, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f));

                // 2. Кольцевые частицы от центра к шарику
                if (ticks % 2 == 0) {
                    centerLoc.getWorld().spawnParticle(Particle.WITCH, centerLoc.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.01);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, (float) (1.2 + (1.0 - progress)));
                }

                // Конец анимации
                if (ticks >= maxTicks) {
                    this.cancel();
                    centerStar.remove();
                    finishGame(player, station, bet, multiplier, ballLoc);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishGame(Player player, CasinoStation station, double bet, double multiplier, Location winLoc) {
        if (multiplier > 0) {
            double winAmount = bet * multiplier;
            if (VaultHook.hasEconomy()) {
                VaultHook.getEconomy().depositPlayer(player, winAmount);
            }

            // Фейерверк и эффект на месте остановившегося шарика
            winLoc.getWorld().spawnParticle(Particle.FIREWORK, winLoc, 40, 0.3, 0.3, 0.3, 0.15);
            winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 50, 0.5, 0.5, 0.5, 0.2);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // Обновляем голограмму
            station.updateHologram(
                    Component.text("🎉 ВЫИГРЫШ! 🎉", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                            .append(Component.text("\nКуш: ", NamedTextColor.GOLD)).append(Component.text("+" + winAmount + "$ (" + multiplier + "x)", NamedTextColor.GREEN, TextDecoration.BOLD))
            );

            player.sendMessage(Component.text(" Вы выиграли ", NamedTextColor.GOLD)
                    .append(Component.text(winAmount + "$", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" (" + multiplier + "x)!", NamedTextColor.GOLD)));

        } else {
            // Проигрыш
            winLoc.getWorld().spawnParticle(Particle.SMOKE, winLoc, 30, 0.2, 0.2, 0.2, 0.05);
            winLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, winLoc, 5, 0.2, 0.2, 0.2, 0.0);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

            // Обновляем голограмму
            station.updateHologram(
                    Component.text("❌ ПРОИГРЫШ ❌", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                            .append(Component.text("\nПовезет в следующий раз!", NamedTextColor.GRAY))
            );

            player.sendMessage(Component.text(" Ставка проиграна...", NamedTextColor.RED));
        }

        // Через 5 секунд возвращаем голограмму в исходное состояние ожидания
        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
                activeGames.put(station.getId(), false);
            }
        }.runTaskLater(plugin, 100L);
    }
}
