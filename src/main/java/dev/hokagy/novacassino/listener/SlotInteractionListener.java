package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.machine.SlotMachineTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

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

        // Проверяем нажатие на рычаг или кнопку
        if (clickedBlock.getType() == Material.LEVER || clickedBlock.getType().name().endsWith("_BUTTON")) {
            Player player = event.getPlayer();

            BlockFace facing = BlockFace.NORTH;
            if (clickedBlock.getBlockData() instanceof Directional directional) {
                facing = directional.getFacing();
            }

            // Вычисляем вектор "Назад" (за стекло) и вектор "Вправо" (для построения ширины 3х3)
            Vector backVector = facing.getOppositeFace().getDirection();
            Vector rightVector = new Vector(-backVector.getZ(), 0, backVector.getX());

            // Точка старта (Нижний Левый угол сетки 3x3 за стеклом)
            // 1 блок вверх, 2 блока назад (пропуская стекло) и 1 блок влево
            Location startDisplayLoc = clickedBlock.getLocation()
                    .add(0, 1, 0)
                    .add(backVector.clone().multiply(2))
                    .subtract(rightVector.clone());

            // Запускаем безопасную анимацию
            new SlotMachineTask(plugin, player, startDisplayLoc, rightVector, 100.0).runTaskTimer(plugin, 0L, 2L);
        }
    }
}
