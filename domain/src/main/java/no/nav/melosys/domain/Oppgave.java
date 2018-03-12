package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.gsak.PrioritetType;

/**
 * Den klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public class Oppgave {

    private String oppgaveId;
    //private Bruker gjelder;
    //private Status status;
    private LocalDate aktivFra;
    private LocalDate aktivTil;
    private String ansvarligId;
    //private Fagomrade fagomrade;
    //private Underkategori underkategori;
    //private Oppgavetype oppgavetype;
    private String beskrivelse;
    private String oppfolging;
    private PrioritetType prioritet;
    private Integer versjon;
    private String saksnummer;
    private String kravId;
    private String dokumentId;
    private String soknadsId;

    public Oppgave(String oppgaveId, String prioritet) {
        this.oppgaveId = oppgaveId;
        this.prioritet = PrioritetType.valueOf(prioritet);
    }

    public boolean harHøyPrioritet() {
        return (prioritet.equals(PrioritetType.HOY_MED) || prioritet.equals(PrioritetType.HOY_UFM));
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

}
