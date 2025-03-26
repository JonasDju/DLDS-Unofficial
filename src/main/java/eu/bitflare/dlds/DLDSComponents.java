package eu.bitflare.dlds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

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
