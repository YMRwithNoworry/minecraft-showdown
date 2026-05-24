package alku.showdown;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModTeamManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get();
    private static final Path TEAMS_FILE = CONFIG_DIR.resolve("showdown-teams.json");

    private static final Map<String, Set<String>> teams = new LinkedHashMap<>();
    private static final Map<String, String> modToTeam = new HashMap<>();

    public static boolean createTeam(String teamName) {
        if (teams.containsKey(teamName)) {
            return false;
        }
        teams.put(teamName, new LinkedHashSet<>());
        save();
        return true;
    }

    public static boolean deleteTeam(String teamName) {
        Set<String> mods = teams.remove(teamName);
        if (mods == null) {
            return false;
        }
        for (String mod : mods) {
            modToTeam.remove(mod);
        }
        save();
        return true;
    }

    public static boolean addModToTeam(String teamName, String modId) {
        Set<String> teamMods = teams.get(teamName);
        if (teamMods == null) {
            return false;
        }
        if (modToTeam.containsKey(modId)) {
            return false;
        }
        teamMods.add(modId);
        modToTeam.put(modId, teamName);
        save();
        return true;
    }

    public static boolean removeModFromTeam(String modId) {
        String teamName = modToTeam.remove(modId);
        if (teamName == null) {
            return false;
        }
        Set<String> teamMods = teams.get(teamName);
        if (teamMods != null) {
            teamMods.remove(modId);
        }
        save();
        return true;
    }

    public static Set<String> getTeam(String teamName) {
        return teams.getOrDefault(teamName, Collections.emptySet());
    }

    public static String getTeamOfMod(String modId) {
        return modToTeam.get(modId);
    }

    public static Map<String, Set<String>> getAllTeams() {
        return Collections.unmodifiableMap(teams);
    }

    public static boolean isModInAnyTeam(String modId) {
        return modToTeam.containsKey(modId);
    }

    public static boolean teamExists(String teamName) {
        return teams.containsKey(teamName);
    }

    public static Set<String> getTeamNames() {
        return Collections.unmodifiableSet(teams.keySet());
    }

    public static void clear() {
        teams.clear();
        modToTeam.clear();
        save();
    }

    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject teamsObj = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : teams.entrySet()) {
            JsonElement arr = GSON.toJsonTree(entry.getValue());
            teamsObj.add(entry.getKey(), arr);
        }
        root.add("teams", teamsObj);

        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(TEAMS_FILE, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("Failed to save showdown teams", e);
        }
    }

    public static void load() {
        if (!Files.exists(TEAMS_FILE)) {
            return;
        }
        try {
            String json = Files.readString(TEAMS_FILE);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject teamsObj = root.getAsJsonObject("teams");
            if (teamsObj == null) return;

            teams.clear();
            modToTeam.clear();

            for (String teamName : teamsObj.keySet()) {
                Set<String> mods = new LinkedHashSet<>();
                for (JsonElement el : teamsObj.getAsJsonArray(teamName)) {
                    mods.add(el.getAsString());
                }
                teams.put(teamName, mods);
                for (String mod : mods) {
                    modToTeam.put(mod, teamName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load showdown teams", e);
        }
    }
}
