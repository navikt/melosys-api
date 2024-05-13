package no.nav.melosys.saksflyt;

import java.util.UUID;

import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.context.ApplicationEvent;

public class ProsessinstansFerdigEvent extends ApplicationEvent {

    public Prosessinstans hentProsessinstans() {
        return (Prosessinstans) getSource();
    }

    public ProsessinstansFerdigEvent(Prosessinstans prosessinstans) {
        super(prosessinstans);
    }

    public UUID getUuid() {
        return hentProsessinstans().getId();
    }

    public UUID getParentId() {
        return hentProsessinstans().getData(ProsessDataKey.PROCESS_PARENT_ID, UUID.class);
    }

    public String getLåsReferanse() {
        return hentProsessinstans().getLåsReferanse();
    }
}
