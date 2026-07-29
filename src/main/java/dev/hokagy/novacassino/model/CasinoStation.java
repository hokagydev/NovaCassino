package dev.hokagy.novacassino.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CasinoStation {

    private final int id;
    private final String type;
    private final Location centerLocation;
    private double radius;

    private TextDisplay hologram;
    private final List<BlockDisplay> ringBlocks = new ArrayList<>();

    public CasinoStation(int id, String type, Location centerLocation, double radius) {
        this.id = id;
        this.type = type;
        this.centerLocation = centerLocation;
        this.radius = radius;
    }

    public void spawn() {
        clear(); // Очистить старые сущности перед переспавном

        // 1. Создаем голограмму над центром
        hologram = centerLocation.getWorld().spawn(centerLocation.clone().add(0, 2.2, 0), TextDisplay.class, text -> {
            text.text(Component.text("Casino", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .append(Component.text("\nКликните чтобы сделать ставку!", NamedTextColor.GRAY))
                    .append(Component.text("\nОжидание игроков...", NamedTextColor.YELLOW)));
            text.setBillboard(Display.Billboard.CENTER);
            text.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
        });

        // 2. Спавним визуальный круг рулетки (37 сегментов: черные и красные блоки)
        int segments = 37;
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location blockLoc = centerLocation.clone().add(x, 0.2, z);
            Material mat = (i == 0) ? Material.GREEN_CONCRETE : (i % 2 == 0 ? Material.RED_CONCRETE : Material.BLACK_CONCRETE);
            BlockData blockData = mat.createBlockData();

            BlockDisplay blockDisplay = centerLocation.getWorld().spawn(blockLoc, BlockDisplay.class, display -> {
                display.setBlock(blockData);
                // Уменьшаем блоки до красивого размера рулетки
                Transformation trans = new Transformation(
                        new Vector3f(-0.2f, 0, -0.2f),
                        new Quaternionf(),
                        new Vector3f(0.4f, 0.3f, 0.4f),
                        new Quaternionf()
                );
                display.setTransformation(trans);
            });
            ringBlocks.add(blockDisplay);
        }
    }

    public void clear() {
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
        }
        for (BlockDisplay bd : ringBlocks) {
            if (bd != null && !bd.isDead()) {
                bd.remove();
            }
        }
        ringBlocks.clear();
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public Location getCenterLocation() { return centerLocation; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { 
        this.radius = radius; 
        spawn(); // Перерисовываем с новым радиусом
    }
}
