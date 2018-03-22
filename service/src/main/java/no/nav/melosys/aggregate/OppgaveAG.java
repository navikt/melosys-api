package no.nav.melosys.aggregate;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.Oppgave;

public class OppgaveAG {

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
