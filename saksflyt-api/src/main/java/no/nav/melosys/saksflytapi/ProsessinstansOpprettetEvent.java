package no.nav.melosys.saksflytapi;

import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.context.ApplicationEvent;

public class ProsessinstansOpprettetEvent extends ApplicationEvent {
    public ProsessinstansOpprettetEvent(Prosessinstans prosessinstans) {
        super(prosessinstans);
    }

    public Prosessinstans hentProsessinstans() {
        return (Prosessinstans) getSource();
    }
}
