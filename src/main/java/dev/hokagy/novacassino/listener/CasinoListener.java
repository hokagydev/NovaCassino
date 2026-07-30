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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CasinoListener implements Listener {

    private final NovaCassino plugin;
    private final Random random = new Random();
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
                        .append(Component.text(" принята! Удачи!", NamedTextColor.GREEN)));

                startSpinAnimation(player, station, bet);

            } else {
                player.sendMessage(Component.text("Экономика (Vault) недоступна на сервере!", NamedTextColor.RED));
            }
        }
    }

    private void startSpinAnimation(Player player, CasinoStation station, double bet) {
        activeGames.put(station.getId(), true);

        station.updateHologram(
                Component.text("🎰 ВРАЩЕНИЕ РУЛЕТКИ... 🎰", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                        .append(Component.text("\nСтавка: ", NamedTextColor.GRAY)).append(Component.text(bet + "$", NamedTextColor.GREEN))
        );

        Location centerLoc = station.getCenterLocation();

        ItemDisplay centerStar = centerLoc.getWorld().spawn(centerLoc.clone().add(0, 1.3, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.NETHER_STAR));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        // Генерация коэффициента: 0x (45%), 1.5x (30%), 3x (18%), 10x (7%)
        int outcome = random.nextInt(100);
        double multiplier;
        if (outcome < 45) {
            multiplier = 0.0;
        } else if (outcome < 75) {
            multiplier = 1.5;
        } else if (outcome < 93) {
            multiplier = 3.0;
        } else {
            multiplier = 10.0; // ДЖЕКПОТ!
        }

        new BukkitRunnable() {
            int ticks = 0;
            double ballAngle = 0;
            float starRotation = 0;
            int maxTicks = 130;

            @Override
            public void run() {
                ticks++;

                double progress = (double) ticks / maxTicks;
                double speed = Math.max(0.015, (1.0 - Math.pow(progress, 1.6)) * 0.42);
                ballAngle += speed;
                starRotation += 0.15f;

                // 1. Анимация центральной звезды
                double hoverY = Math.sin(ticks * 0.12) * 0.18;
                Location currentStarLoc = centerLoc.clone().add(0, 1.3 + hoverY, 0);
                centerStar.teleport(currentStarLoc);

                Quaternionf rot = new Quaternionf(new AxisAngle4f(starRotation, 0, 1, 0));
                org.bukkit.util.Transformation trans = new org.bukkit.util.Transformation(
                        new Vector3f(0, 0, 0),
                        rot,
                        new Vector3f(1.8f, 1.8f, 1.8f),
                        new Quaternionf()
                );
                centerStar.setInterpolationDuration(1);
                centerStar.setInterpolationDelay(0);
                centerStar.setTransformation(trans);

                // 2. Движение шарика и световые лучи
                double radius = station.getRadius();
                double x = radius * Math.cos(ballAngle);
                double z = radius * Math.sin(ballAngle);
                Location ballLoc = centerLoc.clone().add(x, 0.4, z);

                int raySteps = 7;
                for (int i = 0; i <= raySteps; i++) {
                    double ratio = (double) i / raySteps;
                    Location rayPoint = centerLoc.clone().add(x * ratio, 0.5 + (hoverY * (1 - ratio)), z * ratio);
                    rayPoint.getWorld().spawnParticle(
                            Particle.DUST,
                            rayPoint,
                            1,
                            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 0.7f)
                    );
                }

                // Частицы шарика
                ballLoc.getWorld().spawnParticle(Particle.END_ROD, ballLoc, 3, 0.04, 0.04, 0.04, 0.01);
                ballLoc.getWorld().spawnParticle(Particle.GLOW, ballLoc, 2, 0.08, 0.08, 0.08, 0.01);

                if (ticks % 2 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, (float) (1.2 + (1.0 - progress)));
                }

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

            if (multiplier >= 10.0) {
                // 🎉 ДЖЕКПОТ (10x)
                winLoc.getWorld().strikeLightningEffect(winLoc);
                winLoc.getWorld().spawnParticle(Particle.FIREWORK, winLoc, 80, 0.5, 0.5, 0.5, 0.2);
                winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 100, 0.8, 0.8, 0.8, 0.3);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);

                station.updateHologram(
                        Component.text("🔥 ДЖЕКПОТ 10X! 🔥", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                                .append(Component.text("\nПобедитель: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.GOLD))
                                .append(Component.text("\nВыигрыш: ", NamedTextColor.YELLOW)).append(Component.text("+" + winAmount + "$", NamedTextColor.GREEN, TextDecoration.BOLD))
                );

                Bukkit.broadcast(Component.text("🎰 Игрок ", NamedTextColor.GOLD)
                        .append(Component.text(player.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .append(Component.text(" сорвал ДЖЕКПОТ ", NamedTextColor.GOLD))
                        .append(Component.text("+" + winAmount + "$ (10x)", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text(" в NovaCassino!", NamedTextColor.GOLD)));

            } else {
                // Обычный выигрыш (1.5x / 3x)
                winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 50, 0.4, 0.4, 0.4, 0.15);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                station.updateHologram(
                        Component.text("🎉 ВЫИГРЫШ! 🎉", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                                .append(Component.text("\nКуш: ", NamedTextColor.GOLD)).append(Component.text("+" + winAmount + "$ (" + multiplier + "x)", NamedTextColor.GREEN, TextDecoration.BOLD))
                );

                player.sendMessage(Component.text(" Поздравляем! Вы выиграли ", NamedTextColor.GOLD)
                        .append(Component.text(winAmount + "$", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text(" (" + multiplier + "x)!", NamedTextColor.GOLD)));
            }

        } else {
            // Проигрыш (0x)
            winLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, winLoc, 25, 0.3, 0.3, 0.3, 0.05);
            winLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, winLoc, 6, 0.3, 0.3, 0.3, 0.0);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);

            station.updateHologram(
                    Component.text("❌ ПРОИГРЫШ ❌", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                            .append(Component.text("\nПовезет в следующий раз!", NamedTextColor.GRAY))
            );

            player.sendMessage(Component.text(" Ставка не сыграла. Попробуйте еще раз!", NamedTextColor.RED));
        }

        // Возврат голограммы через 5 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
                activeGames.put(station.getId(), false);
            }
        }.runTaskLater(plugin, 100L);
    }
}
