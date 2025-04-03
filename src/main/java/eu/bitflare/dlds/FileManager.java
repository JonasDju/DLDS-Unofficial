package eu.bitflare.dlds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FileManager {

    private final DLDSPlugin plugin;
    private final File saveFile;
    private final Gson gson;

    public FileManager(DLDSPlugin plugin) {
        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), "gamestate.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveGameState() {
        GameManager gameManager = GameManager.getInstance();
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            GameState state = new GameState(
                    gameManager.getTeams(),
                    gameManager.getDragonRespawnTime(),
                    plugin.getPluginMeta().getVersion()
            );

            FileWriter writer = new FileWriter(saveFile);
            gson.toJson(state, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadGameState() {
        GameManager gameManager = GameManager.getInstance();
        if (!saveFile.exists()) {
            return;
        }

        try {
            FileReader reader = new FileReader(saveFile);
            GameState state = gson.fromJson(reader, GameState.class);
            reader.close();

            long dragonRespawnTime = state.getDragonRespawnTime();

            gameManager.setTeams(state.getTeams());
            gameManager.setDragonRespawnTime(dragonRespawnTime == 0 ? Long.MAX_VALUE : dragonRespawnTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class GameState {

        private final Set<DLDSTeam> teams;
        private final long dragonRespawnTime;
        private final String pluginVersion;

        public GameState(Set<DLDSTeam> teams, long dragonRespawnTime, String pluginVersion) {
            this.teams = teams;
            this.dragonRespawnTime = dragonRespawnTime;
            this.pluginVersion = pluginVersion;
        }

        public Set<DLDSTeam> getTeams() {
            return teams;
        }

        public long getDragonRespawnTime() {
            return dragonRespawnTime;
        }

        public String getPluginVersion() {
            return pluginVersion;
        }
    }




}
