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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Модель игровой станции (слоты / рулетка).
 * Исправления:
 *  - предотвращение утечек сущностей (очистка перед спавном)
 *  - установка setSmall(true) и setMarker(true) для корректного отображения и отсутствия хитбоксов
 *  - потокобезопасная коллекция для списков стоек
 */
public class CasinoStation {

    private final NovaCassino plugin;
    private final int id;
    private final String type;
    private final Location centerLocation;
    private double radius;

    private Location displayStart;
    private Location displayEnd;

    // CopyOnWriteArrayList безопаснее при одновременной итерации/изменении из разных точек кода
    private final List<ArmorStand> rouletteStands = new CopyOnWriteArrayList<>();
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

    /**
     * Создаёт круг маленьких ArmorStand с "блоками" на голове.
     * Перед спавном всегда удаляются старые стойки, чтобы избежать дублирования сущностей.
     */
    public void spawnRouletteRing() {
        // Удаляем предыдущие стойки (если они есть)
        removeRouletteRing();

        if (!"ROULETTE".equalsIgnoreCase(type) || centerLocation == null || centerLocation.getWorld() == null) {
            return;
        }

        int totalSlots = plugin.getConfig().getInt("roulette.total-slots", 37);
        double yOffset = plugin.getConfig().getDouble("roulette.y-offset", -0.7);

        // Безопасный парсинг материалов (на случай опечатки в конфиге)
        Material zeroMat = safeMaterial(plugin.getConfig().getString("roulette.blocks.zero", "EMERALD_BLOCK"), Material.EMERALD_BLOCK);
        Material evenMat = safeMaterial(plugin.getConfig().getString("roulette.blocks.even", "BLACK_CONCRETE"), Material.BLACK_CONCRETE);
        Material oddMat = safeMaterial(plugin.getConfig().getString("roulette.blocks.odd", "RED_CONCRETE"), Material.RED_CONCRETE);

        for (int i = 0; i < totalSlots; i++) {
            try {
                double angle = 2 * Math.PI * i / totalSlots;
                double x = centerLocation.getX() + radius * Math.cos(angle);
                double z = centerLocation.getZ() + radius * Math.sin(angle);

                Location loc = new Location(centerLocation.getWorld(), x, centerLocation.getY() + yOffset, z);

                // Спавним ArmorStand (предполагается, что вызов в основном потоке)
                ArmorStand stand = centerLocation.getWorld().spawn(loc, ArmorStand.class);
                // Настройки для визуализации "блока" на голове и отсутствия хитбокса
                stand.setGravity(false);
                stand.setCanPickupItems(false);
                stand.setVisible(false);
                stand.setSmall(true);    // компактная стойка — блок не парит
                stand.setMarker(true);   // убирает хитбокс, не мешает кликам
                stand.setInvulnerable(true);

                Material headMaterial = (i == 0) ? zeroMat : (i % 2 == 0 ? evenMat : oddMat);
                if (stand.getEquipment() != null && headMaterial != null) {
                    stand.getEquipment().setHelmet(new ItemStack(headMaterial));
                }

                rouletteStands.add(stand);
            } catch (Exception ex) {
                // Логируем, но не ломаем цикл — продолжаем создавать остальные слоты
                plugin.getLogger().warning("Ошибка при спавне стойки рулетки: " + ex.getMessage());
            }
        }
    }

    /**
     * Безопасное удаление всех стоек (и очистка коллекции).
     */
    public void removeRouletteRing() {
        // Работать по копии, т.к. список — CopyOnWrite, но на всякий случай
        for (ArmorStand stand : new ArrayList<>(rouletteStands)) {
            try {
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
            } catch (Exception ignored) {}
        }
        rouletteStands.clear();
    }

    /**
     * Обновляет текст голограммы (если не существует — создаёт).
     */
    public void updateHologram(Component text) {
        if (hologramStand == null || !hologramStand.isValid()) {
            spawnHologram(text);
        } else {
            try {
                hologramStand.customName(text);
            } catch (Exception ex) {
                plugin.getLogger().warning("Не удалось обновить голограмму: " + ex.getMessage());
            }
        }
    }

    /**
     * Сбрасывает голограмму на дефолтные строки из messages.yml.
     */
    public void resetHologram() {
        List<String> lines;
        if ("SLOTS".equalsIgnoreCase(type)) {
            lines = plugin.getMessagesConfig().getStringList("hologram.slots_idle");
        } else {
            lines = plugin.getMessagesConfig().getStringList("hologram.roulette_idle");
        }

        if (lines == null || lines.isEmpty()) {
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

    /**
     * Создаёт голограмму (ArmorStand с кастомным именем). Устанавливает marker и small.
     */
    private void spawnHologram(Component text) {
        if (centerLocation == null || centerLocation.getWorld() == null) return;

        double holoY = plugin.getConfig().getDouble("hologram.height-offset", 1.2);
        Location holoLoc = centerLocation.clone().add(0, holoY, 0);

        try {
            // Если голограмма уже есть — удаляем и создаём заново (более предсказуемое поведение)
            if (hologramStand != null && hologramStand.isValid()) {
                hologramStand.remove();
            }
            hologramStand = centerLocation.getWorld().spawn(holoLoc, ArmorStand.class);
            hologramStand.setGravity(false);
            hologramStand.setCanPickupItems(false);
            hologramStand.setCustomNameVisible(true);
            hologramStand.customName(text);
            hologramStand.setVisible(false);
            hologramStand.setMarker(true);
            hologramStand.setSmall(true);
            hologramStand.setInvulnerable(true);
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось заспавнить голограмму: " + ex.getMessage());
        }
    }

    /**
     * Полное удаление всех сущностей, связанных со станцией.
     */
    public void remove() {
        removeRouletteRing();
        try {
            if (hologramStand != null && hologramStand.isValid()) {
                hologramStand.remove();
            }
        } catch (Exception ignored) {}
        hologramStand = null;
    }

    private Material safeMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ex) {
            plugin.getLogger().warning("Неверный материал в конфиге: '" + name + "'. Используется " + fallback.name());
            return fallback;
        }
    }
}
