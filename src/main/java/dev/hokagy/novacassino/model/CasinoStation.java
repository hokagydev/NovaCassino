package dev.hokagy.novacassino.model;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class CasinoStation {

    private final int id;
    private final String type;
    private final Location centerLocation;
    private double radius;
    
    private Location displayStart;
    private Location displayEnd;
    
    private ArmorStand hologramStand;

    // Конструктор с 5 параметрами (для SLOTS с точками)
    public CasinoStation(int id, String type, Location centerLocation, Location displayStart, Location displayEnd) {
        this(id, type, centerLocation, 3.5);
        this.displayStart = displayStart;
        this.displayEnd = displayEnd;
    }

    // Конструктор с 4 параметрами (для ROULETTE / по умолчанию из CasinoManager)
    public CasinoStation(int id, String type, Location centerLocation, double radius) {
        this.id = id;
        this.type = type;
        this.centerLocation = centerLocation;
        this.radius = radius;
        this.displayStart = centerLocation;
        this.displayEnd = centerLocation;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Location getDisplayStart() {
        return displayStart;
    }

    public void setDisplayStart(Location displayStart) {
        this.displayStart = displayStart;
    }

    public Location getDisplayEnd() {
        return displayEnd;
    }

    public void setDisplayEnd(Location displayEnd) {
        this.displayEnd = displayEnd;
    }

    // --- Методы управления голограммой ---

    public void updateHologram(Component text) {
        if (hologramStand == null || !hologramStand.isValid()) {
            spawnHologram(text);
        } else {
            hologramStand.customName(text);
        }
    }

    public void resetHologram() {
        if (hologramStand != null && hologramStand.isValid()) {
            hologramStand.customName(Component.text("🎰 " + type + " #" + id + " 🎰"));
        }
    }

    private void spawnHologram(Component text) {
        if (centerLocation == null || centerLocation.getWorld() == null) return;
        
        Location holoLoc = centerLocation.clone().add(0, 2.2, 0);
        hologramStand = centerLocation.getWorld().spawn(holoLoc, ArmorStand.class);
        hologramStand.setGravity(false);
        hologramStand.setCanPickupItems(false);
        hologramStand.setCustomNameVisible(true);
        hologramStand.customName(text);
        hologramStand.setVisible(false);
    }

    public void remove() {
        if (hologramStand != null && hologramStand.isValid()) {
            hologramStand.remove();
        }
    }
}
