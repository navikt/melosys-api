package no.nav.melosys.integrasjon.joark.behandleinngaaendejournal;

import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringFerdigstillingIkkeMulig;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringObjektIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.FerdigstillJournalfoeringUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostObjektIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostOppdateringIkkeMulig;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.OppdaterJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest;

public class BehandleInngaaendeJournalConsumerImpl implements BehandleInngaaendeJournalConsumer {

    private BehandleInngaaendeJournalV1 port;

    public BehandleInngaaendeJournalConsumerImpl(BehandleInngaaendeJournalV1 port) {
        this.port = port;
    }

    @Override
    public void ferdigstillJournalfoering(FerdigstillJournalfoeringRequest request) throws FerdigstillJournalfoeringFerdigstillingIkkeMulig, FerdigstillJournalfoeringJournalpostIkkeInngaaende, FerdigstillJournalfoeringUgyldigInput, FerdigstillJournalfoeringSikkerhetsbegrensning, FerdigstillJournalfoeringObjektIkkeFunnet {
        port.ferdigstillJournalfoering(request);
    }

    @Override
    public void oppdaterJournalpost(OppdaterJournalpostRequest request) throws OppdaterJournalpostSikkerhetsbegrensning, OppdaterJournalpostOppdateringIkkeMulig, OppdaterJournalpostUgyldigInput, OppdaterJournalpostJournalpostIkkeInngaaende, OppdaterJournalpostObjektIkkeFunnet {
        port.oppdaterJournalpost(request);
    }
}
