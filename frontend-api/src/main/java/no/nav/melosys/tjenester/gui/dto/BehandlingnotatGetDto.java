package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;

import no.nav.melosys.domain.Behandlingsnotat;

public class BehandlingnotatGetDto {

    private final long notatID;
    private final String tekst;
    private final boolean redigerbar;
    private final Instant endretDato;
    private final Instant registrertDato;
    private final String saksbehandlerNavn;

    public BehandlingnotatGetDto(Behandlingsnotat behandlingsnotat) {
        this.notatID = behandlingsnotat.getId();
        this.tekst = behandlingsnotat.getTekst();
        this.redigerbar = behandlingsnotat.erRedigerbar();
        this.endretDato = behandlingsnotat.getEndretDato();
        this.registrertDato = behandlingsnotat.getRegistrertDato();
        this.saksbehandlerNavn = behandlingsnotat.getRegistrertAv();
    }

    public long getNotatID() {
        return notatID;
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

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }

    public boolean isRedigerbar() {
        return redigerbar;
    }
}
