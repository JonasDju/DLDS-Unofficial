package eu.bitflare.dlds;

import eu.bitflare.dlds.exceptions.SomePlayersAreOfflineException;
import eu.bitflare.dlds.exceptions.TeamCurrentlyPlayingException;
import eu.bitflare.dlds.exceptions.TeamNotPlayingException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class DLDSTeam {

    private final Set<PlayerData> players;
    private final String name;

    private boolean isPlaying;
    private boolean isCountdownRunning;

    public DLDSTeam(String name) {
        this.name = name;
        this.players = new HashSet<>();
        this.isPlaying = false;
        this.isCountdownRunning = false;
    }

    public boolean containsPlayer(UUID uuid) {
        return players.stream().anyMatch(playerData -> playerData.getUuid().equals(uuid));
    }

    public boolean containsPlayer(Player player) {
        return containsPlayer(player.getUniqueId());
    }

    public boolean addPlayer(Player player) {
        if(containsPlayer(player)) {
            return false;
        }

        players.add(new PlayerData(player));
        return true;
    }

    public boolean removePlayer(Player player) {
        if(!containsPlayer(player)) {
            return false;
        }

        players.removeIf(playerData -> playerData.getUuid().equals(player.getUniqueId()));
        return true;
    }

    public void startGame() {
        // Ignore call if the team is already playing
        if(isPlaying) {
            throw new TeamCurrentlyPlayingException(this);
        }
        isPlaying = true;

        // Check if all players of the team are online
        if(getOnlinePlayers().size() != players.size()) {
            // Get list of players that are part of the team but not contained in the result of getOnlinePlayers()
            List<String> offlinePlayerNames = players.stream().filter(pd ->
                    !getOnlinePlayers().stream().map(Entity::getUniqueId).toList().contains(pd.getUuid())
            ).map(PlayerData::getPlayerName).toList();
            isPlaying = false;
            throw new SomePlayersAreOfflineException(offlinePlayerNames, this);
        }

        GameManager gameManager = GameManager.getInstance();
        World overworld = Bukkit.getWorlds().getFirst();
        int worldborderSize = gameManager.getPlugin().getConfig().getInt("worldborder");

        // Get random spawn location and generate chunks
        Location location = gameManager.getRandomSpawnLocation(worldborderSize == 0 ? 10000 : worldborderSize/2.0);
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();
        for(int x = chunkX - 5; x < chunkX + 5; x++) {
            for(int z = chunkZ - 5; z < chunkZ + 5; z++) {
                overworld.getChunkAtAsync(x, z, true);
            }
        }

        // Give blindness, slowness and play sound
        for(Player player : getOnlinePlayers()) {
            player.setWalkSpeed(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*13, 1, false, false));
        }

        // Show loading message
        Component message = Component.text("The game will start soon!").color(DLDSColor.LIGHT_BLUE);
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1));
        broadcastTitle(message, Component.empty(), times);


        // Start countdown and teleport players
        isCountdownRunning = true;
        new BukkitRunnable() {

            private int countdown = 12;
            private final Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(250));

            @Override
            public void run() {

                switch (countdown) {
                    case 11 -> {
                        // Player sound shortly after countdown started
                        for(Player player : getOnlinePlayers()) {
                            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 1f, 1f);
                        }
                    }
                    case 1, 2, 3, 4, 5 -> {
                        // Teleport and reset players once countdown reaches 5
                        if(countdown == 5) {
                            gameManager.teleportPlayersDistributed(location, getOnlinePlayers());
                        }

                        // Only show countdown and play notes for the last five seconds

                        TextColor color = switch (countdown) {
                            case 3 -> DLDSColor.ORANGE;
                            case 2 -> DLDSColor.YELLOW;
                            case 1 -> DLDSColor.LIGHT_GREEN;
                            default -> DLDSColor.WHITE;
                        };
                        broadcastTitle(Component.text(countdown).color(color), Component.empty(), times);

                        // Play note
                        for (Player player : getOnlinePlayers()) {
                            player.playNote(location, Instrument.PIANO, Note.natural(0, Note.Tone.F));
                        }
                    }
                    case 0 -> {

                        // Send final title and sound
                        broadcastTitle(Component.text("0").color(DLDSColor.RED), Component.empty(), times);
                        for(Player player : getOnlinePlayers()) {
                            player.playNote(location, Instrument.PIANO, Note.natural(1, Note.Tone.F));
                            player.playSound(location, Sound.BLOCK_NOTE_BLOCK_IMITATE_ENDER_DRAGON, 1.0F, 1.0F);
                        }

                        // Create Scoreboards for all registered players, clear their inventory, set gamemode to survival, and fill hunger / HP, ...
                        for(PlayerData playerData : players) {
                            playerData.setRemainingTime(gameManager.getPlugin().getConfig().getLong("playtime"));

                            Player player = Bukkit.getPlayer(playerData.getUuid());

                            if(player != null && player.isOnline()) {
                                // Reset player
                                gameManager.resetPlayer(player);

                                // Create scoreboard
                                gameManager.getPlugin().getScoreboardManager().createBoardForPlayers(player);
                            }
                        }
                        isCountdownRunning = false;
                        cancel();
                    }
                }

                countdown--;
            }
        }.runTaskTimer(gameManager.getPlugin(), 0L, 20L);

    }

    public void stopGame() {
        if(!isPlaying) {
            throw new TeamNotPlayingException(this);
        }
        isPlaying = false;

        broadcastMessage(DLDSComponents.yourGameWasStopped());

        GameManager gameManager = GameManager.getInstance();
        for(Player player : getOnlinePlayers()) {
            gameManager.getPlugin().getScoreboardManager().deleteBoardForPlayers(player);
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1F, 1F);
        }
    }

    public void broadcastTitle(Component title, Component subtitle, Title.Times times) {
        for(Player player : getOnlinePlayers()) {
            player.showTitle(Title.title(title, subtitle, times));
        }
    }

    public void broadcastMessage(Component message) {
        for(Player player : getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void broadcastActionBar(Component message) {
        for(Player player : getOnlinePlayers()) {
            player.sendActionBar(message);
        }
    }

    public int getCurrentPoints() {
        GameManager gameManager = GameManager.getInstance();
        return players.stream().map(
                playerData -> playerData.getEarnedAdvancements().stream().map(
                        key -> gameManager.getAdvancementPoints(gameManager.getRewardsSection(key))
                ).reduce(0, Integer::sum)
        ).reduce(0, Integer::sum);
    }


    public int getAchievablePoints() {
        return GameManager.getInstance().getTotalAdvancementPoints() * players.size();
    }

    public int getCurrentAdvancementAmount() {
        return players.stream().map(playerData -> playerData.getEarnedAdvancements().size()).reduce(0, Integer::sum);
    }

    public int getAchievableAdvancementAmount() {
        return GameManager.getInstance().getTotalAdvancementCount() * players.size();
    }

    public int getSize() {
        return players.size();
    }

    public Set<Player> getOnlinePlayers() {
        Set<Player> onlinePlayers = new HashSet<>();
        for(PlayerData playerData : players) {
            Player player = Bukkit.getServer().getPlayer(playerData.getUuid());
            if(player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }

    public Optional<PlayerData> getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public Optional<PlayerData> getPlayerData(UUID uuid) {
        return players.stream().filter(playerData -> playerData.getUuid().equals(uuid)).findFirst();
    }

    public Set<NamespacedKey> getEarnedAdvancements() {
        Set<NamespacedKey> res = new HashSet<>();
        for(PlayerData playerData : players) {
            res.addAll(playerData.getEarnedAdvancements());
        }
        return res;
    }

    public boolean hasAdvancement(NamespacedKey key) {
        return getEarnedAdvancements().contains(key);
    }

    // Getters & Setters
    public Set<PlayerData> getPlayers() {
        return players;
    }

    public String getName() {
        return name;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isCountdownRunning() {
        return isCountdownRunning;
    }
}
