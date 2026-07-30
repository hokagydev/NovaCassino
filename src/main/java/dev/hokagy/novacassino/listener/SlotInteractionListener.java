package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.machine.SlotMachineTask;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SlotInteractionListener implements Listener {

    private final NovaCassino plugin;

    public SlotInteractionListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLeverPull(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // Если кликнули по рычагу или кнопке автомата
        if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType().name().endsWith("_BUTTON")) {
            Player player = event.getPlayer();

            // Точка сетки 3x3 блоков за стеклом
            Block displayStart = clickedBlock.getRelative(0, 1, -1);

            new SlotMachineTask(plugin, player, displayStart.getLocation(), 100.0).runTaskTimer(plugin, 0L, 2L);
        }
    }
}
