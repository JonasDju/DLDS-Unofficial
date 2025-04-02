package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSColor.LIGHT_GREY;
import static eu.bitflare.dlds.DLDSComponents.chatPrefix;
import static eu.bitflare.dlds.DLDSComponents.scoreboardHeader;
import static net.kyori.adventure.text.Component.text;

public class SomePlayersAreOfflineException extends DLDSException {

    private List<Player> offlinePlayers;
    private DLDSTeam team;

    public SomePlayersAreOfflineException(List<Player> offlinePlayers, DLDSTeam team) {
        this.offlinePlayers = offlinePlayers;
        this.team = team;
    }

    @Override
    public Component errorMessage() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("Cannot start game for team ", LIGHT_GREY))
                .append(text(team.getName(), LIGHT_BLUE))
                .append(text(". The following players are not online: ", LIGHT_GREY))
                .append(text(String.join(", ", offlinePlayers.stream().map(Player::getName).toList()), LIGHT_GREY))
                .append(text("!", LIGHT_GREY));
    }
}
