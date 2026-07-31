package dev.hokagy.novacassino.model;

import dev.hokagy.novacassino.NovaCassino;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CasinoStation {

    private final NovaCassino plugin;
    private final int id;
    private final String type;
    private final Location centerLocation;
    private double radius;
    
    private Location displayStart;
    private Location displayEnd;
    
    private final List<ArmorStand> rouletteStands = new ArrayList<>();
    private ArmorStand hologramStand;

    public CasinoStation(NovaCassino plugin, int id, String type, Location centerLocation, Location displayStart, Location displayEnd) {
        this(plugin, id, type, centerLocation, 3.5);
        this.displayStart = displayStart;
        this.displayEnd = displayEnd;
    }

    public CasinoStation(NovaCassino plugin, int id, String type, Location centerLocation, double radius) {
        this.plugin = plugin;
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

    public List<ArmorStand> getRouletteStands() {
        return rouletteStands;
    }

    // --- Исправленный спавн маленьких блоков рулетки ---
    public void spawnRouletteRing() {
        removeRouletteRing();

        if (!"ROULETTE".equalsIgnoreCase(type) || centerLocation == null || centerLocation.getWorld() == null) {
            return;
        }

        int totalSlots = plugin.getConfig().getInt("roulette.total-slots", 37);
        double yOffset = plugin.getConfig().getDouble("roulette.y-offset", -0.7);
        
        Material zeroMat = Material.valueOf(plugin.getConfig().getString("roulette.blocks.zero", "EMERALD_BLOCK"));
        Material evenMat = Material.valueOf(plugin.getConfig().getString("roulette.blocks.even", "BLACK_CONCRETE"));
        Material oddMat = Material.valueOf(plugin.getConfig().getString("roulette.blocks.odd", "RED_CONCRETE"));

        for (int i = 0; i < totalSlots; i++) {
            double angle = 2 * Math.PI * i / totalSlots;
            double x = centerLocation.getX() + radius * Math.cos(angle);
            double z = centerLocation.getZ() + radius * Math.sin(angle);
            
            Location loc = new Location(centerLocation.getWorld(), x, centerLocation.getY() + yOffset, z);

            ArmorStand stand = centerLocation.getWorld().spawn(loc, ArmorStand.class);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setVisible(false);
            stand.setSmall(true); // Форсирует маленькие блоки на полу
            stand.setMarker(true); // Убирает хитбокс сущности

            Material headMaterial = (i == 0) ? zeroMat : (i % 2 == 0 ? evenMat : oddMat);
            if (stand.getEquipment() != null) {
                stand.getEquipment().setHelmet(new ItemStack(headMaterial));
            }

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

    public void updateHologram(Component text) {
        if (hologramStand == null || !hologramStand.isValid()) {
            spawnHologram(text);
        } else {
            hologramStand.customName(text);
        }
    }

    public void resetHologram() {
        List<String> lines;
        if ("SLOTS".equalsIgnoreCase(type)) {
            lines = plugin.getMessagesConfig().getStringList("hologram.slots_idle");
        } else {
            lines = plugin.getMessagesConfig().getStringList("hologram.roulette_idle");
        }

        if (lines.isEmpty()) {
            lines = List.of("<aqua><bold>Casino</bold></aqua>", "<white>Кликните чтобы сделать ставку!</white>", "<gold>Ожидание игроков...</gold>");
        }

        Component hologramText = Component.empty();
        MiniMessage mm = MiniMessage.miniMessage();
        for (int i = 0; i < lines.size(); i++) {
            hologramText = hologramText.append(mm.deserialize(lines.get(i)));
            if (i < lines.size() - 1) {
                hologramText = hologramText.append(Component.newline());
            }
        }

        updateHologram(hologramText);
    }

    private void spawnHologram(Component text) {
        if (centerLocation == null || centerLocation.getWorld() == null) return;
        
        double holoY = plugin.getConfig().getDouble("hologram.height-offset", 1.2);
        Location holoLoc = centerLocation.clone().add(0, holoY, 0);

        hologramStand = centerLocation.getWorld().spawn(holoLoc, ArmorStand.class);
        hologramStand.setGravity(false);
        hologramStand.setCanPickupItems(false);
        hologramStand.setCustomNameVisible(true);
        hologramStand.customName(text);
        hologramStand.setVisible(false);
        hologramStand.setMarker(true);
    }

    public void remove() {
        removeRouletteRing();
        if (hologramStand != null && hologramStand.isValid()) {
            hologramStand.remove();
        }
    }
}
