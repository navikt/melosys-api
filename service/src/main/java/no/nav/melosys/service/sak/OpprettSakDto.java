package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class OpprettSakDto {
    private String brukerID;
    private String virksomhetOrgnr;
    private Sakstyper sakstype;
    private Behandlingstema behandlingstema;
    private Behandlingstyper behandlingstype;
    private String oppgaveID;
    private SøknadDto soknadDto;
    private boolean skalTilordnes;
    private Sakstemaer sakstema;
    private Aktoersroller hovedpart;

    public String getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    public void setVirksomhetOrgnr(String virksomhetOrgnr) {
        this.virksomhetOrgnr = virksomhetOrgnr;
    }

    public Aktoersroller getHovedpart() {
        return hovedpart;
    }

    public void setHovedpart(Aktoersroller hovedpart) {
        this.hovedpart = hovedpart;
    }

    public Sakstemaer getSakstema() {
        return sakstema;
    }

    public void setSakstema(Sakstemaer sakstema) {
        this.sakstema = sakstema;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public void setBrukerID(String brukerID) {
        this.brukerID = brukerID;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public void setSakstype(Sakstyper sakstype) {
        this.sakstype = sakstype;
    }

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

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public SøknadDto getSoknadDto() {
        return soknadDto;
    }

    public void setSoknadDto(SøknadDto soknadDto) {
        this.soknadDto = soknadDto;
    }

    public boolean isSkalTilordnes() {
        return skalTilordnes;
    }

    public void setSkalTilordnes(boolean skalTilordnes) {
        this.skalTilordnes = skalTilordnes;
    }
}
