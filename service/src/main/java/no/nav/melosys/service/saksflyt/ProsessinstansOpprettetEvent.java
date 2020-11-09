package no.nav.melosys.service.saksflyt;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.springframework.context.ApplicationEvent;

public class ProsessinstansOpprettetEvent extends ApplicationEvent {
    public ProsessinstansOpprettetEvent(Prosessinstans prosessinstans) {
        super(prosessinstans);
    }

    public Prosessinstans hentProsessinstans() {
        return (Prosessinstans) getSource();
    }
}
