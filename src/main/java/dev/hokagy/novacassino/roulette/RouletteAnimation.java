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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Управляет анимацией рулетки.
 * Исправления:
 *  - snapshot списка стойк при старте (чтобы внешние удаления/перезагрузки не ломали индексы)
 *  - проверки isValid перед использованием ArmorStand
 *  - проверка онлайн-статуса игрока перед отправкой сообщений и выплатой
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

        // Берём снимок списка, чтобы дальнейшие изменения в оригинальном списке не сломали индексы
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

                // Безопасный доступ к текущей стойке: проверяем на null и isValid()
                ArmorStand currentStand = null;
                try {
                    currentStand = activeStands.get(currentIndex);
                } catch (Exception ignored) {}

                if (currentStand == null || !currentStand.isValid()) {
                    // если текущая стойка недействительна — просто сдвигаем индекс и продолжаем
                    currentIndex = (currentIndex + 1) % Math.max(1, activeStands.size());
                    currentTick++;
                    return;
                }

                Location loc = currentStand.getLocation().clone().add(0, 0.8, 0);

                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.1, 0.1, 0.1, 0);
                    loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
                }

                station.updateHologram(Component.text("🎰 ВРАЩЕНИЕ... 🎰", NamedTextColor.GOLD, TextDecoration.BOLD));

                currentTick++;
                currentIndex = (currentIndex + 1) % activeStands.size();

                // Ускорение / замедление
                if (currentTick > totalTicks - 20) {
                    speedDelay = 3;
                } else if (currentTick > totalTicks - 10) {
                    speedDelay = 5;
                }

                if (currentTick >= totalTicks) {
                    cancel();
                    // Зафиксируем winningStand — удостоверимся, что он валиден
                    ArmorStand winning = null;
                    try {
                        winning = activeStands.get(Math.min(targetIndex, activeStands.size() - 1));
                    } catch (Exception ignored) {}
                    if (winning != null && winning.isValid()) {
                        finish(winning, targetIndex);
                    } else {
                        // Если выигрышная стойка недоступна — просто сбросим голограмму
                        station.resetHologram();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finish(ArmorStand winningStand, int winIndex) {
        if (winningStand == null || !winningStand.isValid()) {
            station.resetHologram();
            return;
        }

        Location loc = winningStand.getLocation().clone().add(0, 0.8, 0);
        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        boolean isWin = (winIndex % 2 == 0);
        if (isWin) {
            double winMoney = betAmount * 2;
            try {
                if (plugin.getVaultHook() != null && plugin.getVaultHook().getEconomy() != null && player.isOnline()) {
                    plugin.getVaultHook().getEconomy().depositPlayer(player, winMoney);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Ошибка при выдаче выигрыша: " + ex.getMessage());
            }
            if (player.isOnline()) {
                player.sendMessage(Component.text("🎉 Вы выиграли " + winMoney + "$!", NamedTextColor.GREEN, TextDecoration.BOLD));
            }
            station.updateHologram(Component.text("ВЫИГРЫШ: " + winMoney + "$", NamedTextColor.GREEN, TextDecoration.BOLD));
        } else {
            if (player.isOnline()) {
                player.sendMessage(Component.text("❌ Вы проиграли " + betAmount + "$!", NamedTextColor.RED));
            }
            station.updateHologram(Component.text("ПРОИГРЫШ!", NamedTextColor.RED, TextDecoration.BOLD));
        }

        // Сброс голограммы через задержку (в основном потоке)
        new BukkitRunnable() {
            @Override
            public void run() {
                station.resetHologram();
            }
        }.runTaskLater(plugin, 100L);
    }
}
