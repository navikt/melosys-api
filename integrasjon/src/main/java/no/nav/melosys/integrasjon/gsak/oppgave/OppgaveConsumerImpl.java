package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.List;

import no.nav.tjeneste.virksomhet.oppgave.v3.binding.HentOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgave.v3.binding.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.*;

public class OppgaveConsumerImpl implements OppgaveConsumer {
    private OppgaveV3 port;

    public OppgaveConsumerImpl(OppgaveV3 port) {
        this.port = port;
    }

    @Override
    public FinnOppgaveListeResponse finnOppgaveListe(FinnOppgaveListeRequestMal request) {
        return port.finnOppgaveListe(convertToWSRequest(request));
    }

    @Override
    public HentOppgaveResponse hentOppgave(HentOppgaveRequest request) throws HentOppgaveOppgaveIkkeFunnet {
        return port.hentOppgave(request);
    }

    private FinnOppgaveListeRequest convertToWSRequest(FinnOppgaveListeRequestMal request) {
        FinnOppgaveListeRequest result = new FinnOppgaveListeRequest();

        result.setSok(mapSok(request.getSok()));

        if (request.getFilter() != null) {
            result.setFilter(mapFilter(request.getFilter()));
        }

        if (request.getSortering() != null) {
            result.setSorteringKode(request.getSortering());
        }

        if (request.getIkkeTidligereFordeltTil() != null) {
            result.setIkkeTidligereFordeltTil(request.getIkkeTidligereFordeltTil());
        }
        return result;
    }

    private FinnOppgaveListeSok mapSok(FinnOppgaveListeSokMal sokMal) {
        FinnOppgaveListeSok oppgaveListeSok = new FinnOppgaveListeSok();

        if (sokMal.getFagområdeKodeListe() != null) {
            oppgaveListeSok.getFagomradeKodeListe().addAll(sokMal.getFagområdeKodeListe());
        }

        oppgaveListeSok.setAnsvarligEnhetId(sokMal.getAnsvarligEnhetId());
        oppgaveListeSok.setBrukerId(sokMal.getBrukerId());
        oppgaveListeSok.setSakId(sokMal.getSakId());

        return oppgaveListeSok;
    }

    private FinnOppgaveListeFilter mapFilter(FinnOppgaveListeFilterMal filterMal) {
        FinnOppgaveListeFilter oppgaveListeFilter = new FinnOppgaveListeFilter();

        if (filterMal.getAktiv() != null) {
            oppgaveListeFilter.setAktiv(filterMal.getAktiv());
        }

        if (filterMal.getUfordelte() != null) {
            oppgaveListeFilter.setUfordelte(filterMal.getUfordelte());
        }

        if (filterMal.getUnderkategoriKode() != null) {
            oppgaveListeFilter.setUnderkategoriKode(filterMal.getUnderkategoriKode());
        }

        if (filterMal.getOppgavetypeKodeListe() != null) {
            List<String> oppgavetypeKodeListe = oppgaveListeFilter.getOppgavetypeKodeListe();
            oppgavetypeKodeListe.addAll(filterMal.getOppgavetypeKodeListe());
        }

        return oppgaveListeFilter;
    }
}
