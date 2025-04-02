package eu.bitflare.dlds;

import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager implements Listener {

    private final DLDSPlugin plugin;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    public ScoreboardManager(DLDSPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;

        // Start repeating task to update boards
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateBoards, 0, 20);


    }

    public void updateBoards(){
        for (FastBoard board : this.boards.values()) {
            updateBoard(board);
        }
    }

    private void updateBoard(FastBoard board) {
        GameManager gameManager = GameManager.getInstance();
        int currentPoints = gameManager.getCurrentPoints();
        int maxPoints = gameManager.getMaxPoints();

        int currentAdvancements = gameManager.getCurrentAdvancementAmount();
        int maxAdvancements = gameManager.getMaxAdvancementAmount();

        UUID playerId = board.getPlayer().getUniqueId();
        long remainingTime = gameManager.getPlayers().get(playerId).getRemainingTime();

        board.updateLines(
                Component.text("Remaining Time").color(DLDSColor.LIGHT_GREY),
                Component.text(" » ").color(DLDSColor.LIGHT_GREY).append(DLDSComponents.formatTime(remainingTime)),
                Component.text(""),
                Component.text("Points").color(DLDSColor.LIGHT_GREY),
                Component.text(" » ", DLDSColor.LIGHT_GREY).append(Component.text(currentPoints, DLDSColor.YELLOW))
                        .append(Component.text(" / ", DLDSColor.DARK_GREY))
                        .append(Component.text(maxPoints, DLDSColor.YELLOW)),
                Component.text(""),
                Component.text("Advancements", DLDSColor.LIGHT_GREY),
                Component.text().content(" » ").color(DLDSColor.LIGHT_GREY)
                        .append(Component.text(currentAdvancements, DLDSColor.YELLOW))
                        .append(Component.text(" / ", DLDSColor.DARK_GREY))
                        .append(Component.text(maxAdvancements, DLDSColor.YELLOW))
                        .build()
        );
    }

    public void createBoardForPlayers(Player... players){
        for(Player player : players){
            FastBoard board = new FastBoard(player);
            board.updateTitle(DLDSComponents.scoreboardHeader);
            updateBoard(board);
            boards.put(player.getUniqueId(), board);
        }
    }

    public void deleteBoardForPlayers(Player... players){
        for(Player player : players){
            FastBoard board = boards.get(player.getUniqueId());
            if(board != null){
                boards.remove(player.getUniqueId());
                board.delete();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        GameManager gameManager = GameManager.getInstance();
        Player player = event.getPlayer();

        if(gameManager.getRegisteredUUIDs().contains(player.getUniqueId()) && gameManager.isGameRunning()) {
            createBoardForPlayers(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        FastBoard board = this.boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }





}
