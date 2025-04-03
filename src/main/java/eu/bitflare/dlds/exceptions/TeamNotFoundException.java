package eu.bitflare.dlds.exceptions;

import eu.bitflare.dlds.DLDSComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import static eu.bitflare.dlds.DLDSColor.*;
import static eu.bitflare.dlds.DLDSComponents.*;
import static net.kyori.adventure.text.Component.text;

public class TeamNotFoundException extends DLDSException {

    private final String teamName;

    public TeamNotFoundException(String teamName) {
        this.teamName = teamName;
    }

    @Override
    public Component errorMessage() {
        return chatPrefix(scoreboardHeader)
                .append(text("Error: ").style(Style.style(LIGHT_GREY, TextDecoration.BOLD)))
                .append(text("The team ", LIGHT_GREY))
                .append(text(teamName, LIGHT_BLUE))
                .append(text(" does not exist", RED))
                .append(text("!", LIGHT_GREY));
    }

}
