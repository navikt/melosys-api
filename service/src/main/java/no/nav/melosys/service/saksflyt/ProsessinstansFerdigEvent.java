package no.nav.melosys.service.saksflyt;

import java.util.UUID;

import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.context.ApplicationEvent;

public class ProsessinstansFerdigEvent extends ApplicationEvent {

    private final UUID uuid;
    private final String låsReferanse;

    public ProsessinstansFerdigEvent(Prosessinstans prosessinstans) {
        super(prosessinstans.getId());
        this.uuid = prosessinstans.getId();
        this.låsReferanse = prosessinstans.getLåsReferanse();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLåsReferanse() {
        return låsReferanse;
    }
}
