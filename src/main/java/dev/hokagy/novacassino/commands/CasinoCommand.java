package dev.hokagy.novacassino.commands;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.model.CasinoStation;
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
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("about")) {
                if (!hasPermission(sender, "procasino.command.about")) return true;
                sender.sendMessage(Component.text("--- NovaCassino v1.0.0 ---", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Автор: Hokagydev", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Продвинутый плагин станций казино.", NamedTextColor.GRAY));
                return true;
            }

            String sub = args[0].toLowerCase();

            switch (sub) {
                case "add" -> {
                    if (!(sender instanceof Player player)) {
                        return onlyPlayers(sender);
                    }
                    if (!hasPermission(player, "procasino.command.add")) return true;
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Использование: /procasino add <тип> (Пример: ROULETTE)", NamedTextColor.RED));
                        return true;
                    }
                    if (plugin.getCasinoManager() == null) {
                        player.sendMessage(Component.text("Ошибка: Менеджер казино не инициализирован!", NamedTextColor.RED));
                        return true;
                    }
                    CasinoStation station = plugin.getCasinoManager().createStation(args[1], player.getLocation());
                    player.sendMessage(Component.text("Станция казино #" + station.getId() + " (" + station.getType() + ") успешно создана!", NamedTextColor.GREEN));
                }
                case "delete" -> {
                    if (!hasPermission(sender, "procasino.command.delete")) return true;
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Использование: /procasino delete <id>", NamedTextColor.RED));
                        return true;
                    }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (plugin.getCasinoManager().deleteStation(id)) {
                            sender.sendMessage(Component.text("Станция #" + id + " удалена!", NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Станция с ID #" + id + " не найдена!", NamedTextColor.RED));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("ID должен быть числом!", NamedTextColor.RED));
                    }
                }
                case "teleport" -> {
                    if (!(sender instanceof Player player)) {
                        return onlyPlayers(sender);
                    }
                    if (!hasPermission(player, "procasino.command.teleport")) return true;
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Использование: /procasino teleport <id>", NamedTextColor.RED));
                        return true;
                    }
                    try {
                        int id = Integer.parseInt(args[1]);
                        CasinoStation station = plugin.getCasinoManager().getStation(id);
                        if (station != null) {
                            player.teleport(station.getCenterLocation().clone().add(0, 1, 0));
                            player.sendMessage(Component.text("Телепортирование к станции #" + id, NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("Станция не найдена!", NamedTextColor.RED));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("ID должен быть числом!", NamedTextColor.RED));
                    }
                }
                case "list" -> {
                    if (!hasPermission(sender, "procasino.command.list")) return true;
                    sender.sendMessage(Component.text("--- Список станций NovaCassino ---", NamedTextColor.GOLD));
                    if (plugin.getCasinoManager() == null || plugin.getCasinoManager().getStations().isEmpty()) {
                        sender.sendMessage(Component.text("Нет созданных станций.", NamedTextColor.GRAY));
                        return true;
                    }
                    plugin.getCasinoManager().getStations().forEach((id, st) -> {
                        sender.sendMessage(Component.text("ID: " + id + " | Тип: " + st.getType() + " | Радиус: " + st.getRadius(), NamedTextColor.YELLOW));
                    });
                }
                case "set" -> {
                    if (!hasPermission(sender, "procasino.command.set")) return true;
                    if (args.length < 4 || !args[2].equalsIgnoreCase("radius")) {
                        sender.sendMessage(Component.text("Использование: /procasino set <id> radius <radius>", NamedTextColor.RED));
                        return true;
                    }
                    try {
                        int id = Integer.parseInt(args[1]);
                        double radius = Double.parseDouble(args[3]);
                        if (radius < 2.0 || radius > 7.0) {
                            sender.sendMessage(Component.text("Радиус должен быть в диапазоне 2-7!", NamedTextColor.RED));
                            return true;
                        }
                        CasinoStation st = plugin.getCasinoManager().getStation(id);
                        if (st != null) {
                            st.setRadius(radius);
                            plugin.getCasinoManager().saveStations();
                            sender.sendMessage(Component.text("Радиус станции #" + id + " изменен на " + radius, NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Станция не найдена!", NamedTextColor.RED));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Ошибка в аргументах числа!", NamedTextColor.RED));
                    }
                }
                case "reload" -> {
                    if (!hasPermission(sender, "procasino.command.reload")) return true;
                    plugin.reloadConfig();
                    plugin.getCasinoManager().loadStations();
                    sender.sendMessage(Component.text("NovaCassino перезагружен!", NamedTextColor.GREEN));
                }
                default -> sender.sendMessage(Component.text("Неизвестная подкоманда. Наберите /procasino about", NamedTextColor.RED));
            }
        } catch (Exception e) {
            sender.sendMessage(Component.text("Произошла ошибка при выполнении команды. Проверьте консоль сервера.", NamedTextColor.RED));
            plugin.getLogger().severe("Ошибка при выполнении /procasino:");
            e.printStackTrace();
        }

        return true;
    }

    private boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(Component.text("У вас нет прав (" + perm + ") для выполнения этой команды!", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean onlyPlayers(CommandSender sender) {
        sender.sendMessage(Component.text("Эта команда доступна только игрокам!", NamedTextColor.RED));
        return true;
    }
}
