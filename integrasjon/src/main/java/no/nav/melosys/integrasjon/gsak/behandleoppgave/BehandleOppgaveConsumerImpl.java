package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import no.nav.tjeneste.virksomhet.behandleoppgave.v1.*;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.*;

public class BehandleOppgaveConsumerImpl implements BehandleOppgaveConsumer {

    private BehandleOppgaveV1 port;

    public BehandleOppgaveConsumerImpl(BehandleOppgaveV1 port) {
        this.port = port;
    }

    @Override
    public void lagreOppgave(WSLagreOppgaveRequest request) throws WSOppgaveIkkeFunnetException, WSSikkerhetsbegrensningException, WSOptimistiskLasingException {
        port.lagreOppgave(request);
    }

    @Override
    public WSFerdigstillOppgaveResponse ferdigstillOppgave(WSFerdigstillOppgaveRequest request) throws WSSikkerhetsbegrensningException, WSFerdigstillOppgaveException {
        return port.ferdigstillOppgave(request);
    }

    @Override
    public WSOpprettOppgaveResponse opprettOppgave(WSOpprettOppgaveRequest request) throws WSSikkerhetsbegrensningException {
        return port.opprettOppgave(request);
    }
}
