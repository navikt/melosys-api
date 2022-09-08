package no.nav.melosys.service.oppgave.dto;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class PlukkOppgaveInnDto {

    private Behandlingstema behandlingstema;
    private Behandlingstyper behandlingstype;
    private Sakstemaer sakstema;
    private Sakstyper sakstype;

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(Behandlingstema behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Sakstemaer getSakstema() {
        return sakstema;
    }

    public void setSakstema(Sakstemaer sakstema) {
        this.sakstema = sakstema;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public void setSakstype(Sakstyper sakstype) {
        this.sakstype = sakstype;
    }
}
