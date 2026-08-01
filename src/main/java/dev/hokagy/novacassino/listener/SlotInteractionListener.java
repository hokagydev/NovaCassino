package dev.hokagy.novacassino.listener;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.command.BetCommand;
import dev.hokagy.novacassino.hook.VaultHook;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashSet;
import java.util.Set;

public class SlotInteractionListener implements Listener {

    private final NovaCassino plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Публичный сет для отслеживания работающих автоматов (используется в SlotsAnimation и SlotMachineTask)
    public static final Set<Integer> spinningStations = new HashSet<>();

    public SlotInteractionListener(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();

        for (CasinoStation station : plugin.getCasinoManager().getStations().values()) {
            // Работаем только со станциями типа SLOTS
            if (!"SLOTS".equalsIgnoreCase(station.getType())) {
                continue;
            }

            if (isBlockInStationArea(clickedBlock.getLocation(), station)) {
                event.setCancelled(true);

                // Проверяем, запущен ли автомат прямо сейчас
                if (spinningStations.contains(station.getId())) {
                    player.sendMessage(miniMessage.deserialize("<red>Этот автомат уже крутится! Подождите завершения.</red>"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // 1. Проверяем, введена ли ставка командой /bet
                if (!BetCommand.hasBet(player.getUniqueId())) {
                    player.sendMessage(miniMessage.deserialize("<red>❌ Сначала укажите сумму ставки командой: <yellow>/bet <сумма></yellow></red>"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                double betAmount = BetCommand.getBet(player.getUniqueId());

                // 2. Проверяем баланс игрока через Vault
                if (VaultHook.hasEconomy()) {
                    if (VaultHook.getEconomy().getBalance(player) < betAmount) {
                        player.sendMessage(miniMessage.deserialize("<red>У вас недостаточно средств! Требуется: <gold>" + betAmount + "</gold> монет.</red>"));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    // Снимаем деньги со счета игрока
                    VaultHook.getEconomy().withdrawPlayer(player, betAmount);
                }

                // 3. Запуск автомата
                player.sendMessage(miniMessage.deserialize("<green>🎰 Ставка <gold>" + betAmount + "</gold> монет принята! Запуск автомата...</green>"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

                // Очищаем ставку игрока после использования
                BetCommand.clearBet(player.getUniqueId());

                // TODO: Здесь вызывается твоя функция старта анимации вращения слотов,
                // которая сама добавит station.getId() в spinningStations и удалит по окончании.

                break;
            }
        }
    }

    private boolean isBlockInStationArea(Location loc, CasinoStation station) {
        Location start = station.getDisplayStart();
        Location end = station.getDisplayEnd();

        if (start != null && end != null && start.getWorld().equals(loc.getWorld())) {
            int minX = Math.min(start.getBlockX(), end.getBlockX());
            int maxX = Math.max(start.getBlockX(), end.getBlockX());
            int minY = Math.min(start.getBlockY(), end.getBlockY());
            int maxY = Math.max(start.getBlockY(), end.getBlockY());
            int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
            int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

            return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                   loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                   loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
        }

        Location center = station.getCenterLocation();
        return center != null && center.getWorld().equals(loc.getWorld()) && center.distance(loc) <= 2.0;
    }
}
