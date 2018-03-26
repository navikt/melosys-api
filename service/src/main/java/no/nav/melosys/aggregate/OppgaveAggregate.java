package no.nav.melosys.aggregate;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

public class OppgaveAggregate {

    PersonDokument personDokument;
    SoeknadDokument soeknadDokument;
    Fagsak fagsak;
    Oppgave oppgave;

    public Oppgave getOppgave() {
        return oppgave;
    }

    public void setOppgave(Oppgave oppgave) {
        this.oppgave = oppgave;
    }

    public PersonDokument getPersonDokument() {
        return personDokument;
    }

    public void setPersonDokument(PersonDokument personDokument) {
        this.personDokument = personDokument;
    }

    public SoeknadDokument getSoeknadDokument() {
        return soeknadDokument;
    }

    public void setSoeknadDokument(SoeknadDokument
                                           soeknadDokument) {
        this.soeknadDokument = soeknadDokument;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public void setFagsak(Fagsak fagsak) {
        this.fagsak = fagsak;
    }

}
