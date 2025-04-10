package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSComponents.chatPrefix;
import static eu.bitflare.dlds.DLDSComponents.scoreboardHeader;
import static net.kyori.adventure.text.Component.text;

public class TeamCurrentlyPlayingException extends DLDSException{

    private DLDSTeam team;

    public TeamCurrentlyPlayingException(DLDSTeam team) {
        this.team = team;
    }

    @Override
    public Component errorMessage() {
        String command = "/dlds stop " + team.getName();
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("The team ", LIGHT_GREY))
                .append(text(team.getName(), LIGHT_BLUE))
                .append(text(" is ", LIGHT_GREY))
                .append(text("currently playing", RED))
                .append(text("! Use ", LIGHT_GREY))
                .append(text(command, ORANGE)
                        .clickEvent(ClickEvent.suggestCommand(command))
                        .hoverEvent(HoverEvent.showText(text("Click to copy", WHITE))))
                .append(text(" to stop their game.", LIGHT_GREY));
    }
}
