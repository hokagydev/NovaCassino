package dev.hokagy.novacassino.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class CasinoStation {

    private final int id;
    private final String type;
    private final Location centerLocation;
    private double radius;
    
    private Location displayStart;
    private Location displayEnd;
    
    private final List<ArmorStand> rouletteStands = new ArrayList<>();
    private ArmorStand hologramStand;

    public CasinoStation(int id, String type, Location centerLocation, Location displayStart, Location displayEnd) {
        this(id, type, centerLocation, 3.5);
        this.displayStart = displayStart;
        this.displayEnd = displayEnd;
    }

    public CasinoStation(int id, String type, Location centerLocation, double radius) {
        this.id = id;
        this.type = type;
        this.centerLocation = centerLocation;
        this.radius = radius;
        this.displayStart = centerLocation;
        this.displayEnd = centerLocation;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public Location getCenterLocation() { return centerLocation; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    public Location getDisplayStart() { return displayStart; }
    public void setDisplayStart(Location displayStart) { this.displayStart = displayStart; }
    public Location getDisplayEnd() { return displayEnd; }
    public void setDisplayEnd(Location displayEnd) { this.displayEnd = displayEnd; }

    // --- Спавн 3D-круга рулетки (голов по кругу) ---
    public void spawnRouletteRing() {
        removeRouletteRing();

        if (!"ROULETTE".equalsIgnoreCase(type) || centerLocation == null || centerLocation.getWorld() == null) {
            return;
        }

        int totalSlots = 37; // 1 зеленый (зеро), 18 красных, 18 черных
        for (int i = 0; i < totalSlots; i++) {
            double angle = 2 * Math.PI * i / totalSlots;
            double x = centerLocation.getX() + radius * Math.cos(angle);
            double z = centerLocation.getZ() + radius * Math.sin(angle);
            Location loc = new Location(centerLocation.getWorld(), x, centerLocation.getY(), z);

            ArmorStand stand = centerLocation.getWorld().spawn(loc, ArmorStand.class);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setVisible(false);

            // Определяем цвет сектора (0 = Green, нечетные = Red, четные = Black)
            Material headMaterial = (i == 0) ? Material.EMERALD_BLOCK : (i % 2 == 0 ? Material.BLACK_CONCRETE : Material.RED_CONCRETE);
            stand.getEquipment().setHelmet(new ItemStack(headMaterial));

            rouletteStands.add(stand);
        }
    }

    public void removeRouletteRing() {
        for (ArmorStand stand : rouletteStands) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
        rouletteStands.clear();
    }

    // --- Управление голограммой ---
    public void updateHologram(Component text) {
        if (hologramStand == null || !hologramStand.isValid()) {
            spawnHologram(text);
        } else {
            hologramStand.customName(text);
        }
    }

    public void resetHologram() {
        Component defaultText = Component.text("Casino", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("Кликните чтобы сделать ставку!", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Ожидание игроков...", NamedTextColor.GOLD));
        updateHologram(defaultText);
    }

    private void spawnHologram(Component text) {
        if (centerLocation == null || centerLocation.getWorld() == null) return;
        
        Location holoLoc = centerLocation.clone().add(0, 1.8, 0);
        hologramStand = centerLocation.getWorld().spawn(holoLoc, ArmorStand.class);
        hologramStand.setGravity(false);
        hologramStand.setCanPickupItems(false);
        hologramStand.setCustomNameVisible(true);
        hologramStand.customName(text);
        hologramStand.setVisible(false);
    }

    public void remove() {
        removeRouletteRing();
        if (hologramStand != null && hologramStand.isValid()) {
            hologramStand.remove();
        }
    }
}
