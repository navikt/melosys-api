package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.List;

import no.nav.tjeneste.virksomhet.oppgave.v3.binding.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeFilter;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeRequest;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeSok;

public class OppgaveConsumerImpl implements OppgaveConsumer {
    private OppgaveV3 port;

    public OppgaveConsumerImpl(OppgaveV3 port) {
        this.port = port;
    }

    @Override
    public FinnOppgaveListeResponse finnOppgaveListe(FinnOppgaveListeRequestMal request) {
        return port.finnOppgaveListe(convertToWSRequest(request));
    }

    private FinnOppgaveListeRequest convertToWSRequest(FinnOppgaveListeRequestMal request) {
        FinnOppgaveListeRequest result = new FinnOppgaveListeRequest();

        result.setSok(mapSok(request.getSok()));

        if (request.getFilter() != null) {
            result.setFilter(mapFilter(request.getFilter()));
        }

        if (! (request.getIkkeTidligereFordeltTil() == null)) {
            result.setIkkeTidligereFordeltTil(request.getIkkeTidligereFordeltTil());
        }
        return result;
    }

    private FinnOppgaveListeSok mapSok(FinnOppgaveListeSokMal sokMal) {
        FinnOppgaveListeSok oppgaveListeSok = new FinnOppgaveListeSok();
        oppgaveListeSok.setAnsvarligEnhetId(sokMal.getAnsvarligEnhetId());
        oppgaveListeSok.setBrukerId(sokMal.getBrukerId());
        oppgaveListeSok.setSakId(sokMal.getSakId());

        return oppgaveListeSok;
    }

    private FinnOppgaveListeFilter mapFilter(FinnOppgaveListeFilterMal filterMal) {
        FinnOppgaveListeFilter oppgaveListeFilter = new FinnOppgaveListeFilter();
        oppgaveListeFilter.setOpprettetEnhetId(filterMal.getOpprettetEnhetId());
        oppgaveListeFilter.setOpprettetEnhetNavn(filterMal.getOpprettetEnhetNavn());
        oppgaveListeFilter.setAnsvarligEnhetNavn(filterMal.getAnsvarligEnhetNavn());

        if (filterMal.getOppgavetypeKodeListe() != null) {
            List<String> oppgavetypeKodeListe = oppgaveListeFilter.getOppgavetypeKodeListe();
            oppgavetypeKodeListe.addAll(filterMal.getOppgavetypeKodeListe());
        }

        if (filterMal.getBrukertypeKodeListe() != null) {
            List<String> brukertypeKodeListe = oppgaveListeFilter.getBrukertypeKodeListe();
            brukertypeKodeListe.addAll(filterMal.getBrukertypeKodeListe());
        }

        return oppgaveListeFilter;
    }
}
