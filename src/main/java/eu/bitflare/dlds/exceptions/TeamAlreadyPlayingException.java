package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSComponents.chatPrefix;
import static eu.bitflare.dlds.DLDSComponents.scoreboardHeader;
import static net.kyori.adventure.text.Component.text;

public class TeamAlreadyPlayingException extends DLDSException{

    private DLDSTeam team;

    public TeamAlreadyPlayingException(DLDSTeam team) {
        this.team = team;
    }

    @Override
    public Component errorMessage() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("The team ", LIGHT_GREY))
                .append(text(team.getName(), LIGHT_BLUE))
                .append(text(" is ", LIGHT_GREY))
                .append(text("already playing", RED))
                .append(text("!", LIGHT_GREY));
    }
}
