package eu.bitflare.dlds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
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
            state.upgrade(plugin);
            reader.close();

            gameManager.setTeams(state.getTeams());
            gameManager.setDragonRespawnTime(state.getDragonRespawnTime());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class GameState {

        // Backwards compatibility (v0.1 - v0.4)
        private Map<UUID, PlayerData> registeredPlayers;
        private Boolean isGameRunning;

        private Set<DLDSTeam> teams;
        private Long dragonRespawnTime;
        private final String pluginVersion;

        public GameState(Set<DLDSTeam> teams, long dragonRespawnTime, String pluginVersion) {
            this.teams = teams;
            this.dragonRespawnTime = dragonRespawnTime;
            this.pluginVersion = pluginVersion;
        }

        /**
         * Checks the current version of loaded data and upgrades it to the new format (if required)
         */
        public void upgrade(DLDSPlugin plugin) {
            // Take a look at the plugin version number inside the gamestate.json file and act accordingly
            switch (pluginVersion) {
                case null -> {
                    // The gamestate.json file was generated from plugin version v0.1 - v0.4. Therefore:
                    // It contains the "registeredPlayers" map which has to be converted to a set of teams
                    // It contains the "isGameRunning" boolean
                    // It may or may not contain the "dragonRespawnTime" long (only added in version v0.2)
                    plugin.getLogger().info("Found gamestate file from version v0.1 - v0.4. Upgrading...");

                    // Convert registeredPlayers -> teams
                    // Only try conversion if the game is actually running, otherwise just start clean
                    teams = new HashSet<>();
                    if(!registeredPlayers.isEmpty() && isGameRunning) {
                        // Create a new team with default name and correct player data
                        DLDSTeam team = new DLDSTeam("Team");
                        team.getPlayers().addAll(registeredPlayers.values());
                        team.setPlaying(true);

                        teams.add(team);
                    }

                    // Load dragonRespawnTime
                    // If it does not exist (v0.1), then set it to the max long value
                    if(dragonRespawnTime == null) {
                        dragonRespawnTime = Long.MAX_VALUE;
                    }
                }
                default -> {}
            }
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
