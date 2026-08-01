package dev.hokagy.novacassino.command;

import dev.hokagy.novacassino.NovaCassino;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Хранилище активных ставок игроков (UUID игрока -> Сумма)
    private static final Map<UUID, Double> playerBets = new HashMap<>();

    public BetCommand(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage.deserialize("<red>Команда доступна только для игроков!</red>"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(miniMessage.deserialize("<red>Использование: /bet <сумма></red>"));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[0]);
            double minBet = plugin.getConfig().getDouble("custom_bet.min", 10.0);
            double maxBet = plugin.getConfig().getDouble("custom_bet.max", 100000.0);

            if (amount < minBet || amount > maxBet) {
                player.sendMessage(miniMessage.deserialize("<red>Ставка должна быть от <gold>" + minBet + "</gold> до <gold>" + maxBet + "</gold> монет!</red>"));
                return true;
            }

            playerBets.put(player.getUniqueId(), amount);
            player.sendMessage(miniMessage.deserialize("<green>Ставка <gold>" + amount + "</gold> монет успешно выбрана! Теперь нажмите на слот-автомат для игры.</green>"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        } catch (NumberFormatException e) {
            player.sendMessage(miniMessage.deserialize("<red>Укажите корректное число!</red>"));
        }

        return true;
    }

    public static double getBet(UUID playerUuid) {
        return playerBets.getOrDefault(playerUuid, 0.0);
    }

    public static boolean hasBet(UUID playerUuid) {
        return playerBets.containsKey(playerUuid) && playerBets.get(playerUuid) > 0;
    }

    public static void clearBet(UUID playerUuid) {
        playerBets.remove(playerUuid);
    }
}
