package dev.hokagy.novacassino.machine;

import dev.hokagy.novacassino.NovaCassino;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SlotMachineTask extends BukkitRunnable {

    private final NovaCassino plugin;
    private final Player player;
    private final Location startCorner; // Левый нижний угол сетки 3x3
    private final double betAmount;

    private final List<Material> symbols = Arrays.asList(
            Material.TNT,
            Material.CACTUS,
            Material.DIAMOND_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.REDSTONE_BLOCK
    );

    private final Material[][] currentGrid = new Material[3][3];
    private final Random random = new Random();
    private int ticks = 0;

    public SlotMachineTask(NovaCassino plugin, Player player, Location startCorner, double betAmount) {
        this.plugin = plugin;
        this.player = player;
        this.startCorner = startCorner;
        this.betAmount = betAmount;
    }

    @Override
    public void run() {
        ticks++;

        for (int col = 0; col < 3; col++) {
            if (ticks < 15 || (ticks < 25 && col > 0) || (ticks < 35 && col > 1)) {
                for (int row = 0; row < 3; row++) {
                    currentGrid[col][row] = symbols.get(random.nextInt(symbols.size()));
                }
            }
        }

        renderGrid();
        player.playSound(startCorner, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);

        if (ticks >= 35) {
            cancel();
            checkWin();
        }
    }

    private void renderGrid() {
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                Block block = startCorner.clone().add(col, row, 0).getBlock();
                block.setType(currentGrid[col][row]);
            }
        }
    }

    private void checkWin() {
        boolean win = false;
        Material winningMat = null;

        // По горизонтали
        for (int row = 0; row < 3; row++) {
            if (currentGrid[0][row] == currentGrid[1][row] && currentGrid[1][row] == currentGrid[2][row]) {
                win = true;
                winningMat = currentGrid[0][row];
                break;
            }
        }

        // По вертикали
        if (!win) {
            for (int col = 0; col < 3; col++) {
                if (currentGrid[col][0] == currentGrid[col][1] && currentGrid[col][1] == currentGrid[col][2]) {
                    win = true;
                    winningMat = currentGrid[col][0];
                    break;
                }
            }
        }

        if (win) {
            double multiplier = getMultiplier(winningMat);
            double prize = betAmount * multiplier;
            player.playSound(startCorner, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            spawnFirework(startCorner.clone().add(1, 3, 0));

            player.sendMessage(Component.text("🎉 ВЫИГРЫШ! ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Вы собрали 3 в ряд (" + winningMat.name() + ") и выиграли $" + prize, NamedTextColor.GOLD)));
        } else {
            player.playSound(startCorner, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Увы! Повезет в следующий раз.", NamedTextColor.RED));
        }
    }

    private double getMultiplier(Material mat) {
        return switch (mat) {
            case DIAMOND_BLOCK -> 10.0;
            case EMERALD_BLOCK -> 7.0;
            case GOLD_BLOCK -> 5.0;
            case TNT -> 3.0;
            case CACTUS -> 2.0;
            default -> 1.5;
        };
    }

    private void spawnFirework(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        
        // 🔥 Исправлено: заменено на точный RGB цвет золота
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.fromRGB(255, 215, 0))
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
                
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
