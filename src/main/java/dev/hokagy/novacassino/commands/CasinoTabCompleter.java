package dev.hokagy.novacassino.commands;

import dev.hokagy.novacassino.NovaCassino;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CasinoTabCompleter implements TabCompleter {

    private final NovaCassino plugin;

    public CasinoTabCompleter(NovaCassino plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("about", "add", "delete", "teleport", "list", "set", "reload", "help");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add")) {
                completions.add("ROULETTE");
            } else if (sub.equals("delete") || sub.equals("teleport") || sub.equals("set")) {
                plugin.getCasinoManager().getStations().keySet().forEach(id -> completions.add(String.valueOf(id)));
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if ("radius".startsWith(args[2].toLowerCase())) {
                completions.add("radius");
            }
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[2].equalsIgnoreCase("radius")) {
            completions.addAll(List.of("2.5", "3.5", "4.5", "5.5"));
            return completions;
        }

        return completions;
    }
}
