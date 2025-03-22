package eu.bitflare.dlds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import static net.kyori.adventure.text.Component.*;

public class DLDSComponents {

    // Scoreboard
    public static final Component scoreboardHeader = text()
            .content("Unofficial DLDS").style(Style.style(DLDSColor.DARK_GREEN, TextDecoration.BOLD)).build();

    public static Component formatTime(long remainingTime) {
        long hours = remainingTime / 3600;
        long minutes = (remainingTime % 3600) / 60;
        long seconds = remainingTime % 60;
        boolean isNegative = (hours < 0 || minutes < 0 || seconds < 0);

        return text()
                .color(isNegative ? DLDSColor.RED : DLDSColor.YELLOW)
                .content(isNegative ? "-" : "")
                .append(text(String.format("%02d:%02d:%02d", Math.abs(hours), Math.abs(minutes), Math.abs(seconds))))
                .build();
    }


    // Kick messages
    private static Component kickMessagePointsAndAdvancements(int currentPoints, int maxPoints, int currentAdvancements, int maxAdvancements) {
        return text()
                .content("Points: ").color(DLDSColor.LIGHT_GREY)
                .append(text(currentPoints, DLDSColor.YELLOW))
                .append(text(" / ", DLDSColor.DARK_GREY))
                .append(text(maxPoints, DLDSColor.YELLOW))
                .appendNewline()
                .append(text("Advancements: ").color(DLDSColor.LIGHT_GREY))
                .append(text(currentAdvancements, DLDSColor.YELLOW))
                .append(text(" / ", DLDSColor.DARK_GREY))
                .append(text(maxAdvancements, DLDSColor.YELLOW))
                .build();
    }

    public static Component getPlayerTimeoutKickMessage(int currentPoints, int maxPoints, int currentAdvancements, int maxAdvancements) {
        return scoreboardHeader
                .appendNewline().appendNewline()
                .append(text("Your ", DLDSColor.LIGHT_GREY))
                .append(text("time ran out", DLDSColor.RED))
                .append(text("!", DLDSColor.LIGHT_GREY))
                .appendNewline().appendNewline()
                .append(kickMessagePointsAndAdvancements(currentPoints, maxPoints, currentAdvancements, maxAdvancements));
    }

    public static Component getPlayerDeathKickMessage(int currentPoints, int maxPoints, int currentAdvancements, int maxAdvancements) {
        return scoreboardHeader
                .appendNewline().appendNewline()
                .append(text("You ", DLDSColor.LIGHT_GREY))
                .append(text("died", DLDSColor.RED))
                .append(text("!", DLDSColor.LIGHT_GREY))
                .appendNewline().appendNewline()
                .append(kickMessagePointsAndAdvancements(currentPoints, maxPoints, currentAdvancements, maxAdvancements));
    }

}
