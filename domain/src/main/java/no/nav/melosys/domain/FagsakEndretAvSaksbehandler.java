package no.nav.melosys.domain;

import org.springframework.context.ApplicationEvent;

public class FagsakEndretAvSaksbehandler extends ApplicationEvent {
    public FagsakEndretAvSaksbehandler(Fagsak fagsak) {
        super(fagsak);
    }

    public Fagsak getFagsak() {
        return (Fagsak) super.getSource();
    }
}
