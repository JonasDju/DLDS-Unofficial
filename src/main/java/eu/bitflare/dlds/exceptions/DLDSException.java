package eu.bitflare.dlds.exceptions;

import net.kyori.adventure.text.Component;


public abstract class DLDSException extends RuntimeException {

    public DLDSException() {
        super();
    }

    public abstract Component errorMessage();

}
