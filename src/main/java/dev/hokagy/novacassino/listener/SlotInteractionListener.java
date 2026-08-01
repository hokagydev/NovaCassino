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
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
     * Блокировка ПКМ по стойкам / ArmorStand вокруг слотов.
     * Приоритет HIGHEST гарантирует, что мы перехватим клик до вызова GUI.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (isNearSlots(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityInteractGeneral(PlayerInteractEntityEvent event) {
        if (isNearSlots(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    /**
     * Перехват кликов ПКМ по всем блокам около SLOTS.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLeverPull(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        
        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();
        Location checkLoc = (clickedBlock != null) ? clickedBlock.getLocation() : player.getLocation();

        CasinoStation matchedStation = findSlotsStation(checkLoc);

        if (matchedStation != null) {
            // ЖЁСТКАЯ БЛОКИРОВКА: Глушим ВСЕ клики ПКМ в радиусе слотов, 
            // чтобы никакие внешние слушатели не открывали GUI рулетки!
            event.setCancelled(true);

            if (event.getHand() != EquipmentSlot.HAND) return;
            if (clickedBlock == null) return;

            // Запускаем автомат ТОЛЬКО если кликнули по рычагу или кнопке
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

    private boolean isNearSlots(Entity entity) {
        if (entity == null) return false;
        return findSlotsStation(entity.getLocation()) != null;
    }

    private CasinoStation findSlotsStation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            if ("SLOTS".equalsIgnoreCase(station.getType())) {
                Location center = station.getCenterLocation();
                if (center != null && center.getWorld() != null && center.getWorld().equals(loc.getWorld())) {
                    if (center.distance(loc) <= 5.0) {
                        return station;
                    }
                }
            }
        }
        return null;
    }
}
