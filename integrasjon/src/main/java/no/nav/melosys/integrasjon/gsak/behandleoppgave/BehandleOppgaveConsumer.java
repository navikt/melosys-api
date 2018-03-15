package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSFerdigstillOppgaveException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSOppgaveIkkeFunnetException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSOptimistiskLasingException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.WSSikkerhetsbegrensningException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.*;

public interface BehandleOppgaveConsumer {

    void lagreOppgave(WSLagreOppgaveRequest request) throws WSOppgaveIkkeFunnetException, WSSikkerhetsbegrensningException, WSOptimistiskLasingException;

    WSFerdigstillOppgaveResponse ferdigstillOppgave(WSFerdigstillOppgaveRequest request) throws WSSikkerhetsbegrensningException, WSFerdigstillOppgaveException;

    WSOpprettOppgaveResponse opprettOppgave(WSOpprettOppgaveRequest request) throws WSSikkerhetsbegrensningException;
}
