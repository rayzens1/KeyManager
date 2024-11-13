package fr.keymanager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandesGestionGroupes implements CommandExecutor, Listener {

    private final KeyManager plugin;

    public CommandesGestionGroupes(KeyManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Map<String, List<String>> groups = plugin.getGroups();

        switch (label.toLowerCase()) {
            case "creategroup":
                if (args.length != 1) return false;
                String groupName = args[0];

                if (!groups.containsKey(groupName)) {
                    groups.put(groupName, new ArrayList<>());
                    plugin.saveGroups();
                    sender.sendMessage("Groupe " + groupName + " créé.");
                } else {
                    sender.sendMessage("Ce groupe existe déjà.");
                }
                return true;

            case "addblock":
                if (args.length != 1 || !(sender instanceof Player)) return false;
                groupName = args[0];

                if (groups.containsKey(groupName)) {
                    Player player = (Player) sender;
                    Block block = player.getTargetBlockExact(5);

                    if (block != null) {
                        String blockPosition = formatBlockPosition(block);
                        List<String> groupBlocks = groups.get(groupName);

                        if (!groupBlocks.contains(blockPosition)) {
                            groupBlocks.add(blockPosition);
                            plugin.saveGroups();
                            player.sendMessage("Bloc ajouté au groupe " + groupName);
                        } else {
                            player.sendMessage("Le bloc est déjà dans ce groupe.");
                        }
                    } else {
                        player.sendMessage("Aucun bloc ciblé.");
                    }
                } else {
                    sender.sendMessage("Le groupe " + groupName + " n'existe pas.");
                }
                return true;

            case "removeblock":
                if (args.length != 1 || !(sender instanceof Player)) return false;
                groupName = args[0];

                if (groups.containsKey(groupName)) {
                    Player player = (Player) sender;
                    Block block = player.getTargetBlockExact(5);

                    if (block != null) {
                        String blockPosition = formatBlockPosition(block);
                        List<String> groupBlocks = groups.get(groupName);

                        if (groupBlocks.contains(blockPosition)) {
                            groupBlocks.remove(blockPosition);
                            plugin.saveGroups();
                            player.sendMessage("Bloc retiré du groupe " + groupName);
                        } else {
                            player.sendMessage("Le bloc n'est pas dans ce groupe.");
                        }
                    } else {
                        player.sendMessage("Aucun bloc ciblé.");
                    }
                } else {
                    sender.sendMessage("Le groupe " + groupName + " n'existe pas.");
                }
                return true;

            case "givekey":
                if (args.length != 1 || !(sender instanceof Player)) return false;
                groupName = args[0];

                if (groups.containsKey(groupName)) {
                    String id = plugin.getConfig().getString("key_item.id");
                    int customModelData = plugin.getConfig().getInt("key_item.custom_model_data");
                    String name = plugin.getConfig().getString("key_item.name");
                    Player player = (Player) sender;
                    Material material = Material.getMaterial(id.toUpperCase()); // Obtenir le type d'item à partir de l'id
                    if (material == null) {
                        player.sendMessage(ChatColor.RED+"L'ID de l'item est invalide ou n'existe pas.");
                        plugin.getLogger().warning("L'ID de l'item est invalide ou n'existe pas.");
                        return false;
                    }
                    ItemStack feather = new ItemStack(material);
                    ItemMeta meta = feather.getItemMeta();
                    meta.setCustomModelData(customModelData);
                    meta.setDisplayName(name);
                    meta.setLore(Collections.singletonList(groupName));
                    feather.setItemMeta(meta);

                    player.getInventory().addItem(feather);
                    player.sendMessage("Key donnée pour le groupe " + groupName);
                } else {
                    sender.sendMessage("Le groupe " + groupName + " n'existe pas.");
                }
                return true;
            case "listgroup":
                Player player = (Player) sender;

                // Vérifier si des groupes existent
                if (groups.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Il n'y a actuellement aucun groupe.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Liste des groupes:");

                    // Afficher chaque groupe et ses positions
                    for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                        String nameGroup = entry.getKey();
                        player.sendMessage("- "+ChatColor.YELLOW + nameGroup);
                    }
                }
                return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Location blockLocation = event.getClickedBlock().getLocation();
        String locationKey = formatLocation(blockLocation);

        // Vérifier si la position du bloc est dans un groupe
        String groupName = null;
        for (Map.Entry<String, List<String>> entry : plugin.getGroups().entrySet()) {
            if (entry.getValue().contains(locationKey)) {
                groupName = entry.getKey();
                break;
            }
        }

        if (groupName == null) return; // Le bloc n'est dans aucun groupe

        // Vérifier si le joueur a une plume "key" avec le bon nom de groupe dans le lore
        if (!hasCorrectKey(player, groupName) && !player.hasPermission("plugin.bypassdoor")) {
            player.sendMessage(ChatColor.RED + "Vous ne pouvez pas interagir avec ce bloc sans la clé du groupe " + groupName + ".");
            event.setCancelled(true);
        }
    }

    private boolean hasCorrectKey(Player player, String groupName) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand(); // Obtenir l'item tenu en main

        String id = plugin.getConfig().getString("key_item.id");
        Material material = Material.getMaterial(id.toUpperCase()); // Obtenir le type d'item à partir de l'id

        if (itemInHand != null && itemInHand.getType() == material && itemInHand.hasItemMeta()) {
            ItemMeta meta = itemInHand.getItemMeta();
            // Vérifier que l'item a le nom "key" et le groupe correct dans le lore
            return meta.hasLore() && meta.getLore().contains(groupName);
        }
        return false;
    }

    // Utilitaire pour formater la position du bloc en chaîne
    private String formatBlockPosition(Block block) {
        return block.getX() + ":" + block.getY() + ":" + block.getZ() + ":" + block.getWorld().getName();
    }
    private String formatLocation(Location location) {
        return location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ() + ":" + location.getWorld().getName();
    }
}
