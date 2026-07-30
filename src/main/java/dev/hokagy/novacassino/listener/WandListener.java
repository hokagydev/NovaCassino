package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final NovaCassino plugin;

    public WandListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.WOODEN_AXE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().hasDisplayName()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getSelectionManager().setPos1(player.getUniqueId(), clicked.getLocation());
            player.sendMessage(Component.text("Первая точка установлена: (" + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ() + ")", NamedTextColor.GREEN));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getSelectionManager().setPos2(player.getUniqueId(), clicked.getLocation());
            player.sendMessage(Component.text("Вторая точка установлена: (" + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ() + ")", NamedTextColor.GREEN));
        }
    }
}
