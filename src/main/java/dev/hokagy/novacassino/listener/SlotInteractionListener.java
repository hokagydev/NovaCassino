package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.gui.RouletteGUI;
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

public class StationInteractListener implements Listener {

    private final NovaCassino plugin;

    public StationInteractListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    /**
     * Блокировка кликов ПКМ по ArmorStand'ам (стойкам, голограммам, блокам рулетки/слотов)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ArmorStand)) return;

        // Ищем, принадлежит ли этот ArmorStand какой-либо станции
        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            Location loc = station.getCenterLocation();
            if (loc != null && loc.getWorld().equals(clicked.getWorld()) && loc.distance(clicked.getLocation()) <= 4.0) {
                
                // Если кликнули по автомату SLOTS — полностью глушим клик по стойкам
                if ("SLOTS".equalsIgnoreCase(station.getType())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Обработка ПКМ по блокам / рычагам / кнопкам
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();

        // Поиск ближайшей станции
        CasinoStation targetStation = null;
        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            Location loc = station.getCenterLocation();
            if (loc != null && loc.getWorld().equals(clickedBlock.getWorld())) {
                if (loc.distance(clickedBlock.getLocation()) <= 4.0) {
                    targetStation = station;
                    break;
                }
            }
        }

        if (targetStation == null) return;

        // СТРОГОЕ РАЗДЕЛЕНИЕ ЛОГИКИ ТИПОВ СТАНЦИЙ
        if ("SLOTS".equalsIgnoreCase(targetStation.getType())) {
            // Запрещаем абсолютно любое открытие меню
            event.setCancelled(true);

            // Регистрируем запуск только если нажат именно рычаг или кнопка
            if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType().name().endsWith("_BUTTON")) {
                if (SlotInteractionListener.spinningStations.contains(targetStation.getId())) {
                    return;
                }

                if (targetStation.getDisplayStart() != null && targetStation.getDisplayEnd() != null) {
                    double betAmount = plugin.getConfig().getDouble("slots.default_bet", 100.0);
                    SlotsAnimation animation = new SlotsAnimation(plugin, player, targetStation.getDisplayStart(), targetStation.getDisplayEnd(), targetStation.getId(), betAmount);
                    animation.start();
                }
            }
        } else if ("ROULETTE".equalsIgnoreCase(targetStation.getType())) {
            // Только для рулетки открываем GUI!
            event.setCancelled(true);
            RouletteGUI.open(plugin, player, targetStation);
        }
    }
}
