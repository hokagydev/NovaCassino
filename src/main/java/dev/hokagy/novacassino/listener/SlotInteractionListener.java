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
     * Блокировка кликов ПКМ по ArmorStand'ам и стойкам около автоматов SLOTS.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ArmorStand)) return;

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if ("SLOTS".equalsIgnoreCase(station.getType())) {
                Location loc = station.getCenterLocation();
                if (loc != null && loc.getWorld() != null && loc.getWorld().equals(clicked.getWorld()) 
                        && loc.distance(clicked.getLocation()) <= 5.0) {
                    // Полностью блокируем клик, чтобы не вызывались никакие GUI
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Перехват кликов по блокам вокруг автомата SLOTS.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLeverPull(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        CasinoStation matchedStation = null;

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if ("SLOTS".equalsIgnoreCase(station.getType())) {
                Location loc = station.getCenterLocation();
                if (loc != null && loc.getWorld() != null && loc.getWorld().equals(clickedBlock.getWorld())
                        && loc.distance(clickedBlock.getLocation()) <= 5.0) {
                    matchedStation = station;
                    break;
                }
            }
        }

        // Если клик произошел около автомата SLOTS
        if (matchedStation != null) {
            // ВСЕГДА отменяем стандартный клик и открытие чужих GUI вокруг слотов!
            event.setCancelled(true);

            // Реакция на старт работы автомата только при клике по рычагу или кнопке
            if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType().name().endsWith("_BUTTON")) {
                if (spinningStations.contains(matchedStation.getId())) {
                    return;
                }

                if (matchedStation.getDisplayStart() != null && matchedStation.getDisplayEnd() != null) {
                    Player player = event.getPlayer();
                    double betAmount = plugin.getConfig().getDouble("slots.default_bet", 100.0);
                    SlotsAnimation animation = new SlotsAnimation(plugin, player, matchedStation.getDisplayStart(), matchedStation.getDisplayEnd(), matchedStation.getId(), betAmount);
                    animation.start();
                }
            }
        }
    }
}
