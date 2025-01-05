package btepl.utils.bTEPLUtils;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BlockReplacerTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        // Jeśli komenda jest wywoływana przez gracza
        if (sender instanceof Player) {
            // Jeśli użytkownik jest w trakcie wpisywania pierwszego argumentu (materiał docelowy)
            if (args.length == 1) {
                // Podpowiadanie dostępnych materiałów
                for (Material material : Material.values()) {
                    if (material.isBlock()) {
                        completions.add(material.name().toLowerCase());
                    }
                }
            }
            // Jeśli użytkownik jest w trakcie wpisywania drugiego argumentu (materiał sąsiadujący)
            else if (args.length == 2) {
                for (Material material : Material.values()) {
                    if (material.isBlock()) {
                        completions.add(material.name().toLowerCase());
                    }
                }
            }
            // Jeśli użytkownik jest w trakcie wpisywania trzeciego argumentu (długość boku)
            else if (args.length == 3) {
                completions.add("<length>");  // Tutaj możemy dodać propozycję liczby
            }
        }

        return completions;
    }
}
