package no.nav.melosys.saksflytapi.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ProsessinstansInfo {
    private final UUID uuid;
    private final ProsessStatus prosessStatus;
    private final LocalDateTime registrertDato;
    private final String låsReferanse;
    private final SedLåsReferanse sedLåsReferanse;

    public ProsessinstansInfo(Prosessinstans prosessinstans) {
        this(prosessinstans.getId(), prosessinstans.getStatus(),
            prosessinstans.getRegistrertDato(), prosessinstans.getLåsReferanse());
    }

    public ProsessinstansInfo(UUID uuid, ProsessStatus prosessStatus, LocalDateTime registrertDato, String låsReferanse) {
        this.uuid = uuid;
        this.prosessStatus = prosessStatus;
        this.registrertDato = registrertDato;
        this.låsReferanse = låsReferanse;
        this.sedLåsReferanse = SedLåsReferanse.erGyldigReferanse(låsReferanse) ? new SedLåsReferanse(låsReferanse) : null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ProsessStatus getProsessStatus() {
        return prosessStatus;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public String getLåsReferanse() {
        return låsReferanse;
    }

    public SedLåsReferanse getSedLåsReferanse() {
        return sedLåsReferanse;
    }
}
