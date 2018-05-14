package no.nav.melosys.integrasjon.gsak.oppgave.dto;

import java.util.Collections;
import java.util.List;

public class OppgaveSvar {
    private int antallTreffTotalt;
    private List<OppgaveDto> oppgaver;

    public int getAntallTreffTotalt() {
        return antallTreffTotalt;
    }

    public void setAntallTreffTotalt(int antallTreffTotalt) {
        this.antallTreffTotalt = antallTreffTotalt;
    }

    public List<OppgaveDto> getOppgaver() {
        return Collections.unmodifiableList(oppgaver);
    }

    public void setOppgaver(List<OppgaveDto> oppgaver) {
        this.oppgaver = Collections.unmodifiableList(oppgaver);
    }
}
