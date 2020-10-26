package no.nav.melosys.saksflyt.kontroll.dto;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;

public class HentProsessinstansDto {
    private final UUID id;
    private final String prosessType;
    private final LocalDateTime endretDato;
    private final String sistFullførtSteg;

    public HentProsessinstansDto(Prosessinstans prosessinstans) {
        this.id = prosessinstans.getId();
        this.prosessType = prosessinstans.getType().getKode();
        this.endretDato = prosessinstans.getEndretDato();
        this.sistFullførtSteg = Optional.ofNullable(prosessinstans.getSistFullførtSteg())
            .map(ProsessSteg::getKode)
            .orElse(null);
    }

    public UUID getId() {
        return id;
    }

    public String getProsessType() {
        return prosessType;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public String getSistFullførtSteg() {
        return sistFullførtSteg;
    }
}
