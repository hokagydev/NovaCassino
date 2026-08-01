package dev.hokagy.novacassino.command;

import dev.hokagy.novacassino.NovaCassino;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BetCommand implements CommandExecutor {

    private final NovaCassino plugin;
    // Хранилище активных ставок игроков (UUID игрока -> Сумма ставки)
    private static final Map<UUID, Double> activeBets = new HashMap<>();

    public BetCommand(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Использование: /bet <сумма>", NamedTextColor.RED));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[0]);

            if (amount <= 0) {
                player.sendMessage(Component.text("Ставка должна быть больше 0!", NamedTextColor.RED));
                return true;
            }

            // Запоминаем ставку игрока
            activeBets.put(player.getUniqueId(), amount);
            
            player.sendMessage(Component.text("Ставка ", NamedTextColor.GREEN)
                    .append(Component.text(amount + " монеток ", NamedTextColor.GOLD))
                    .append(Component.text("успешно установлена! Нажмите на любой автомат для игры.", NamedTextColor.GREEN)));
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Укажите корректное число!", NamedTextColor.RED));
        }

        return true;
    }

    /**
     * Получить текущую выбранную ставку игрока.
     */
    public static double getBet(UUID playerUuid) {
        return activeBets.getOrDefault(playerUuid, 0.0);
    }

    /**
     * Проверить, сделана ли ставка.
     */
    public static boolean hasBet(UUID playerUuid) {
        return activeBets.containsKey(playerUuid) && activeBets.get(playerUuid) > 0;
    }

    /**
     * Сбросить ставку (вызывается после того, как автомат прокрутился).
     */
    public static void clearBet(UUID playerUuid) {
        activeBets.remove(playerUuid);
    }
}
