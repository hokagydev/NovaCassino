package dev.hokagy.novacassino.machine;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.listener.SlotInteractionListener;
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
    private final Location pos1;
    private final Location pos2;
    private final int stationId;
    private final double betAmount;

    private final List<Material> symbols = Arrays.asList(
            Material.RED_CONCRETE,
            Material.CACTUS,
            Material.DIAMOND_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.LAPIS_BLOCK
    );

    private final Material[][] currentGrid = new Material[3][3];
    private final Random random = new Random();
    private int ticks = 0;

    public SlotMachineTask(NovaCassino plugin, Player player, Location pos1, Location pos2, int stationId, double betAmount) {
        this.plugin = plugin;
        this.player = player;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.stationId = stationId;
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
        player.playSound(pos1, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.4f);

        if (ticks >= 35) {
            cancel();
            checkWin();
            SlotInteractionListener.spinningStations.remove(stationId);
        }
    }

    private void renderGrid() {
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        boolean isXAxis = (maxX - minX) >= (maxZ - minZ);

        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                // Строго плоскость 3х3 по выделенным точкам
                int x = isXAxis ? minX + col : minX;
                int z = isXAxis ? minZ : minZ + col;
                int y = minY + row;

                Block block = pos1.getWorld().getBlockAt(x, y, z);
                block.setType(currentGrid[col][row], false);
            }
        }
    }

    private void checkWin() {
        boolean win = false;
        Material winningMat = null;

        for (int row = 0; row < 3; row++) {
            if (currentGrid[0][row] == currentGrid[1][row] && currentGrid[1][row] == currentGrid[2][row]) {
                win = true;
                winningMat = currentGrid[0][row];
                break;
            }
        }

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
            player.playSound(pos1, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            spawnFirework(pos1.clone().add(0, 3, 0));

            player.sendMessage(Component.text("🎉 ВЫИГРЫШ! ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Вы собрали 3 в ряд и выиграли $" + prize, NamedTextColor.GOLD)));
        } else {
            player.playSound(pos1, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Увы! Повезет в следующий раз.", NamedTextColor.RED));
        }
    }

    private double getMultiplier(Material mat) {
        return switch (mat) {
            case DIAMOND_BLOCK -> 10.0;
            case EMERALD_BLOCK -> 7.0;
            case GOLD_BLOCK -> 5.0;
            case RED_CONCRETE -> 3.0;
            case CACTUS -> 2.0;
            default -> 1.5;
        };
    }

    private void spawnFirework(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.fromRGB(255, 215, 0))
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
