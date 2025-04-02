package eu.bitflare.dlds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
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
                    gameManager.getPlayers(),
                    gameManager.isGameRunning(),
                    gameManager.getDragonRespawnTime()
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

            gameManager.setGameRunning(state.isGameRunning());
            gameManager.setPlayers(state.getRegisteredPlayers());
            gameManager.setDragonRespawnTime(dragonRespawnTime == 0 ? Long.MAX_VALUE : dragonRespawnTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class GameState {

        private final Map<UUID, PlayerData> registeredPlayers;
        private final boolean isGameRunning;
        private final long dragonRespawnTime;

        public GameState(Map<UUID, PlayerData> registeredPlayers, boolean isGameRunning, long dragonRespawnTime) {
            this.registeredPlayers = registeredPlayers;
            this.isGameRunning = isGameRunning;
            this.dragonRespawnTime = dragonRespawnTime;
        }

        public Map<UUID, PlayerData> getRegisteredPlayers() {
            return registeredPlayers;
        }

        public boolean isGameRunning() {
            return isGameRunning;
        }

        public long getDragonRespawnTime() {
            return dragonRespawnTime;
        }

    }




}
