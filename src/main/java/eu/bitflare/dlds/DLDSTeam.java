package eu.bitflare.dlds;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DLDSTeam {

    private final Set<PlayerData> players;
    private final String name;
    private final GameManager gameManager;

    public DLDSTeam(GameManager gameManager, String name) {
        this.gameManager = gameManager;
        this.name = name;
        this.players = new HashSet<>();
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

    public int getCurrentPoints() {
        return players.stream().map(
                playerData -> playerData.getEarnedAdvancements().stream().map(
                        key -> gameManager.getAdvancementPoints(gameManager.getRewardsSection(key))
                ).reduce(0, Integer::sum)
        ).reduce(0, Integer::sum);
    }

    public int getAchievablePoints() {
        return gameManager.getTotalAdvancementPoints() * players.size();
    }

    public int getCurrentAdvancementAmount() {
        return players.stream().map(playerData -> playerData.getEarnedAdvancements().size()).reduce(0, Integer::sum);
    }

    public int getAchievableAdvancementAmount() {
        return gameManager.getTotalAdvancementCount() * players.size();
    }

    public int getSize() {
        return players.size();
    }


    // Getters & Setters
    public Set<PlayerData> getPlayers() {
        return players;
    }

    public String getName() {
        return name;
    }

}
