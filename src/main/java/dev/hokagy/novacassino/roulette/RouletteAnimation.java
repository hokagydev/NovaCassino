package dev.hokagy.novacassino.roulette;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Управляет анимацией рулетки.
 * Исправления:
 *  - Адаптация под Paper API 1.21.1 (Particle.HAPPY_VILLAGER и Particle.EXPLOSION_EMITTER)
 *  - Безопасное использование VaultHook.getEconomy()
 *  - Расчет шансов и мультипликаторов из config.yml (solo_chances)
 */
public class RouletteAnimation {

    private final NovaCassino plugin;
    private final CasinoStation station;
    private final Player player;
    private final double betAmount;

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

                station.updateHologram(Component.text("🎰 ВРАЩЕНИЕ... 🎰", NamedTextColor.GOLD, TextDecoration.BOLD));

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
                    player.sendMessage(Component.text("🔥 ДЖЕКПОТ! Вы выиграли " + payout + "$! (x" + multiplier + ")", NamedTextColor.GOLD, TextDecoration.BOLD));
                    station.updateHologram(Component.text("🔥 ДЖЕКПОТ: " + payout + "$ 🔥", NamedTextColor.GOLD, TextDecoration.BOLD));
                } else {
                    player.sendMessage(Component.text("🎉 Вы выиграли " + payout + "$! (x" + multiplier + ")", NamedTextColor.GREEN, TextDecoration.BOLD));
                    station.updateHologram(Component.text("ВЫИГРЫШ: " + payout + "$", NamedTextColor.GREEN, TextDecoration.BOLD));
                }
            }
        } else {
            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            if (player.isOnline()) {
                player.sendMessage(Component.text("❌ Вы проиграли " + betAmount + "$!", NamedTextColor.RED));
            }
            station.updateHologram(Component.text("ПРОИГРЫШ!", NamedTextColor.RED, TextDecoration.BOLD));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
            }
        }.runTaskLater(plugin, 100L);
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
