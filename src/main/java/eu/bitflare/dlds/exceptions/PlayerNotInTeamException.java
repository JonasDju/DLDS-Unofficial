package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSComponents.*;
import static net.kyori.adventure.text.Component.text;

public class PlayerNotInTeamException extends DLDSException {

    private final Player player;
    private final DLDSTeam team;

    public PlayerNotInTeamException(Player player, DLDSTeam team) {
        this.player = player;
        this.team = team;
    }

    @Override
    public Component errorMessage() {
        Component res =  chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("Player ", LIGHT_GREY))
                .append(text(player.getName(), LIGHT_GREEN));
        if(team != null) {
            res = res.append(text(" is not part of the team ", LIGHT_GREY))
                    .append(text(team.getName(), LIGHT_BLUE))
                    .append(text("!", LIGHT_GREY));
        } else {
            res = res.append(text(" is ", LIGHT_GREY))
                    .append(text("not part of a team", RED))
                    .append(text("!", LIGHT_GREY));
        }
        return res;
    }
}
