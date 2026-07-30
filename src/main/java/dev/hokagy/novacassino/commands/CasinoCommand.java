package dev.hokagy.novacassino.command;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CasinoCommand implements CommandExecutor, TabCompleter {

    private final NovaCassino plugin;

    public CasinoCommand(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sendHelpMenu(sender);
                return true;
            }

            if (args[0].equalsIgnoreCase("about")) {
                if (!hasPermission(sender, "procasino.command.about")) return true;
                sender.sendMessage(Component.text("--- NovaCassino v1.0.0 ---", NamedTextColor.GOLD, TextDecoration.BOLD));
                sender.sendMessage(Component.text("Автор: ", NamedTextColor.GRAY).append(Component.text("Hokagydev", NamedTextColor.YELLOW)));
                sender.sendMessage(Component.text("Продвинутый плагин станций казино с 3D-рулетками.", NamedTextColor.GRAY));
                return true;
            }

            String sub = args[0].toLowerCase();

            switch (sub) {
                case "add" -> {
                    if (!(sender instanceof Player player)) return onlyPlayers(sender);
                    if (!hasPermission(player, "procasino.command.add")) return true;
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Использование: /procasino add <тип> (Пример: ROULETTE)", NamedTextColor.RED));
                        return true;
                    }
                    CasinoStation station = plugin.getCasinoManager().createStation(args[1].toUpperCase(), player.getLocation());
                    player.sendMessage(Component.text("Станция казино #" + station.getId() + " (" + station.getType() + ") создана!", NamedTextColor.GREEN));
                }
                case "delete" -> {
                    if (!hasPermission(sender, "procasino.command.delete")) return true;
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Использование: /procasino delete <id>", NamedTextColor.RED));
                        return true;
                    }
                    int id = Integer.parseInt(args[1]);
                    if (plugin.getCasinoManager().deleteStation(id)) {
                        sender.sendMessage(Component.text("Станция #" + id + " удалена!", NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Станция #" + id + " не найдена!", NamedTextColor.RED));
                    }
                }
                case "teleport" -> {
                    if (!(sender instanceof Player player)) return onlyPlayers(sender);
                    if (!hasPermission(player, "procasino.command.teleport")) return true;
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Использование: /procasino teleport <id>", NamedTextColor.RED));
                        return true;
                    }
                    int id = Integer.parseInt(args[1]);
                    CasinoStation station = plugin.getCasinoManager().getStation(id);
                    if (station != null) {
                        player.teleport(station.getCenterLocation().clone().add(0, 1, 0));
                        player.sendMessage(Component.text("Телепортирование к станции #" + id, NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("Станция не найдена!", NamedTextColor.RED));
                    }
                }
                case "list" -> {
                    if (!hasPermission(sender, "procasino.command.list")) return true;
                    sender.sendMessage(Component.text("--- Список станций NovaCassino ---", NamedTextColor.GOLD, TextDecoration.BOLD));
                    if (plugin.getCasinoManager().getStations().isEmpty()) {
                        sender.sendMessage(Component.text("Нет созданных станций.", NamedTextColor.GRAY));
                        return true;
                    }
                    plugin.getCasinoManager().getStations().forEach((id, st) -> {
                        sender.sendMessage(Component.text("• ID: ", NamedTextColor.YELLOW)
                                .append(Component.text(id, NamedTextColor.WHITE))
                                .append(Component.text(" | Тип: ", NamedTextColor.YELLOW))
                                .append(Component.text(st.getType(), NamedTextColor.GREEN))
                                .append(Component.text(" | Радиус: ", NamedTextColor.YELLOW))
                                .append(Component.text(st.getRadius(), NamedTextColor.AQUA)));
                    });
                }
                case "set" -> {
                    if (!hasPermission(sender, "procasino.command.set")) return true;
                    if (args.length < 4 || !args[2].equalsIgnoreCase("radius")) {
                        sender.sendMessage(Component.text("Использование: /procasino set <id> radius <радиус>", NamedTextColor.RED));
                        return true;
                    }
                    int id = Integer.parseInt(args[1]);
                    double radius = Double.parseDouble(args[3]);
                    if (radius < 2.0 || radius > 7.0) {
                        sender.sendMessage(Component.text("Радиус должен быть в диапазоне от 2.0 до 7.0!", NamedTextColor.RED));
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
                }
                case "reload" -> {
                    if (!hasPermission(sender, "procasino.command.reload")) return true;
                    plugin.reloadConfig();
                    plugin.reloadMessagesConfig();
                    plugin.getCasinoManager().loadStations();
                    sender.sendMessage(Component.text("Конфигурация NovaCassino перезагружена!", NamedTextColor.GREEN));
                }
                default -> sendHelpMenu(sender);
            }
        } catch (Exception e) {
            sender.sendMessage(Component.text("Ошибка при исполнении команды. Проверьте аргументы.", NamedTextColor.RED));
        }
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(Component.text("═════════════ [ NovaCassino Help ] ═════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/procasino help ", NamedTextColor.YELLOW).append(Component.text("- Показать эту справку", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino about ", NamedTextColor.YELLOW).append(Component.text("- Информация о плагине", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino add <тип> ", NamedTextColor.YELLOW).append(Component.text("- Создать станцию (ROULETTE)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino delete <id> ", NamedTextColor.YELLOW).append(Component.text("- Удалить станцию по ID", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino teleport <id> ", NamedTextColor.YELLOW).append(Component.text("- Телепорт к станции", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino list ", NamedTextColor.YELLOW).append(Component.text("- Показать все станции", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino set <id> radius <радиус> ", NamedTextColor.YELLOW).append(Component.text("- Задать радиус (2-7)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino reload ", NamedTextColor.YELLOW).append(Component.text("- Перезагрузить плагин", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("═══════════════════════════════════════════════", NamedTextColor.GOLD));
    }

    private boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(Component.text("У вас нет прав (" + perm + ") для выполнения этой команды!", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean onlyPlayers(CommandSender sender) {
        sender.sendMessage(Component.text("Команда только для игроков!", NamedTextColor.RED));
        return true;
    }

    // 🌟 УДОБНЫЙ ТАБ-КОМПЛИТЕР
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("help", "about", "add", "delete", "teleport", "list", "set", "reload");
            return filterCompletions(subCommands, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                List<String> types = List.of("ROULETTE", "SLOTS", "WHEEL");
                return filterCompletions(types, args[1]);
            }

            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("set")) {
                // Подгружает существующие ID созданных станций
                List<String> stationIds = plugin.getCasinoManager().getStations().keySet().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                return filterCompletions(stationIds, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filterCompletions(List.of("radius"), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("radius")) {
            return filterCompletions(List.of("2.5", "3.5", "5.0"), args[3]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> list, String input) {
        return list.stream()
                .filter(item -> item.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
