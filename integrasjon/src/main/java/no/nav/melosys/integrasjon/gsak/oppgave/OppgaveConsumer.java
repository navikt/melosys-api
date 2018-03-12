package no.nav.melosys.integrasjon.gsak.oppgave;

import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;

public interface OppgaveConsumer {
    FinnOppgaveListeResponse finnOppgaveListe(FinnOppgaveListeRequestMal request);
}
