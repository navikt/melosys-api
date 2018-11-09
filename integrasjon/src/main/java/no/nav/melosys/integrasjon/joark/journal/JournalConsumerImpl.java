package no.nav.melosys.integrasjon.joark.journal;


import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.*;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentURLResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;

public class JournalConsumerImpl implements JournalConsumer {
    private JournalV3 port;

    public JournalConsumerImpl(JournalV3 port) {
        this.port = port;
    }

    @Override
    public HentDokumentResponse hentDokument(HentDokumentRequest request) throws HentDokumentDokumentIkkeFunnet, HentDokumentJournalpostIkkeFunnet, HentDokumentSikkerhetsbegrensning  {
        return port.hentDokument(request);
    }

    @Override
    public HentDokumentURLResponse hentDokumentURL(HentDokumentURLRequest request) throws HentDokumentURLDokumentIkkeFunnet, HentDokumentURLSikkerhetsbegrensning {
        return port.hentDokumentURL(request);
    }

    @Override
    public HentKjerneJournalpostListeResponse hentKjerneJournalpostListe(HentKjerneJournalpostListeRequest request) throws HentKjerneJournalpostListeSikkerhetsbegrensning, HentKjerneJournalpostListeUgyldigInput {
        return port.hentKjerneJournalpostListe(request);
    }
}
