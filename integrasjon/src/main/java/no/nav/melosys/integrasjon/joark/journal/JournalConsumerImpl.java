package no.nav.melosys.integrasjon.joark.journal;

import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentURLDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentURLSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentURLRequest;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentURLResponse;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentJournalpostListeResponse;

public class JournalConsumerImpl implements JournalConsumer {
    private JournalV2 port;

    public JournalConsumerImpl(JournalV2 port) {
        this.port = port;
    }

    @Override
    public HentDokumentResponse hentDokument(HentDokumentRequest request) throws HentDokumentDokumentIkkeFunnet, HentDokumentSikkerhetsbegrensning {
        return port.hentDokument(request);
    }

    @Override
    public HentJournalpostListeResponse hentJournalpostListe(HentJournalpostListeRequest request) throws HentJournalpostListeSikkerhetsbegrensning {
        return port.hentJournalpostListe(request);
    }

    @Override
    public HentDokumentURLResponse hentDokumentURL(HentDokumentURLRequest request) throws HentDokumentURLDokumentIkkeFunnet, HentDokumentURLSikkerhetsbegrensning {
        return port.hentDokumentURL(request);
    }

}
