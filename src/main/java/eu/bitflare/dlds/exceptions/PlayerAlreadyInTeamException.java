package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSComponents;
import eu.bitflare.dlds.DLDSTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSComponents.*;
import static net.kyori.adventure.text.Component.text;

public class PlayerAlreadyInTeamException extends DLDSException {

    private final Player player;
    private final DLDSTeam team;

    public PlayerAlreadyInTeamException(Player player, DLDSTeam team) {
        this.player = player;
        this.team = team;
    }

    @Override
    public Component errorMessage() {
        String command = "/dlds team removeplayer " + player.getName();
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("Player ", LIGHT_GREY))
                .append(text(player.getName(), LIGHT_GREEN))
                .append(text(" is ", LIGHT_GREY))
                .append(text("already in team ", RED))
                .append(text(team.getName(), LIGHT_BLUE))
                .append(text(". Use ", LIGHT_GREY))
                .append(text(command, ORANGE)
                        .clickEvent(ClickEvent.suggestCommand(command))
                        .hoverEvent(HoverEvent.showText(text("Click to copy", WHITE))))
                .append(text(" to remove them from their team.", LIGHT_GREY));
    }
}
