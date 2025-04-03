package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSColor.LIGHT_GREY;
import static eu.bitflare.dlds.DLDSComponents.chatPrefix;
import static eu.bitflare.dlds.DLDSComponents.scoreboardHeader;
import static net.kyori.adventure.text.Component.text;

public class EmptyTeamException extends DLDSException {

    private final DLDSTeam team;

    public EmptyTeamException(DLDSTeam team) {
        this.team = team;
    }


    @Override
    public Component errorMessage() {
        String command = "/dlds team addplayer ";
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("The team ", LIGHT_GREY))
                .append(text(team.getName(), LIGHT_BLUE))
                .append(text(" is empty", RED))
                .append(text("! Use ", LIGHT_GREY))
                .append(text(command + "<playername> " + team.getName(), ORANGE)
                        .clickEvent(ClickEvent.suggestCommand(command))
                        .hoverEvent(HoverEvent.showText(text("Click to copy", WHITE))))
                .append(text(" to add someone to the team!", LIGHT_GREY));
    }
}
