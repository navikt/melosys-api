package no.nav.melosys.saksflyt.kontroll.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansHendelse;

public class HentProsessinstansDto {
    private final UUID id;
    private final Long behandlingId;
    private final String prosessType;
    private final LocalDateTime endretDato;
    private final String sistFullførtSteg;
    private final String sisteFeilmelding;

    public HentProsessinstansDto(Prosessinstans prosessinstans) {
        this.id = prosessinstans.getId();
        this.behandlingId = prosessinstans.getBehandling() == null ? null : prosessinstans.getBehandling().getId();
        this.prosessType = prosessinstans.getType().getKode();
        this.endretDato = prosessinstans.getEndretDato();
        this.sistFullførtSteg = Optional.ofNullable(prosessinstans.getSistFullførtSteg())
            .map(ProsessSteg::getKode)
            .orElse(null);
        this.sisteFeilmelding = prosessinstans.getHendelser()
            .stream()
            .max(Comparator.comparing(ProsessinstansHendelse::getDato))
            .map(ProsessinstansHendelse::getMelding)
            .orElse(null);
    }

    public UUID getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
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

    public String getSisteFeilmelding() {
        return sisteFeilmelding;
    }
}
