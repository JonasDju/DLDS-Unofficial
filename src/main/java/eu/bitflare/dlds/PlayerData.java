package eu.bitflare.dlds;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String playerName;
    private long remainingTime;
    private boolean isDead;
    private Set<NamespacedKey> earnedAdvancements;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.remainingTime = 12 * 60 * 60;
        this.isDead = false;
        this.earnedAdvancements = new HashSet<>();
    }


    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(long remainingTime) {
        this.remainingTime = remainingTime;
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public Set<NamespacedKey> getEarnedAdvancements() {
        return earnedAdvancements;
    }

}
