package eu.bitflare.dlds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static eu.bitflare.dlds.DLDSColor.*;

public class DLDSComponents {

    // Scoreboard
    public static final Component scoreboardHeader = text()
            .content("Unofficial DLDS").style(Style.style(DARK_GREEN, TextDecoration.BOLD)).build();

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
        final int chatWidth = 53;
        final int pluginNameWidth = 17;

        Component header = empty()
                .append(text("=".repeat((chatWidth - pluginNameWidth)/2) + "[", DARK_GREY))
                .append(scoreboardHeader)
                .append(text("]" + "=".repeat((chatWidth - pluginNameWidth)/2), DARK_GREY));
        Component footer = text("=".repeat(chatWidth), DARK_GREY);

        return empty()
                .append(header)
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
                .append(footer);
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
        return chatPrefix(scoreboardHeader).append(text()
                .color(LIGHT_GREY)
                .content("Error: DLDS is ")
                .append(text("already running", RED))
                .append(text("!", LIGHT_GREY))
                .build()
        );
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
    public static Component timePlayerNotFound(String playerName) {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("The player ", LIGHT_GREY))
                .append(text(playerName, LIGHT_GREEN))
                .append(text(" is ", LIGHT_GREY))
                .append(text("not registered", RED))
                .append(text("!", LIGHT_GREY));
    }

    // time -> success
    public static Component timeSuccess(String playerName, int hours, int minutes, int seconds) {
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return chatPrefix(scoreboardHeader)
                .append(text("Time set for player ", LIGHT_GREY))
                .append(text(playerName, LIGHT_GREEN))
                .append(text(" to ", LIGHT_GREY))
                .append(text(time, YELLOW))
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
