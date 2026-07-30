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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class CasinoListener implements Listener {

    private final NovaCassino plugin;
    private final Random random = new Random();

    // Блокировка станции на время игры (Station ID -> Status)
    private final Map<Integer, Boolean> activeGames = new HashMap<>();
    
    // Ожидание ввода кастомной ставки в чат (Player UUID -> Station ID)
    private final Map<UUID, Integer> awaitingCustomBet = new HashMap<>();

    public CasinoListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    // 1. Открытие GUI по ПКМ на станцию
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if (station.getCenterLocation().getWorld().equals(player.getWorld())) {
                if (station.getCenterLocation().distance(player.getLocation()) <= station.getRadius() + 1.5) {
                    if (event.getAction().name().contains("RIGHT_CLICK")) {
                        event.setCancelled(true);

                        // Проверка: крутится ли станция прямо сейчас
                        if (activeGames.getOrDefault(station.getId(), false)) {
                            player.sendMessage(Component.text("⛔ На этой станции уже идет рулетка! Дождитесь окончания.", NamedTextColor.RED));
                            return;
                        }

                        openBetMenu(player, station);
                        return;
                    }
                }
            }
        }
    }

    // 👥 Поиск игроков рядом со станцией (до 5 человек)
    private List<Player> getNearbyPlayers(CasinoStation station) {
        List<Player> nearby = new ArrayList<>();
        Location center = station.getCenterLocation();
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distance(center) <= station.getRadius() + 2.0) {
                nearby.add(p);
                if (nearby.size() >= 5) break;
            }
        }
        return nearby;
    }

    // 2. Открытие GUI
    public void openBetMenu(Player player, CasinoStation station) {
        List<Player> nearbyPlayers = getNearbyPlayers(station);
        int count = nearbyPlayers.size();

        String modeTitle = (count == 1) ? "Соло-режим" : "Игроков рядом: " + count + "/5";
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("🎰 #" + station.getId() + " | " + modeTitle, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        // Готовые кнопки ставок
        gui.setItem(10, createBetItem(Material.GOLD_NUGGET, "Маленькая ставка", 100));
        gui.setItem(12, createBetItem(Material.GOLD_INGOT, "Средняя ставка", 500));
        gui.setItem(14, createBetItem(Material.GOLD_BLOCK, "Крупная ставка", 2500));

        // Своя ставка
        ItemStack customItem = new ItemStack(Material.PAPER);
        ItemMeta cMeta = customItem.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Component.text("✍️ Своя ставка", NamedTextColor.YELLOW, TextDecoration.BOLD));
            cMeta.lore(List.of(
                    Component.text("Нажмите, чтобы ввести сумму", NamedTextColor.GRAY),
                    Component.text("прямо в чат!", NamedTextColor.GRAY)
            ));
            customItem.setItemMeta(cMeta);
        }
        gui.setItem(16, customItem);

        player.openInventory(gui);
    }

    private ItemStack createBetItem(Material mat, String name, double amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD));
            meta.lore(List.of(
                    Component.text("Поставить: ", NamedTextColor.GRAY).append(Component.text(amount + "$", NamedTextColor.GREEN)),
                    Component.text("Нажмите для старта!", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    // 3. Обработка клика в меню
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().title().toString();
        if (!title.contains("🎰 #")) return;

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

        // Нажал на "Своя ставка"
        if (clicked.getType() == Material.PAPER) {
            player.closeInventory();
            awaitingCustomBet.put(player.getUniqueId(), station.getId());
            player.sendMessage(Component.text("✍️ Введите желаемую сумму ставки в чат (или 'cancel' для отмены):", NamedTextColor.YELLOW));
            return;
        }

        double bet = 0;
        if (clicked.getType() == Material.GOLD_NUGGET) bet = 100;
        else if (clicked.getType() == Material.GOLD_INGOT) bet = 500;
        else if (clicked.getType() == Material.GOLD_BLOCK) bet = 2500;

        if (bet > 0) {
            player.closeInventory();
            processGameStart(player, station, bet);
        }
    }

    // 4. Перехват чата для ввода своей ставки
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingCustomBet.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        int stationId = awaitingCustomBet.remove(player.getUniqueId());
        CasinoStation station = plugin.getCasinoManager().getStation(stationId);

        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("❌ Ввод ставки отменен.", NamedTextColor.RED));
            return;
        }

        if (station == null) return;

        try {
            double bet = Double.parseDouble(msg);
            if (bet <= 0) {
                player.sendMessage(Component.text("❌ Ставка должна быть больше 0$!", NamedTextColor.RED));
                return;
            }

            // Переходим в основной поток Bukkit для запуска спина
            Bukkit.getScheduler().runTask(plugin, () -> processGameStart(player, station, bet));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("❌ Некорректное число! Ввод ставки отменен.", NamedTextColor.RED));
        }
    }

    // 5. Проверка балансов и запуск игры
    private void processGameStart(Player host, CasinoStation station, double betPerPlayer) {
        if (activeGames.getOrDefault(station.getId(), false)) {
            host.sendMessage(Component.text("⛔ На этой станции уже крутится рулетка!", NamedTextColor.RED));
            return;
        }

        List<Player> nearbyPlayers = getNearbyPlayers(station);

        // 🟢 ОДИНОЧНЫЙ РЕЖИМ (1 игрок)
        if (nearbyPlayers.size() <= 1) {
            if (!VaultHook.hasEconomy() || VaultHook.getEconomy().getBalance(host) < betPerPlayer) {
                host.sendMessage(Component.text("❌ У вас недостаточно средств! Баланс: " + (VaultHook.hasEconomy() ? VaultHook.getEconomy().getBalance(host) : 0) + "$", NamedTextColor.RED));
                return;
            }
            VaultHook.getEconomy().withdrawPlayer(host, betPerPlayer);
            startSoloSpin(host, station, betPerPlayer);
        }
        // ⚔️ МУЛЬТИПЛЕЕР (2-5 игроков)
        else {
            List<Player> qualifiedPlayers = new ArrayList<>();
            for (Player p : nearbyPlayers) {
                if (VaultHook.hasEconomy() && VaultHook.getEconomy().getBalance(p) >= betPerPlayer) {
                    qualifiedPlayers.add(p);
                } else {
                    p.sendMessage(Component.text("⚠️ У вас недостаточно средств (" + betPerPlayer + "$) для дуэли!", NamedTextColor.RED));
                }
            }

            if (qualifiedPlayers.size() < 2) {
                host.sendMessage(Component.text("❌ Недостаточно игроков с нужным балансом для мультиплеера!", NamedTextColor.RED));
                return;
            }

            // Списываем деньги у всех готовых участников
            for (Player p : qualifiedPlayers) {
                VaultHook.getEconomy().withdrawPlayer(p, betPerPlayer);
                p.sendMessage(Component.text("🎰 Вы в игре! Ставка ", NamedTextColor.GREEN)
                        .append(Component.text(betPerPlayer + "$", NamedTextColor.GOLD))
                        .append(Component.text(" принята.", NamedTextColor.GREEN)));
            }

            startMultiplayerSpin(qualifiedPlayers, station, betPerPlayer);
        }
    }

    // 🎰 1. Соло-рулетка
    private void startSoloSpin(Player player, CasinoStation station, double bet) {
        activeGames.put(station.getId(), true);

        station.updateHologram(
                Component.text("🎰 СОЛО РУЛЕТКА 🎰", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("\nИгрок: ", NamedTextColor.GRAY)).append(Component.text(player.getName(), NamedTextColor.WHITE))
                        .append(Component.text("\nСтавка: ", NamedTextColor.GRAY)).append(Component.text(bet + "$", NamedTextColor.GREEN))
        );

        Location centerLoc = station.getCenterLocation();
        ItemDisplay centerStar = centerLoc.getWorld().spawn(centerLoc.clone().add(0, 1.3, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.NETHER_STAR));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        int outcome = random.nextInt(100);
        double multiplier = (outcome < 45) ? 0.0 : (outcome < 75 ? 1.5 : (outcome < 93 ? 3.0 : 10.0));

        runSpinTask(station, centerLoc, centerStar, (winLoc) -> {
            if (multiplier > 0) {
                double winAmount = bet * multiplier;
                VaultHook.getEconomy().depositPlayer(player, winAmount);

                if (multiplier >= 10.0) {
                    winLoc.getWorld().strikeLightningEffect(winLoc);
                    winLoc.getWorld().spawnParticle(Particle.FIREWORK, winLoc, 80, 0.5, 0.5, 0.5, 0.2);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                    station.updateHologram(Component.text("🔥 ДЖЕКПОТ 10X! 🔥\nВыигрыш: +" + winAmount + "$", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                } else {
                    winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 50, 0.4, 0.4, 0.4, 0.15);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    station.updateHologram(Component.text("🎉 ВЫИГРЫШ! 🎉\nКуш: +" + winAmount + "$ (" + multiplier + "x)", NamedTextColor.GREEN, TextDecoration.BOLD));
                }
            } else {
                winLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, winLoc, 25, 0.3, 0.3, 0.3, 0.05);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
                station.updateHologram(Component.text("❌ ПРОИГРЫШ ❌", NamedTextColor.RED, TextDecoration.BOLD));
            }

            resetStationLater(station, 100L);
        });
    }

    // ⚔️ 2. Мультиплеер (Битва за общий банк)
    private void startMultiplayerSpin(List<Player> participants, CasinoStation station, double betPerPlayer) {
        activeGames.put(station.getId(), true);
        double totalBank = betPerPlayer * participants.size();

        station.updateHologram(
                Component.text("⚔️ БИТВА ЗА БАНК: " + totalBank + "$ ⚔️", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("\nИгроков: ", NamedTextColor.GRAY)).append(Component.text(participants.size() + " чел.", NamedTextColor.GREEN))
        );

        Location centerLoc = station.getCenterLocation();
        ItemDisplay centerItem = centerLoc.getWorld().spawn(centerLoc.clone().add(0, 1.3, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.DIAMOND));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        // Случайный победитель забирает банк
        Player winner = participants.get(random.nextInt(participants.size()));

        runSpinTask(station, centerLoc, centerItem, (winLoc) -> {
            VaultHook.getEconomy().depositPlayer(winner, totalBank);

            winLoc.getWorld().spawnParticle(Particle.FIREWORK, winLoc, 60, 0.4, 0.4, 0.4, 0.15);
            winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 80, 0.5, 0.5, 0.5, 0.2);

            for (Player p : participants) {
                if (p.equals(winner)) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    p.sendMessage(Component.text("🏆 ВЫ ЗАБРАЛИ ВЕСЬ БАНК: ", NamedTextColor.GOLD)
                            .append(Component.text(totalBank + "$!", NamedTextColor.GREEN, TextDecoration.BOLD)));
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                    p.sendMessage(Component.text("💀 Вы проиграли... Победил: " + winner.getName(), NamedTextColor.RED));
                }
            }

            station.updateHologram(
                    Component.text("🏆 ПОБЕДИТЕЛЬ: " + winner.getName() + " 🏆", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("\nКуш: ", NamedTextColor.GRAY)).append(Component.text("+" + totalBank + "$", NamedTextColor.GOLD, TextDecoration.BOLD))
            );

            resetStationLater(station, 120L);
        });
    }

    // 🌟 Универсальный таск анимации вращения
    private void runSpinTask(CasinoStation station, Location centerLoc, ItemDisplay centerItem, java.util.function.Consumer<Location> onFinish) {
        new BukkitRunnable() {
            int ticks = 0;
            double ballAngle = 0;
            float rotation = 0;
            int maxTicks = 130;

            @Override
            public void run() {
                ticks++;
                double progress = (double) ticks / maxTicks;
                double speed = Math.max(0.015, (1.0 - Math.pow(progress, 1.6)) * 0.42);
                ballAngle += speed;
                rotation += 0.15f;

                double hoverY = Math.sin(ticks * 0.12) * 0.18;
                centerItem.teleport(centerLoc.clone().add(0, 1.3 + hoverY, 0));

                Quaternionf rot = new Quaternionf(new AxisAngle4f(rotation, 0, 1, 0));
                org.bukkit.util.Transformation trans = new org.bukkit.util.Transformation(
                        new Vector3f(0, 0, 0), rot, new Vector3f(1.8f, 1.8f, 1.8f), new Quaternionf()
                );
                centerItem.setInterpolationDuration(1);
                centerItem.setTransformation(trans);

                double radius = station.getRadius();
                double x = radius * Math.cos(ballAngle);
                double z = radius * Math.sin(ballAngle);
                Location ballLoc = centerLoc.clone().add(x, 0.4, z);

                // Искры и лазерные лучи
                for (int i = 0; i <= 6; i++) {
                    double ratio = (double) i / 6;
                    Location rayPoint = centerLoc.clone().add(x * ratio, 0.5 + (hoverY * (1 - ratio)), z * ratio);
                    rayPoint.getWorld().spawnParticle(Particle.DUST, rayPoint, 1, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 0.7f));
                }

                ballLoc.getWorld().spawnParticle(Particle.END_ROD, ballLoc, 2, 0.04, 0.04, 0.04, 0.01);

                if (ticks % 2 == 0) {
                    centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, (float) (1.2 + (1.0 - progress)));
                }

                if (ticks >= maxTicks) {
                    this.cancel();
                    centerItem.remove();
                    onFinish.accept(ballLoc);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void resetStationLater(CasinoStation station, long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
                activeGames.put(station.getId(), false);
            }
        }.runTaskLater(plugin, delay);
    }
}
