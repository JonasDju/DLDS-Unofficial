package eu.bitflare.dlds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static eu.bitflare.dlds.DLDSColor.*;

public class DLDSComponents {

    private static final int CHAT_WIDTH = 53;
    private static final int PLUGIN_NAME_WIDTH = 17;


    // Scoreboard
    public static final Component scoreboardHeader = text()
            .content("Unofficial DLDS").style(Style.style(DARK_GREEN, TextDecoration.BOLD)).build();

    public static Component longMessageHeader = empty()
                .append(text("=".repeat((CHAT_WIDTH - PLUGIN_NAME_WIDTH) / 2) + "[", DARK_GREY))
                .append(scoreboardHeader)
                .append(text("]" + "=".repeat((CHAT_WIDTH - PLUGIN_NAME_WIDTH) / 2), DARK_GREY));


    public static Component longMessageFooter = text("=".repeat(CHAT_WIDTH), DARK_GREY);

    public static Component formatTime(long remainingTime) {
        long hours = remainingTime / 3600;
        long minutes = (remainingTime % 3600) / 60;
        long seconds = remainingTime % 60;
        boolean isNegative = (hours < 0 || minutes < 0 || seconds < 0);

        return text()
                .color(isNegative ? RED : YELLOW)
                .content(isNegative ? "-" : "")
                .append(text(String.format("%02d:%02d:%02d", Math.abs(hours), Math.abs(minutes), Math.abs(seconds))))
                .build();
    }

    // Chat messages
    // Chat prefix ( [ ... ] )
    public static Component chatPrefix(Component prefix) {
        return text()
                .color(DARK_GREY)
                .content("[")
                .append(prefix)
                .append(text("] ", DARK_GREY))
                .build();
    }

    // New advancement
    public static Component newAdvancementMessage(Player player) {
        return chatPrefix(scoreboardHeader).append(text()
                .color(LIGHT_GREY)
                .content("New Advancement: Reward has been given to ")
                .append(text(player.getName(), LIGHT_GREEN))
                .append(text(".", LIGHT_GREY))
                .build()
        );
    }

    // No space in inventory for reward
    public static Component rewardInventoryFull() {
        return chatPrefix(scoreboardHeader)
                .append(text("Warning: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("Your ", LIGHT_GREY))
                .append(text("inventory is full", RED))
                .append(text("! Some rewards have been dropped at your location.", LIGHT_GREY));
    }

    // Leave message
    public static Component playerQuitMessage(Player player) {
        return chatPrefix(text("-", RED))
                .append(text(player.getName(), LIGHT_GREY));
    }

    // Join message
    public static Component playerJoinMessage(Player player) {
        return chatPrefix(text("+", LIGHT_GREEN))
                .append(text(player.getName(), LIGHT_GREY));
    }

    // Death message
    public static Component playerDeathMessage(Player player) {
        return chatPrefix(text("☠", RED))
                .append(text(player.getName(), LIGHT_GREY));
    }

    // Timeout quit message
    public static Component playerTimeoutQuitMessage(Player player) {
        return chatPrefix(text("⌛", RED))
                .append(text(player.getName(), LIGHT_GREY));
    }

    // Command messages
    // any -> must be player
    public static Component mustBePlayer() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("You ", LIGHT_GREY))
                .append(text("must be a player", RED))
                .append(text(" to use this command!", LIGHT_GREY));
    }

    // help message
    public static Component helpMessage() {
        return empty()
                .append(longMessageHeader)
                .appendNewline()
                .append(text("Commands:", LIGHT_GREY))
                .appendNewline()
                .append(renderCommandHelp("dlds enter", 1,
                        "Enter the event. Must be executed by",
                        "every player who wishes to participate."))
                .append(renderCommandHelp("dlds start", 1,
                        "Start the event once all players entered."))
                .append(renderCommandHelp("dlds stop", 2,
                        "Stop the event and reset the scoreboard."))
                .append(renderCommandHelp("dlds time", 3,
                        "Set the remaining time of a given player."))
                .append(longMessageFooter);
    }

    private static Component renderCommandHelp(String command, int initialOffset, String... descriptionLines) {
        Component res = text("  - ", LIGHT_GREY)
                .append(text("/"+command, ORANGE))
                .append(text(": ", LIGHT_GREY));

        for(int i = 0; i < descriptionLines.length; i++) {
            if(i > 0) {
                res = res.append(text(" ".repeat(22)));
            }
            res = res.append(text(" ".repeat(i == 0 ? initialOffset : 0) + descriptionLines[i], LIGHT_GREY)).appendNewline();
        }
        return res;
    }

    // enter -> DLDS already running
    public static Component registerGameAlradyRunning() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("You cannot enter because ", LIGHT_GREY))
                .append(text("DLDS is already running", RED))
                .append(text("!", LIGHT_GREY));
    }

    // enter -> already registered
    public static Component registerAlreadyRegistered() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("You have ", LIGHT_GREY))
                .append(text("already entered", RED))
                .append(text(" the event!", LIGHT_GREY));
    }

    // enter -> success
    public static Component registerSuccess() {
        return chatPrefix(scoreboardHeader).append(text()
                .color(LIGHT_GREY)
                .content("You have ")
                .append(text("entered the event", LIGHT_GREEN))
                .append(text("!", LIGHT_GREY))
                .build()
        );
    }

    // start -> no players
    public static Component startNoPlayers() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("No players", RED))
                .append(text(" have entered yet! Use ", LIGHT_GREY))
                .append(text("/dlds enter", ORANGE))
                .append(text(" to enter the game!", LIGHT_GREY));
    }

    // start -> already running
    public static Component startAlreadyRunning() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("DLDS is", RED))
                .append(text("already running", RED))
                .append(text("!", LIGHT_GREY));
    }

    // stop -> not started
    public static Component stopNotStarted() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("DLDS has ", LIGHT_GREY))
                .append(text("not started yet", RED))
                .append(text("!", LIGHT_GREY));
    }

    // stop -> success
    public static Component stopSuccess() {
        return chatPrefix(scoreboardHeader).append(text()
                .color(LIGHT_GREY)
                .content("DLDS ")
                .append(text("has been stopped", LIGHT_GREEN))
                .append(text("!", LIGHT_GREY))
                .build()
        );
    }

    // time -> player not found / registered
    public static Component timePlayerNotFound(Player player) {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("The player ", LIGHT_GREY))
                .append(text(player.getName(), LIGHT_GREEN))
                .append(text(" is ", LIGHT_GREY))
                .append(text("not registered", RED))
                .append(text("!", LIGHT_GREY));
    }

    // time -> success
    public static Component timeSuccess(Player player, int hours, int minutes, int seconds) {
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return chatPrefix(scoreboardHeader)
                .append(text("Time set for player ", LIGHT_GREY))
                .append(text(player.getName(), LIGHT_GREEN))
                .append(text(" to ", LIGHT_GREY))
                .append(text(time, YELLOW))
                .append(text("!", LIGHT_GREY));
    }

    // team -> create -> success
    public static Component teamCreateSuccess(String teamName) {
        return chatPrefix(scoreboardHeader)
                .append(text("Successfully created the team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text("!", LIGHT_GREY));
    }

    // team -> remove -> success
    public static Component teamRemoveSuccess(String teamName) {
        return chatPrefix(scoreboardHeader)
                .append(text("Successfully deleted the team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text("!", LIGHT_GREY));
    }

    // team -> list
    public static Component teamList(Set<DLDSTeam> teams) {
        if(teams.isEmpty()) {
            return chatPrefix(scoreboardHeader)
                    .append(text("There are currently ", LIGHT_GREY))
                    .append(text("no teams", RED))
                    .append(text("!", LIGHT_GREY));
        }

        Component result = empty()
                .append(longMessageHeader)
                .appendNewline()
                .append(text("Teams:", LIGHT_GREY))
                .appendNewline();

        for(DLDSTeam team : teams) {
            result = result.append(text("  - ", LIGHT_GREY))
                    .append(text(team.getName(), LIGHT_BLUE))
                    .append(text(": ", LIGHT_GREY));

            List<String> playerNames = team.getPlayers().stream().map(PlayerData::getPlayerName).toList();

            if(playerNames.isEmpty()) {
                result = result.append(text("No players!", LIGHT_GREY));
            } else if (playerNames.size() <= 2) {
                result = result.append(text(
                        String.join(", ", playerNames), LIGHT_GREY));
            } else {
                for(String playerName : playerNames) {
                    result = result.appendNewline()
                            .append(text("    - ", LIGHT_GREY))
                            .append(text(playerName, LIGHT_GREY));
                }
            }
            result = result.appendNewline();
        }
        return result.append(longMessageFooter);
    }

    // team -> addplayer -> success
    public static Component teamAddPlayerSuccess(Player player, String teamName) {
        return chatPrefix(scoreboardHeader)
                .append(text("Successfully added player ", LIGHT_GREY))
                .append(text(player.getName(), LIGHT_GREEN))
                .append(text(" to team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text("!", LIGHT_GREY));
    }

    // team -> addplayer -> you were added
    public static Component youWereAdded(String teamName) {
        return chatPrefix(scoreboardHeader)
                .append(text("You were added to the team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text("!", LIGHT_GREY));
    }

    // team -> removeplayer -> success
    public static Component teamRemovePlayerSuccess(Player player, String teamName) {
        return chatPrefix(scoreboardHeader)
                .append(text("Successfully removed player ", LIGHT_GREY))
                .append(text(player.getName(), LIGHT_GREEN))
                .append(text(" from team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text("!", LIGHT_GREY));
    }

    // team -> removeplayer -> you were removed
    public static Component youWereRemoved(String teamName) {
        return chatPrefix(scoreboardHeader)
                .append(text("You were removed from team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text("!", LIGHT_GREY));
    }


    // Kick messages
    private static Component kickMessagePointsAndAdvancements(int currentPoints, int maxPoints, int currentAdvancements, int maxAdvancements) {
        return text()
                .content("Points: ").color(LIGHT_GREY)
                .append(text(currentPoints, YELLOW))
                .append(text(" / ", DARK_GREY))
                .append(text(maxPoints, YELLOW))
                .appendNewline()
                .append(text("Advancements: ").color(LIGHT_GREY))
                .append(text(currentAdvancements, YELLOW))
                .append(text(" / ", DARK_GREY))
                .append(text(maxAdvancements, YELLOW))
                .build();
    }

    public static Component getPlayerTimeoutKickMessage(int currentPoints, int maxPoints, int currentAdvancements, int maxAdvancements) {
        return scoreboardHeader
                .appendNewline().appendNewline()
                .append(text("Your ", LIGHT_GREY))
                .append(text("time ran out", RED))
                .append(text("!", LIGHT_GREY))
                .appendNewline().appendNewline()
                .append(kickMessagePointsAndAdvancements(currentPoints, maxPoints, currentAdvancements, maxAdvancements));
    }

    public static Component getPlayerDeathKickMessage(int currentPoints, int maxPoints, int currentAdvancements, int maxAdvancements) {
        return scoreboardHeader
                .appendNewline().appendNewline()
                .append(text("You ", LIGHT_GREY))
                .append(text("died", RED))
                .append(text("!", LIGHT_GREY))
                .appendNewline().appendNewline()
                .append(kickMessagePointsAndAdvancements(currentPoints, maxPoints, currentAdvancements, maxAdvancements));
    }

}
