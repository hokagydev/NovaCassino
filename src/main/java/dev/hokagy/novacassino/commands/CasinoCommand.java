package dev.hokagy.novacassino.command;

import dev.hokagy.novacassino.NovaCassino;
import dev.hokagy.novacassino.model.CasinoStation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
                sender.sendMessage(Component.text("Продвинутый плагин станций казино с 3D-рулетками и слотами.", NamedTextColor.GRAY));
                return true;
            }

            String sub = args[0].toLowerCase();

            switch (sub) {
                case "wand" -> {
                    if (!(sender instanceof Player player)) return onlyPlayers(sender);
                    if (!hasPermission(player, "procasino.command.admin")) return true;

                    ItemStack wand = new ItemStack(Material.WOODEN_AXE);
                    ItemMeta meta = wand.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("🪄 Палочка создания автомата", NamedTextColor.GOLD, TextDecoration.BOLD));
                        wand.setItemMeta(meta);
                    }
                    player.getInventory().addItem(wand);
                    player.sendMessage(Component.text("Вам выдана палочка! Нажмите ПКМ/ЛКМ по блоку для выделения.", NamedTextColor.GREEN));
                }
                case "add" -> {
                    if (!(sender instanceof Player player)) return onlyPlayers(sender);
                    if (!hasPermission(player, "procasino.command.add")) return true;
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Использование: /procasino add <тип> (Пример: ROULETTE или SLOTS)", NamedTextColor.RED));
                        return true;
                    }

                    String type = args[1].toUpperCase();

                    if (type.equals("SLOTS")) {
                        if (!plugin.getSelectionManager().hasSelection(player.getUniqueId())) {
                            player.sendMessage(Component.text("Сначала выделите экран палочкой (/procasino wand)!", NamedTextColor.RED));
                            return true;
                        }

                        Location pos1 = plugin.getSelectionManager().getPos1(player.getUniqueId());
                        Location pos2 = plugin.getSelectionManager().getPos2(player.getUniqueId());
                        Location displayStart = plugin.getSelectionManager().getMinCorner(player.getUniqueId());

                        CasinoStation station = plugin.getCasinoManager().createStation(type, displayStart);
                        station.setDisplayStart(pos1);
                        station.setDisplayEnd(pos2);
                        station.updateHologram(Component.text("🎰 СЛОТ-АВТОМАТ 🎰", NamedTextColor.GOLD, TextDecoration.BOLD));
                        plugin.getCasinoManager().saveStations();

                        player.sendMessage(Component.text("Игровой автомат #" + station.getId() + " (" + station.getType() + ") привязан к выделенной сетке!", NamedTextColor.GREEN));

                    } else if (type.equals("ROULETTE")) {
                        Location loc = player.getLocation();
                        CasinoStation station = plugin.getCasinoManager().createStation(type, loc);
                        station.spawnRouletteRing(); // Заспавнить круг из маленьких блоков
                        station.resetHologram();     // Заспавнить голограмму
                        plugin.getCasinoManager().saveStations();

                        player.sendMessage(Component.text("Станция казино #" + station.getId() + " (ROULETTE) создана!", NamedTextColor.GREEN));
                    } else {
                        CasinoStation station = plugin.getCasinoManager().createStation(type, player.getLocation());
                        plugin.getCasinoManager().saveStations();
                        player.sendMessage(Component.text("Станция казино #" + station.getId() + " (" + station.getType() + ") создана!", NamedTextColor.GREEN));
                    }
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
                        st.spawnRouletteRing(); // Обновить круг из маленьких блоков
                        plugin.getCasinoManager().saveStations();
                        sender.sendMessage(Component.text("Радиус станции #" + id + " изменен на " + radius, NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Станция не найдена!", NamedTextColor.RED));
                    }
                }
                case "reload" -> {
                    if (!hasPermission(sender, "procasino.command.reload")) return true;
                    plugin.reloadAllConfigs();
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
        sender.sendMessage(Component.text("/procasino help ", NamedTextColor.YELLOW).append(Component.text("- Показать справку", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino wand ", NamedTextColor.YELLOW).append(Component.text("- Получить топорик выделения", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino add <тип> ", NamedTextColor.YELLOW).append(Component.text("- Создать станцию (ROULETTE / SLOTS)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino delete <id> ", NamedTextColor.YELLOW).append(Component.text("- Удалить станцию", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino teleport <id> ", NamedTextColor.YELLOW).append(Component.text("- Телепорт к станции", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino list ", NamedTextColor.YELLOW).append(Component.text("- Список станций", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino set <id> radius <радиус> ", NamedTextColor.YELLOW).append(Component.text("- Изменить радиус", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/procasino reload ", NamedTextColor.YELLOW).append(Component.text("- Перезагрузка конфигов", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("═══════════════════════════════════════════════", NamedTextColor.GOLD));
    }

    private boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(Component.text("У вас нет прав (" + perm + ")!", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean onlyPlayers(CommandSender sender) {
        sender.sendMessage(Component.text("Команда только для игроков!", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterCompletions(List.of("help", "about", "wand", "add", "delete", "teleport", "list", "set", "reload"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                return filterCompletions(List.of("ROULETTE", "SLOTS"), args[1]);
            }
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("set")) {
                List<String> ids = plugin.getCasinoManager().getStations().keySet().stream().map(String::valueOf).collect(Collectors.toList());
                return filterCompletions(ids, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filterCompletions(List.of("radius"), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("radius")) {
            return filterCompletions(List.of("2.5", "3.5", "5.0"), args[3]);
        }

        return new ArrayList<>();
    }

    private List<String> filterCompletions(List<String> list, String input) {
        return list.stream().filter(item -> item.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
