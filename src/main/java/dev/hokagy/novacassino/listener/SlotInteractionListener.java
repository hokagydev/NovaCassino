package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.machine.SlotsAnimation;
import dev.hokagy.novacassino.model.CasinoStation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashSet;
import java.util.Set;

public class SlotInteractionListener implements Listener {

    public static final Set<Integer> spinningStations = new HashSet<>();
    private final NovaCassino plugin;

    public SlotInteractionListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    /**
     * Блокировка кликов ПКМ по ArmorStand'ам (стойкам, голограммам слотов), 
     * чтобы не срабатывали сторонние действия вокруг автомата SLOTS.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ArmorStand)) return;

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            Location loc = station.getCenterLocation();
            if (loc != null && loc.getWorld().equals(clicked.getWorld()) && loc.distance(clicked.getLocation()) <= 4.0) {
                if ("SLOTS".equalsIgnoreCase(station.getType())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Запуск автомата SLOTS по нажатию на рычаг или кнопку.
     * Абсолютно блокирует любые открытия меню.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLeverPull(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();

        CasinoStation matchedStation = null;
        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if ("SLOTS".equalsIgnoreCase(station.getType())) {
                Location loc = station.getCenterLocation();
                if (loc != null && loc.getWorld().equals(clickedBlock.getWorld())
                        && loc.distance(clickedBlock.getLocation()) <= 5.0) {
                    matchedStation = station;
                    break;
                }
            }
        }

        if (matchedStation == null) return;

        // Отменяем стандартное взаимодействие и открывание GUI
        event.setCancelled(true);

        // Реагируем только на нажатие рычага или кнопки
        if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType().name().endsWith("_BUTTON")) {
            if (spinningStations.contains(matchedStation.getId())) {
                return;
            }

            if (matchedStation.getDisplayStart() != null && matchedStation.getDisplayEnd() != null) {
                double betAmount = plugin.getConfig().getDouble("slots.default_bet", 100.0);
                SlotsAnimation animation = new SlotsAnimation(plugin, player, matchedStation.getDisplayStart(), matchedStation.getDisplayEnd(), matchedStation.getId(), betAmount);
                animation.start();
            }
        }
    }
}
