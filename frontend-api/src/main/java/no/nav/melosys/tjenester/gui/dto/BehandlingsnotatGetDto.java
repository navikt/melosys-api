package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;

import no.nav.melosys.domain.Behandlingsnotat;

public class BehandlingsnotatGetDto {

    private final long notatId; //
    private final String tekst; //
    private final Instant endretDato;
    private final Instant registrertDato;
    private final String behandlingstypeKode;
    private final boolean redigerbar; //
    private final String registrertAvNavn;
    private final String sistEndretAvNavn;

    public BehandlingsnotatGetDto(Behandlingsnotat behandlingsnotat, boolean redigerbar, String registrertAvNavn, String sistEndretAvNavn) {
        this.notatId = behandlingsnotat.getId();
        this.tekst = behandlingsnotat.getTekst();
        this.endretDato = behandlingsnotat.getEndretDato();
        this.registrertDato = behandlingsnotat.getRegistrertDato();
        this.behandlingstypeKode = behandlingsnotat.getBehandling().getType().getKode();

        this.redigerbar = redigerbar;
        this.registrertAvNavn = registrertAvNavn;
        this.sistEndretAvNavn = sistEndretAvNavn;
    }

    public long getNotatId() {
        return notatId;
    }

    public String getTekst() {
        return tekst;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public String getBehandlingstypeKode() {
        return behandlingstypeKode;
    }

    public boolean isRedigerbar() {
        return redigerbar;
    }

    public String getRegistrertAvNavn() {
        return registrertAvNavn;
    }

    public String getSistEndretAvNavn() {
        return sistEndretAvNavn;
    }
}
