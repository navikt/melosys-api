package no.nav.melosys.integrasjon.joark.journal;


import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentURLDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentURLSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.JournalV3;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentURLRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentURLResponse;

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
}
