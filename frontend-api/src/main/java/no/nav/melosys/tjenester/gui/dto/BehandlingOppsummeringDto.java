package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingOppsummeringDto {

    private Behandlingsstatus behandlingsstatus;
    private Behandlingstyper behandlingstype;
    private Instant registrertDato;
    private Instant endretDato;
    private Instant sisteOpplysningerHentetDato;

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(Behandlingsstatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    public Instant getSisteOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }
}
