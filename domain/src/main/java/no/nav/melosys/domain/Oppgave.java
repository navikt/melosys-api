package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.domain.gsak.Underkategori;

/**
 * Den klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public class Oppgave {
    private String oppgaveId;
    private String saksnummer;
    private LocalDate aktivFra;
    private LocalDate aktivTil;
    private Fagomrade fagomrade;
    private Underkategori underkategori;
    private String ansvarligId;
    private Oppgavetype oppgavetype;
    private PrioritetType prioritet;
    private String gsakSaksnummer;
    private String dokumentId;

    public Oppgave(String oppgaveId, String prioritet) {
        this.oppgaveId = oppgaveId;
        this.prioritet = PrioritetType.valueOf(prioritet);
    }

    public Oppgave() {
    }

    public boolean erBehandling() {
        if (oppgavetype != null) {
            return oppgavetype.equals(Oppgavetype.BEH_SAK_MED) || oppgavetype.equals(Oppgavetype.BEH_SAK_MK_UFM);
        } else {
            return false;
        }
    }

    public boolean erJournalFøring() {
        if (oppgavetype != null) {
            return oppgavetype.equals(Oppgavetype.JFR_MED) || oppgavetype.equals(Oppgavetype.JFR_UFM);
        } else {
            return false;
        }
    }

    public boolean harHøyPrioritet() {
        return ((PrioritetType.HOY_MED.equals(prioritet)) || PrioritetType.HOY_UFM.equals(prioritet));
    }

    public String getAnsvarligId() {
        return ansvarligId;
    }

    public void setAnsvarligId(String ansvarligId) {
        this.ansvarligId = ansvarligId;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
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

    public void setFagomrade(Fagomrade fagomrade) {
        this.fagomrade = fagomrade;
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

    public void setPrioritet(PrioritetType prioritet) {
        this.prioritet = prioritet;
    }

    public String getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(String gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }
}
