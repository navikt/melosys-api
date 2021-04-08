package no.nav.melosys.service.saksflyt;

import java.util.UUID;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansLåsType;
import org.springframework.context.ApplicationEvent;

public class ProsessinstansFerdigEvent extends ApplicationEvent {

    private final UUID uuid;
    private final ProsessinstansLåsType prosessinstansLåsType;
    private final String låsReferanse;

    public ProsessinstansFerdigEvent(Prosessinstans prosessinstans) {
        super(prosessinstans.getId());
        this.uuid = prosessinstans.getId();
        this.prosessinstansLåsType = prosessinstans.getLåsType();
        this.låsReferanse = prosessinstans.getLåsReferanse();
    }

    public UUID getUuid() {
        return uuid;
    }

    public ProsessinstansLåsType getProsessinstansLåsType() {
        return prosessinstansLåsType;
    }

    public String getLåsReferanse() {
        return låsReferanse;
    }
}
