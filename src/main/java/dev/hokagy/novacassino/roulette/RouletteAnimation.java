package dev.hokagy.novacassino.roulette;

import dev.hokagy.novacassino.NovaCassino;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Random;

public class RouletteAnimation {

    private final NovaCassino plugin;
    private final Player player;
    private final double betAmount;
    private final Random random = new Random();

    public RouletteAnimation(NovaCassino plugin, Player player, double betAmount) {
        this.plugin = plugin;
        this.player = player;
        this.betAmount = betAmount;
    }

    public void start() {
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2)).add(0, 1.5, 0);

        // Создаем голограмму-заголовок
        TextDisplay textDisplay = loc.getWorld().spawn(loc.clone().add(0, 0.8, 0), TextDisplay.class, display -> {
            display.text(Component.text("🎰 NOVACASSINO 🎰", NamedTextColor.GOLD, TextDecoration.BOLD));
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
        });

        // Создаем плавающий предмет рулетки
        ItemDisplay itemDisplay = loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.NETHER_STAR));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });

        // Случайный выигрыш: 0x (проигрыш), 2x, или 5x (джекпот)
        int outcome = random.nextInt(100);
        double multiplier;
        if (outcome < 50) {
            multiplier = 0.0; // 50% проигрыш
        } else if (outcome < 90) {
            multiplier = 2.0; // 40% удвоение
        } else {
            multiplier = 5.0; // 10% джекпот
        }

        new BukkitRunnable() {
            int ticks = 0;
            float angle = 0;
            int maxTicks = 100; // 5 секунд анимации

            @Override
            public void run() {
                ticks++;

                // Рассчитываем замедление
                float speed = (float) (maxTicks - ticks) / maxTicks * 0.5f;
                angle += Math.max(speed, 0.05f);

                // Вращение предмета
                Quaternionf rotation = new Quaternionf(new AxisAngle4f(angle, 0, 1, 0));
                itemDisplay.setInterpolationDuration(1);
                itemDisplay.setInterpolationDelay(0);
                itemDisplay.setLeftRotation(rotation);

                // Эффекты и звуки
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 3, 0.2, 0.2, 0.2, 0.02);
                if (ticks % 3 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f + (speed * 2));
                }

                // Завершение анимации
                if (ticks >= maxTicks) {
                    this.cancel();
                    finish(textDisplay, itemDisplay, multiplier);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finish(TextDisplay textDisplay, ItemDisplay itemDisplay, double multiplier) {
        Location loc = itemDisplay.getLocation();

        if (multiplier > 0) {
            double winAmount = betAmount * multiplier;
            textDisplay.text(Component.text("ВЫИГРЫШ: " + winAmount + "$ (" + multiplier + "x)!", NamedTextColor.GREEN, TextDecoration.BOLD));
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // TODO: Здесь добавь выдачу денег через Vault API
            player.sendMessage(Component.text(" Поздравляем! Вы выиграли ", NamedTextColor.GOLD)
                    .append(Component.text(winAmount + "$", NamedTextColor.GREEN, TextDecoration.BOLD)));
        } else {
            textDisplay.text(Component.text("ПРОИГРЫШ! Повезет в другой раз", NamedTextColor.RED, TextDecoration.BOLD));
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.3, 0.3, 0.3, 0.05);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            
            player.sendMessage(Component.text(" Вы проиграли свою ставку...", NamedTextColor.RED));
        }

        // Удаляем объекты рулетки через 3 секунды
        new BukkitRunnable() {
            @Override
            public void run() {
                textDisplay.remove();
                itemDisplay.remove();
            }
        }.runTaskLater(plugin, 60L);
    }
}
