package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingOppsummeringDto {

    private Behandlingsstatus behandlingsstatus;
    private Behandlingstyper behandlingstype;
    private Behandlingstema behandlingstema;
    private String fakturaserieId;
    private Instant registrertDato;
    private Instant endretDato;
    private String endretAvNavn;
    private Instant sisteOpplysningerHentetDato;
    private Instant svarFrist;
    private LocalDate behandlingsfrist;
    private Behandlingsresultattyper behandlingsresultattype;

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

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(Behandlingstema behandlingstema) {
        this.behandlingstema = behandlingstema;
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

    public String getEndretAvNavn() {
        return endretAvNavn;
    }

    public void setEndretAvNavn(String endretAvNavn) {
        this.endretAvNavn = endretAvNavn;
    }

    public Instant getSisteOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }

    public Instant getSvarFrist() {
        return svarFrist;
    }

    public void setSvarFrist(Instant svarFrist) {
        this.svarFrist = svarFrist;
    }

    public LocalDate getBehandlingsfrist() {
        return behandlingsfrist;
    }

    public void setBehandlingsfrist(LocalDate behandlingsfrist) {
        this.behandlingsfrist = behandlingsfrist;
    }

    public Behandlingsresultattyper getBehandlingsresultattype() {
        return behandlingsresultattype;
    }

    public void setBehandlingsresultattype(Behandlingsresultattyper behandlingsresultattype) {
        this.behandlingsresultattype = behandlingsresultattype;
    }

    public String getFakturaserieId() {
        return fakturaserieId;
    }

    public void setFakturaserieId(String fakturaserieId) {
        this.fakturaserieId = fakturaserieId;
    }
}
