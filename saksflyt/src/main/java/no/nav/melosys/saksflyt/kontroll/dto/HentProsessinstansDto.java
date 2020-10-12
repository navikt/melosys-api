package no.nav.melosys.saksflyt.kontroll.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.Prosessinstans;

public class HentProsessinstansDto {
    private final UUID id;
    private final String prosessType;
    private final String sistFullførtSteg;
    private final LocalDateTime endretDato;

    public HentProsessinstansDto(Prosessinstans prosessinstans) {
        this.id = prosessinstans.getId();
        this.prosessType = prosessinstans.getType().getKode();
        this.sistFullførtSteg = prosessinstans.getSistFullførtSteg().getKode();
        this.endretDato = prosessinstans.getEndretDato();
    }

    public UUID getId() {
        return id;
    }

    public String getProsessType() {
        return prosessType;
    }

    public String getSistFullførtSteg() {
        return sistFullførtSteg;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }
}
