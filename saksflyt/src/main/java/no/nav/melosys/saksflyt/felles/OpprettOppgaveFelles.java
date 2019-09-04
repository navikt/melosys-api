package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;

public final class OpprettOppgaveFelles {

    private OpprettOppgaveFelles() {
    }

    public static Oppgave lagOppgaveForManuellSedbehandling(String saksnummer, String aktørId, String journalpostId) {
        //Midlertidige verdier for oppgave satt til disse er nærmere avklart
        return new Oppgave.Builder()
            .setPrioritet(PrioritetType.NORM)
            .setTema(Tema.UFM)
            .setSaksnummer(saksnummer)
            .setAktørId(aktørId)
            .setJournalpostId(journalpostId)
            .setOppgavetype(Oppgavetyper.BEH_SED)
            .setBehandlingstema(Behandlingstema.EU_EOS).build();
    }
}
