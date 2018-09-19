package no.nav.melosys.service.oppgave.dto;


import java.time.LocalDate;

import no.nav.melosys.domain.oppgave.PrioritetType;

public class OppgaveDto {
    private LocalDate aktivTil;
    private String ansvarligID;
    private String oppgaveID;
    private String oppgavetypeKode;
    private PrioritetType prioritet;
    private int versjon;

    //Getter brukes av Jackson for å serialisere oppgave objekter til frontend i OppgaveTjeneste
    public LocalDate getAktivTil() {
        return aktivTil;
    }

    public void setAktivTil(LocalDate aktivTil) {
        this.aktivTil = aktivTil;
    }

    public String getAnsvarligID() {
        return ansvarligID;
    }

    public void setAnsvarligID(String ansvarligID) {
        this.ansvarligID = ansvarligID;
    }

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public String getOppgavetypeKode() {
        return oppgavetypeKode;
    }

    public void setOppgavetypeKode(String oppgavetypeKode) {
        this.oppgavetypeKode = oppgavetypeKode;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(PrioritetType prioritet) {
        this.prioritet = prioritet;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }
}
