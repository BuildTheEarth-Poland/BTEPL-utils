package btepl.utils.bTEPLUtils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldedit.IncompleteRegionException;

import java.util.ArrayList;
import java.util.List;

public class BlockReplacerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("BlockReplacerPlugin enabled!");

        // Rejestracja TabCompletera
        getCommand("side").setTabCompleter(new BlockReplacerTabCompleter());
    }

    @Override
    public void onDisable() {
        getLogger().info("BlockReplacerPlugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "Only players can execute this command");
            return false;
        }

        Player player = (Player) sender;

        if (args.length != 3) { // Sprawdzenie poprawności argumentów
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "Usage: /side <target material> <new material> <length of new material>");
            return false;
        }

        Material targetMaterial;
        Material sideMaterial;
        int sideLength;

        try {
            targetMaterial = Material.valueOf(args[0].toUpperCase());
            sideMaterial = Material.valueOf(args[1].toUpperCase());
            sideLength = Integer.parseInt(args[2]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "Wrong parameter");
            return false;
        }

        // Pobierz plugin WorldEdit
        WorldEditPlugin worldEditPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEditPlugin == null) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.RED + "WorldEdit was not found");
            return false;
        }

        SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);

        Region selection;
        try {
            selection = sessionManager.get(wePlayer).getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException e) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.RED + "Your selection is incomplete.");
            return false;
        }

        if (selection == null) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.RED + "No selection was found");
            return false;
        }

        // Pobierz świat i regiony
        World world = BukkitAdapter.adapt(player.getWorld());
        BlockVector3 min = selection.getMinimumPoint();
        BlockVector3 max = selection.getMaximumPoint();

        // Oblicz kierunek (NORTH, SOUTH, EAST, WEST)
        org.bukkit.block.BlockFace direction = player.getFacing();
        BlockVector3 offset = BlockVector3.ZERO;

        switch (direction) {
            case NORTH:
                offset = BlockVector3.at(0, 0, -1);
                break;
            case SOUTH:
                offset = BlockVector3.at(0, 0, 1);
                break;
            case EAST:
                offset = BlockVector3.at(1, 0, 0);
                break;
            case WEST:
                offset = BlockVector3.at(-1, 0, 0);
                break;
            default:
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "This direction is not supported.");
                return false;
        }

        // Utwórz sesję edycyjną
        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(world)
                .actor(wePlayer) // Powiązanie sesji z graczem
                .build()) {

            boolean success = false;

            // Iterowanie przez zaznaczony obszar
            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        BlockVector3 blockPosition = BlockVector3.at(x, y, z);
                        BlockState blockState = editSession.getBlock(blockPosition);

                        // Sprawdź, czy blok odpowiada materiałowi docelowemu
                        if (blockState.getBlockType().equals(BukkitAdapter.asBlockType(targetMaterial))) {
                            for (int i = 1; i <= sideLength; i++) {
                                BlockVector3 sidePosition = blockPosition.add(offset.multiply(i));
                                BlockState sideBlockState = editSession.getBlock(sidePosition);

                                // Blokuj zmianę, jeśli napotkano inny blok (nie powietrze)
                                if (!sideBlockState.getBlockType().getId().equals("minecraft:air")) {
                                    break;
                                }

                                // Ustaw nowy blok sąsiadujący
                                editSession.setBlock(sidePosition, BukkitAdapter.adapt(sideMaterial.createBlockData()));
                                success = true;
                            }
                        }
                    }
                }
            }

            if (!success) {
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "There are no blocks matching the specified criteria");
                return false;
            }

            // Zarejestruj transakcję zmian użytkownika w historii sesji WorldEdit
            if (success) {
                sessionManager.get(wePlayer).remember(editSession); // Dodanie do historii użytkownika
                editSession.commit(); // Zapisuje zmiany i przygotowuje je do cofania
            } else {
                if (editSession == null) {
                    getLogger().warning("EditSession is null!");
                } else {
                    getLogger().info("EditSession exists. Attempting to cancel...");
                    editSession.undo(editSession); // Anulowanie transakcji w przypadku braku zmian
                    editSession.close();
                }
            }
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "Blocks changed successfully");

            return true;

        } catch (Exception e) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "BTE" + ChatColor.WHITE + "" + ChatColor.BOLD + "PL " + ChatColor.GRAY + " » " + ChatColor.GRAY + "Error occurred while changing blocks");
            e.printStackTrace();
            return false;
        }
    }
}
