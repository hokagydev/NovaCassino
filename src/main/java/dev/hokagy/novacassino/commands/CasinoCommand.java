package dev.hokagy.novacassino.commands;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.roulette.RouletteAnimation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CasinoCommand implements CommandExecutor {

    private final NovaCassino plugin;

    public CasinoCommand(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда только для игроков!");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("play")) {
            player.sendMessage(Component.text("Использование: /casino play <ставка>", NamedTextColor.YELLOW));
            return true;
        }

        try {
            double bet = Double.parseDouble(args[1]);
            if (bet <= 0) {
                player.sendMessage(Component.text("Ставка должна быть больше 0!", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text(" Рулетка запускается! Ваша ставка: ", NamedTextColor.GOLD)
                    .append(Component.text(bet + "$", NamedTextColor.GREEN)));

            // TODO: Списать деньги у игрока (Vault API)
            
            // Запуск красивой 3D рулетки
            new RouletteAnimation(plugin, player, bet).start();

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text(" Укажите корректную сумму ставки!", NamedTextColor.RED));
        }

        return true;
    }
}
