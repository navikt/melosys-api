package no.nav.melosys.integrasjon.joark.inngaaendejournal;

import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.HentJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.InngaaendeJournalV1;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.UtledJournalfoeringsbehovJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.UtledJournalfoeringsbehovJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.UtledJournalfoeringsbehovJournalpostKanIkkeBehandles;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.UtledJournalfoeringsbehovSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.UtledJournalfoeringsbehovUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovResponse;

public class InngaaendeJournalConsumerImpl implements InngaaendeJournalConsumer {

    private InngaaendeJournalV1 port;

    public InngaaendeJournalConsumerImpl(InngaaendeJournalV1 port) {
        this.port = port;
    }

    @Override
    public HentJournalpostResponse hentJournalpost(HentJournalpostRequest request) throws HentJournalpostJournalpostIkkeFunnet, HentJournalpostJournalpostIkkeInngaaende, HentJournalpostSikkerhetsbegrensning, HentJournalpostUgyldigInput {
        return port.hentJournalpost(request);
    }

    @Override
    public UtledJournalfoeringsbehovResponse utledJournalfoeringsbehov(UtledJournalfoeringsbehovRequest request) throws UtledJournalfoeringsbehovSikkerhetsbegrensning, UtledJournalfoeringsbehovUgyldigInput, UtledJournalfoeringsbehovJournalpostKanIkkeBehandles, UtledJournalfoeringsbehovJournalpostIkkeFunnet, UtledJournalfoeringsbehovJournalpostIkkeInngaaende {
        return port.utledJournalfoeringsbehov(request);
    }
}
