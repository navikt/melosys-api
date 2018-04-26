package no.nav.melosys.integrasjon.joark;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.behandleinngaaendejournal.BehandleInngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JoarkService implements JoarkFasade {

    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;

    @Autowired
    public JoarkService(BehandleInngaaendeJournalConsumer behandleInngaaendeJournal, InngaaendeJournalConsumer inngaaendeJournal, JournalConsumer journal) {
        this.behandleInngaaendeJournalConsumer = behandleInngaaendeJournal;
        this.inngaaendeJournalConsumer = inngaaendeJournal;
        this.journalConsumer = journal;
    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) throws SikkerhetsbegrensningException {
        HentDokumentRequest request = new HentDokumentRequest();
        request.setDokumentId(dokumentID);
        request.setJournalpostId(journalPostID);

        Variantformater variantformat = new Variantformater();
        variantformat.setValue(Variantformat.ARKIV.toString());
        request.setVariantformat(variantformat);

        HentDokumentResponse hentDokumentResponse = null;
        try {
            hentDokumentResponse = journalConsumer.hentDokument(request);
        } catch (HentDokumentDokumentIkkeFunnet hentDokumentDokumentIkkeFunnet) {
            throw new IntegrasjonException(hentDokumentDokumentIkkeFunnet);
        } catch (HentDokumentSikkerhetsbegrensning hentDokumentSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentDokumentSikkerhetsbegrensning);
        } catch (HentDokumentJournalpostIkkeFunnet hentDokumentJournalpostIkkeFunnet) {
            throw new IntegrasjonException(hentDokumentJournalpostIkkeFunnet);
        }
        return hentDokumentResponse.getDokument();
    }
}
