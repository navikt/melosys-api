package no.nav.melosys.domain;

import org.springframework.context.ApplicationEvent;

public class FagsakEndretAvSaksbehandler extends ApplicationEvent {
    public FagsakEndretAvSaksbehandler(String saksnummer) {
        super(saksnummer);
    }

    public String getSaksnummer() {
        return (String) super.getSource();
    }
}
