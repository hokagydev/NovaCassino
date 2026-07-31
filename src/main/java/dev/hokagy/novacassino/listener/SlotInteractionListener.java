package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.machine.SlotMachineTask;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

public class SlotInteractionListener implements Listener {

    public static final Set<Integer> spinningStations = new HashSet<>();
    private final NovaCassino plugin;

    public SlotInteractionListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLeverPull(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType().name().endsWith("_BUTTON")) {
            Player player = event.getPlayer();

            CasinoStation matchedStation = null;
            for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
                if ("SLOTS".equalsIgnoreCase(station.getType())) {
                    if (station.getCenterLocation() != null && station.getCenterLocation().getWorld().equals(clickedBlock.getWorld())
                            && station.getCenterLocation().distance(clickedBlock.getLocation()) <= 5.0) {
                        matchedStation = station;
                        break;
                    }
                }
            }

            if (matchedStation == null || matchedStation.getDisplayStart() == null || matchedStation.getDisplayEnd() == null) {
                return;
            }

            if (spinningStations.contains(matchedStation.getId())) {
                player.sendMessage(Component.text("Этот автомат уже крутится! Дождитесь окончания.", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }

            spinningStations.add(matchedStation.getId());

            new SlotMachineTask(plugin, player, matchedStation.getDisplayStart(), matchedStation.getDisplayEnd(), matchedStation.getId(), 100.0)
                    .runTaskTimer(plugin, 0L, 2L);
        }
    }
}
