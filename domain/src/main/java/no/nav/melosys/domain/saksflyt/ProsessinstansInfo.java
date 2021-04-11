package no.nav.melosys.domain.saksflyt;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ProsessinstansInfo {
    private final UUID uuid;
    private final ProsessStatus prosessStatus;
    private final LocalDateTime registrertDato;
    private final ProsessinstansLåsType låsType;
    private final String låsReferanse;
    private final SedLåsReferanse sedLåsReferanse;

    public ProsessinstansInfo(Prosessinstans prosessinstans) {
        this(prosessinstans.getId(), prosessinstans.getStatus(), prosessinstans.getRegistrertDato(),
            prosessinstans.getLåsType(), prosessinstans.getLåsReferanse());
    }

    public ProsessinstansInfo(UUID uuid, ProsessStatus prosessStatus, LocalDateTime registrertDato, ProsessinstansLåsType låsType, String låsReferanse) {
        this.uuid = uuid;
        this.prosessStatus = prosessStatus;
        this.registrertDato = registrertDato;
        this.låsType = låsType;
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

    public ProsessinstansLåsType getLåsType() {
        return låsType;
    }

    public String getLåsReferanse() {
        return låsReferanse;
    }

    public SedLåsReferanse getSedLåsReferanse() {
        return sedLåsReferanse;
    }
}
