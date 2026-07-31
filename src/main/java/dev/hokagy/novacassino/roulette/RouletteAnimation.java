package dev.hokagy.novacassino.roulette;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RouletteAnimation {

    private final NovaCassino plugin;
    private final CasinoStation station;
    private final Player player;
    private final double betAmount;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public RouletteAnimation(NovaCassino plugin, CasinoStation station, Player player, double betAmount) {
        this.plugin = plugin;
        this.station = station;
        this.player = player;
        this.betAmount = betAmount;
    }

    public void start() {
        List<ArmorStand> stands = station.getRouletteStands();
        if (stands.isEmpty()) {
            station.spawnRouletteRing();
            stands = station.getRouletteStands();
        }

        final List<ArmorStand> activeStands = new ArrayList<>(stands);
        if (activeStands.isEmpty()) return;

        final int targetIndex = ThreadLocalRandom.current().nextInt(activeStands.size());
        final int totalTicks = 80 + targetIndex;

        // Обновляем голограмму при старте вращения (hologram.solo_active)
        updateHologramFromConfig("hologram.solo_active", betAmount, 0.0);

        new BukkitRunnable() {
            int currentTick = 0;
            int currentIndex = 0;
            int speedDelay = 1;
            int delayCounter = 0;

            @Override
            public void run() {
                if (activeStands.isEmpty()) {
                    cancel();
                    station.resetHologram();
                    return;
                }

                if (delayCounter < speedDelay) {
                    delayCounter++;
                    return;
                }
                delayCounter = 0;

                ArmorStand currentStand = null;
                try {
                    currentStand = activeStands.get(currentIndex);
                } catch (Exception ignored) {}

                if (currentStand == null || !currentStand.isValid()) {
                    currentIndex = (currentIndex + 1) % Math.max(1, activeStands.size());
                    currentTick++;
                    return;
                }

                Location loc = currentStand.getLocation().clone().add(0, 0.8, 0);

                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 5, 0.1, 0.1, 0.1, 0);
                    loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
                }

                currentTick++;
                currentIndex = (currentIndex + 1) % activeStands.size();

                if (currentTick > totalTicks - 20) {
                    speedDelay = 3;
                } else if (currentTick > totalTicks - 10) {
                    speedDelay = 5;
                }

                if (currentTick >= totalTicks) {
                    cancel();
                    ArmorStand winning = null;
                    try {
                        winning = activeStands.get(Math.min(targetIndex, activeStands.size() - 1));
                    } catch (Exception ignored) {}
                    if (winning != null && winning.isValid()) {
                        finish(winning);
                    } else {
                        station.resetHologram();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finish(ArmorStand winningStand) {
        if (winningStand == null || !winningStand.isValid()) {
            station.resetHologram();
            return;
        }

        Location loc = winningStand.getLocation().clone().add(0, 0.8, 0);

        double multiplier = calculateMultiplier();
        double payout = betAmount * multiplier;

        if (payout > 0) {
            if (loc.getWorld() != null) {
                loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            try {
                if (VaultHook.hasEconomy() && player.isOnline()) {
                    VaultHook.getEconomy().depositPlayer(player, payout);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Ошибка при выдаче выигрыша: " + ex.getMessage());
            }

            if (player.isOnline()) {
                if (multiplier >= 10.0) {
                    // Сообщение и голограмма для ДЖЕКПОТА
                    sendMessageFromConfig("solo_jackpot", payout, multiplier);
                    updateHologramFromConfig("hologram.solo_jackpot", payout, multiplier);
                } else {
                    // Сообщение и голограмма для ОБЫЧНОГО ВЫИГРЫША
                    sendMessageFromConfig("solo_win", payout, multiplier);
                    updateHologramFromConfig("hologram.solo_win", payout, multiplier);
                }
            }
        } else {
            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            if (player.isOnline()) {
                // Сообщение и голограмма для ПРОИГРЫША
                sendMessageFromConfig("solo_loss", betAmount, multiplier);
            }
            updateHologramFromConfig("hologram.solo_loss", betAmount, multiplier);
        }

        // Возвращаем дефолтную голограмму через 5 секунд (100 тиков)
        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
            }
        }.runTaskLater(plugin, 100L);
    }

    /**
     * Форматирует и отправляет сообщение игроку из messages.yml с поддержкой MiniMessage и Legacy & кодов.
     */
    private void sendMessageFromConfig(String path, double amount, double multiplier) {
        FileConfiguration msgConfig = plugin.getMessagesConfig();
        String rawMsg = msgConfig.getString(path, "");
        if (rawMsg.isEmpty()) return;

        String prefix = msgConfig.getString("prefix", "");
        String fullMsg = prefix + rawMsg;

        fullMsg = replacePlaceholders(fullMsg, amount, multiplier);

        // Конвертируем legacy-цвета & в стандартные теги MiniMessage
        fullMsg = convertLegacyToMiniMessage(fullMsg);

        player.sendMessage(miniMessage.deserialize(fullMsg));
    }

    /**
     * Обновляет голограмму над станцией из messages.yml (с поддержкой переносов строк \n).
     */
    private void updateHologramFromConfig(String path, double amount, double multiplier) {
        FileConfiguration msgConfig = plugin.getMessagesConfig();
        String rawHolo = msgConfig.getString(path, "");
        if (rawHolo.isEmpty()) return;

        rawHolo = replacePlaceholders(rawHolo, amount, multiplier);
        rawHolo = convertLegacyToMiniMessage(rawHolo);

        // В случае многострочного текста спарсенный компонент передаем в станцию
        Component component = miniMessage.deserialize(rawHolo);
        station.updateHologram(component);
    }

    private String replacePlaceholders(String text, double amount, double multiplier) {
        return text.replace("<amount>", String.valueOf(amount))
                   .replace("%amount%", String.valueOf(amount))
                   .replace("<multiplier>", String.valueOf(multiplier))
                   .replace("%multiplier%", String.valueOf(multiplier))
                   .replace("<player>", player.getName())
                   .replace("%player%", player.getName());
    }

    private String convertLegacyToMiniMessage(String text) {
        return text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&l", "<bold>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");
    }

    private double calculateMultiplier() {
        int chanceLoss = plugin.getConfig().getInt("solo_chances.loss.chance", 45);
        int chanceSmall = plugin.getConfig().getInt("solo_chances.win_small.chance", 30);
        int chanceMedium = plugin.getConfig().getInt("solo_chances.win_medium.chance", 18);

        double multLoss = plugin.getConfig().getDouble("solo_chances.loss.multiplier", 0.0);
        double multSmall = plugin.getConfig().getDouble("solo_chances.win_small.multiplier", 1.5);
        double multMedium = plugin.getConfig().getDouble("solo_chances.win_medium.multiplier", 3.0);
        double multJackpot = plugin.getConfig().getDouble("solo_chances.jackpot.multiplier", 10.0);

        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < chanceLoss) {
            return multLoss;
        } else if (roll < chanceLoss + chanceSmall) {
            return multSmall;
        } else if (roll < chanceLoss + chanceSmall + chanceMedium) {
            return multMedium;
        } else {
            return multJackpot;
        }
    }
}
