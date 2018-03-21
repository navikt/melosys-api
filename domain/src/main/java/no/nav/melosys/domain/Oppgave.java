package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.gsak.*;

/**
 * Den klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public class Oppgave {

    private String oppgaveId;
    //private Bruker gjelder;
    //private Status status;
    private LocalDate aktivFra;
    private LocalDate aktivTil;
    //private String ansvarligId;
    private Fagomrade fagomrade;
    private Underkategori underkategori;
    private Oppgavetype oppgavetype;
    private String beskrivelse;
    //private String oppfolging;
    private PrioritetType prioritet;
    //private Integer versjon;
    private String saksnummer;
    //private String kravId;
    private String dokumentId;
    private String soknadsId;

    public Oppgave() {
    }

    public boolean harHøyPrioritet() {
        return ((PrioritetType.HOY_MED.equals(prioritet)) || PrioritetType.HOY_UFM.equals(prioritet));
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }

    public LocalDate getAktivFra() {
        return aktivFra;
    }

    public void setAktivFra(LocalDate aktivFra) {
        this.aktivFra = aktivFra;
    }

    public LocalDate getAktivTil() {
        return aktivTil;
    }

    public void setAktivTil(LocalDate aktivTil) {
        this.aktivTil = aktivTil;
    }

    public Fagomrade getFagomrade() {
        return fagomrade;
    }

    public void setFagomrade(Fagomrade fagomrade) {
        this.fagomrade = fagomrade;
    }

    public Underkategori getUnderkategori() {
        return underkategori;
    }

    public void setUnderkategori(Underkategori underkategori) {
        this.underkategori = underkategori;
    }

    public Oppgavetype getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(Oppgavetype oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(PrioritetType prioritet) {
        this.prioritet = prioritet;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }
}
