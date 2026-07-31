package dev.hokagy.novacassino.roulette;

import dev.hokagy.novacassino.NovaCassino;
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

import java.util.List;
import java.util.Random;

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

        final List<ArmorStand> activeStands = stands;
        final int targetIndex = new Random().nextInt(activeStands.size());
        final int totalTicks = 80 + targetIndex;

        new BukkitRunnable() {
            int currentTick = 0;
            int currentIndex = 0;
            int speedDelay = 1;
            int delayCounter = 0;

            @Override
            public void run() {
                if (delayCounter < speedDelay) {
                    delayCounter++;
                    return;
                }
                delayCounter = 0;

                ArmorStand currentStand = activeStands.get(currentIndex);
                Location loc = currentStand.getLocation().clone().add(0, 0.8, 0);

                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.1, 0.1, 0.1, 0);
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
                    finish(activeStands.get(targetIndex), targetIndex);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finish(ArmorStand winningStand, int winIndex) {
        Location loc = winningStand.getLocation().clone().add(0, 0.8, 0);
        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        boolean isWin = (winIndex % 2 == 0);
        if (isWin) {
            double winMoney = betAmount * 2;
            if (plugin.getVaultHook() != null && plugin.getVaultHook().getEconomy() != null) {
                plugin.getVaultHook().getEconomy().depositPlayer(player, winMoney);
            }
            player.sendMessage(Component.text("🎉 Вы выиграли " + winMoney + "$!", NamedTextColor.GREEN, TextDecoration.BOLD));
            station.updateHologram(Component.text("ВЫИГРЫШ: " + winMoney + "$", NamedTextColor.GREEN, TextDecoration.BOLD));
        } else {
            player.sendMessage(Component.text("❌ Вы проиграли " + betAmount + "$!", NamedTextColor.RED));
            station.updateHologram(Component.text("ПРОИГРЫШ!", NamedTextColor.RED, TextDecoration.BOLD));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
            }
        }.runTaskLater(plugin, 100L);
    }
}
