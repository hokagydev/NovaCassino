package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
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
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Random random = new Random();

    private final Map<Integer, Boolean> activeGames = new HashMap<>();
    private final Map<UUID, Integer> awaitingCustomBet = new HashMap<>();

    public CasinoListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    private Component getMsg(String path) {
        String msg = plugin.getMessagesConfig().getString(path, "");
        return miniMessage.deserialize(msg);
    }

    private Component getMsg(String path, Map<String, String> placeholders) {
        String msg = plugin.getMessagesConfig().getString(path, "");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return miniMessage.deserialize(msg);
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
                            player.sendMessage(getMsg("game_already_running"));
                            return;
                        }

                        openBetMenu(player, station);
                        return;
                    }
                }
            }
        }
    }

    private List<Player> getNearbyPlayers(CasinoStation station) {
        List<Player> nearby = new ArrayList<>();
        Location center = station.getCenterLocation();
        double offset = plugin.getConfig().getDouble("station.detection_radius_offset", 2.0);
        int max = plugin.getConfig().getInt("station.max_players", 5);

        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distance(center) <= station.getRadius() + offset) {
                nearby.add(p);
                if (nearby.size() >= max) break;
            }
        }
        return nearby;
    }

    public void openBetMenu(Player player, CasinoStation station) {
        List<Player> nearbyPlayers = getNearbyPlayers(station);
        int count = nearbyPlayers.size();
        int max = plugin.getConfig().getInt("station.max_players", 5);

        Component title = (count == 1)
                ? getMsg("gui.title_solo", Map.of("id", String.valueOf(station.getId())))
                : getMsg("gui.title_multi", Map.of("id", String.valueOf(station.getId()), "count", String.valueOf(count), "max", String.valueOf(max)));

        Inventory gui = Bukkit.createInventory(null, 27, title);

        FileConfiguration config = plugin.getConfig();
        gui.setItem(10, createBetItem(Material.GOLD_NUGGET, "Маленькая ставка", config.getDouble("preset_bets.slot_10", 100.0)));
        gui.setItem(12, createBetItem(Material.GOLD_INGOT, "Средняя ставка", config.getDouble("preset_bets.slot_12", 500.0)));
        gui.setItem(14, createBetItem(Material.GOLD_BLOCK, "Крупная ставка", config.getDouble("preset_bets.slot_14", 2500.0)));

        ItemStack customItem = new ItemStack(Material.PAPER);
        ItemMeta cMeta = customItem.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(getMsg("gui.custom_item_name"));
            List<Component> lore = new ArrayList<>();
            for (String l : plugin.getMessagesConfig().getStringList("gui.custom_item_lore")) {
                lore.add(miniMessage.deserialize(l));
            }
            cMeta.lore(lore);
            customItem.setItemMeta(cMeta);
        }
        gui.setItem(16, customItem);

        player.openInventory(gui);
    }

    private ItemStack createBetItem(Material mat, String name, double amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(getMsg("gui.bet_item_name", Map.of("name", name)));
            List<Component> lore = new ArrayList<>();
            for (String l : plugin.getMessagesConfig().getStringList("gui.bet_item_lore")) {
                lore.add(miniMessage.deserialize(l.replace("<amount>", String.valueOf(amount))));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Перехватываем клик по нашему заголовку GUI
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        CasinoStation station = null;
        for (CasinoStation s : plugin.getCasinoManager().getStations().values()) {
            if (s.getCenterLocation().getWorld().equals(player.getWorld()) &&
                s.getCenterLocation().distance(player.getLocation()) <= s.getRadius() + 3.0) {
                station = s;
                break;
            }
        }
        if (station == null) return;

        event.setCancelled(true);

        if (clicked.getType() == Material.PAPER) {
            player.closeInventory();
            awaitingCustomBet.put(player.getUniqueId(), station.getId());
            player.sendMessage(getMsg("enter_custom_bet"));
            return;
        }

        FileConfiguration config = plugin.getConfig();
        double bet = 0;
        if (clicked.getType() == Material.GOLD_NUGGET) bet = config.getDouble("preset_bets.slot_10", 100.0);
        else if (clicked.getType() == Material.GOLD_INGOT) bet = config.getDouble("preset_bets.slot_12", 500.0);
        else if (clicked.getType() == Material.GOLD_BLOCK) bet = config.getDouble("preset_bets.slot_14", 2500.0);

        if (bet > 0) {
            player.closeInventory();
            processGameStart(player, station, bet);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingCustomBet.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        int stationId = awaitingCustomBet.remove(player.getUniqueId());
        CasinoStation station = plugin.getCasinoManager().getStation(stationId);

        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage(getMsg("bet_cancelled"));
            return;
        }

        if (station == null) return;

        try {
            double bet = Double.parseDouble(msg);
            double min = plugin.getConfig().getDouble("custom_bet.min", 10.0);
            double max = plugin.getConfig().getDouble("custom_bet.max", 100000.0);

            if (bet < min || bet > max) {
                player.sendMessage(getMsg("bet_must_be_positive"));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> processGameStart(player, station, bet));
        } catch (NumberFormatException e) {
            player.sendMessage(getMsg("invalid_number"));
        }
    }

    private void processGameStart(Player host, CasinoStation station, double betPerPlayer) {
        if (activeGames.getOrDefault(station.getId(), false)) {
            host.sendMessage(getMsg("game_already_running"));
            return;
        }

        List<Player> nearbyPlayers = getNearbyPlayers(station);

        if (nearbyPlayers.size() <= 1) {
            if (!VaultHook.hasEconomy() || VaultHook.getEconomy().getBalance(host) < betPerPlayer) {
                host.sendMessage(getMsg("not_enough_money_solo", Map.of("amount", String.valueOf(betPerPlayer))));
                return;
            }
            VaultHook.getEconomy().withdrawPlayer(host, betPerPlayer);
            startSoloSpin(host, station, betPerPlayer);
        } else {
            List<Player> qualifiedPlayers = new ArrayList<>();
            for (Player p : nearbyPlayers) {
                if (VaultHook.hasEconomy() && VaultHook.getEconomy().getBalance(p) >= betPerPlayer) {
                    qualifiedPlayers.add(p);
                } else {
                    p.sendMessage(getMsg("not_enough_money_multi", Map.of("amount", String.valueOf(betPerPlayer))));
                }
            }

            if (qualifiedPlayers.size() < 2) {
                host.sendMessage(getMsg("not_enough_qualified_players"));
                return;
            }

            for (Player p : qualifiedPlayers) {
                VaultHook.getEconomy().withdrawPlayer(p, betPerPlayer);
                p.sendMessage(getMsg("join_multi_game", Map.of("amount", String.valueOf(betPerPlayer))));
            }

            startMultiplayerSpin(qualifiedPlayers, station, betPerPlayer);
        }
    }

    private void startSoloSpin(Player player, CasinoStation station, double bet) {
        activeGames.put(station.getId(), true);

        station.updateHologram(getMsg("hologram.solo_active", Map.of("player", player.getName(), "amount", String.valueOf(bet))));

        Location centerLoc = station.getCenterLocation();
        ItemDisplay centerStar = centerLoc.getWorld().spawn(centerLoc.clone().add(0, 1.3, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.NETHER_STAR));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        // Расчет выигрыша из config.yml
        double multiplier = calculateSoloMultiplier();

        runSpinTask(station, centerLoc, centerStar, (winLoc) -> {
            if (multiplier > 0) {
                double winAmount = bet * multiplier;
                VaultHook.getEconomy().depositPlayer(player, winAmount);

                if (multiplier >= 10.0) {
                    winLoc.getWorld().strikeLightningEffect(winLoc);
                    winLoc.getWorld().spawnParticle(Particle.FIREWORK, winLoc, 80, 0.5, 0.5, 0.5, 0.2);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                    player.sendMessage(getMsg("solo_jackpot", Map.of("amount", String.valueOf(winAmount))));
                    station.updateHologram(getMsg("hologram.solo_jackpot", Map.of("amount", String.valueOf(winAmount))));
                } else {
                    winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 50, 0.4, 0.4, 0.4, 0.15);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    player.sendMessage(getMsg("solo_win", Map.of("amount", String.valueOf(winAmount), "multiplier", String.valueOf(multiplier))));
                    station.updateHologram(getMsg("hologram.solo_win", Map.of("amount", String.valueOf(winAmount), "multiplier", String.valueOf(multiplier))));
                }
            } else {
                winLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, winLoc, 25, 0.3, 0.3, 0.3, 0.05);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
                player.sendMessage(getMsg("solo_loss"));
                station.updateHologram(getMsg("hologram.solo_loss"));
            }

            resetStationLater(station, 100L);
        });
    }

    private double calculateSoloMultiplier() {
        int roll = random.nextInt(100);
        FileConfiguration cfg = plugin.getConfig();

        int lossChance = cfg.getInt("solo_chances.loss.chance", 45);
        int smallChance = cfg.getInt("solo_chances.win_small.chance", 30);
        int mediumChance = cfg.getInt("solo_chances.win_medium.chance", 18);

        if (roll < lossChance) {
            return cfg.getDouble("solo_chances.loss.multiplier", 0.0);
        } else if (roll < lossChance + smallChance) {
            return cfg.getDouble("solo_chances.win_small.multiplier", 1.5);
        } else if (roll < lossChance + smallChance + mediumChance) {
            return cfg.getDouble("solo_chances.win_medium.multiplier", 3.0);
        } else {
            return cfg.getDouble("solo_chances.jackpot.multiplier", 10.0);
        }
    }

    private void startMultiplayerSpin(List<Player> participants, CasinoStation station, double betPerPlayer) {
        activeGames.put(station.getId(), true);
        double totalBank = betPerPlayer * participants.size();

        station.updateHologram(getMsg("hologram.multi_active", Map.of("amount", String.valueOf(totalBank), "count", String.valueOf(participants.size()))));

        Location centerLoc = station.getCenterLocation();
        ItemDisplay centerItem = centerLoc.getWorld().spawn(centerLoc.clone().add(0, 1.3, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.DIAMOND));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        Player winner = participants.get(random.nextInt(participants.size()));

        runSpinTask(station, centerLoc, centerItem, (winLoc) -> {
            VaultHook.getEconomy().depositPlayer(winner, totalBank);

            winLoc.getWorld().spawnParticle(Particle.FIREWORK, winLoc, 60, 0.4, 0.4, 0.4, 0.15);
            winLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, winLoc, 80, 0.5, 0.5, 0.5, 0.2);

            for (Player p : participants) {
                if (p.equals(winner)) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    p.sendMessage(getMsg("multi_win", Map.of("amount", String.valueOf(totalBank))));
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                    p.sendMessage(getMsg("multi_loss", Map.of("winner", winner.getName())));
                }
            }

            station.updateHologram(getMsg("hologram.multi_win", Map.of("winner", winner.getName(), "amount", String.valueOf(totalBank))));

            resetStationLater(station, 120L);
        });
    }

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
