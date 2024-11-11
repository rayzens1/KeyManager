package fr.keymanager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KeyManager extends JavaPlugin {

    private final File groupsFile = new File(getDataFolder(), "groups.json");
    private final Gson gson = new Gson();
    private Map<String, List<String>> groups = new HashMap<>();

    @Override
    public void onEnable() {
        // Crée le dossier du plugin si nécessaire
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        saveDefaultConfig(); // Crée le fichier config.yml s'il n'existe pas

        // Charge les groupes depuis le fichier JSON
        loadGroups();

        // Enregistrement des commandes et des événements
        getCommand("creategroup").setExecutor(new CommandesGestionGroupes(this));
        getCommand("addblock").setExecutor(new CommandesGestionGroupes(this));
        getCommand("removeblock").setExecutor(new CommandesGestionGroupes(this));
        getCommand("givekey").setExecutor(new CommandesGestionGroupes(this));
        getCommand("listgroup").setExecutor(new CommandesGestionGroupes(this));
        getServer().getPluginManager().registerEvents(new CommandesGestionGroupes(this), this);
    }

    @Override
    public void onDisable() {
        saveGroups();
    }

    public Map<String, List<String>> getGroups() {
        return groups;
    }

    public void saveGroups() {
        try (Writer writer = new FileWriter(groupsFile)) {
            gson.toJson(groups, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadGroups() {
        if (groupsFile.exists()) {
            try (Reader reader = new FileReader(groupsFile)) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                groups = gson.fromJson(reader, type);
                if (groups == null) groups = new HashMap<>();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            saveGroups();  // Crée le fichier s'il n'existe pas
        }
    }


}
