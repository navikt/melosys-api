package no.nav.melosys.integrasjon.joark.inngaaendejournal;

import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.UtledJournalfoeringsbehovJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.UtledJournalfoeringsbehovJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.UtledJournalfoeringsbehovJournalpostKanIkkeBehandles;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.UtledJournalfoeringsbehovSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.UtledJournalfoeringsbehovUgyldigInput;
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
