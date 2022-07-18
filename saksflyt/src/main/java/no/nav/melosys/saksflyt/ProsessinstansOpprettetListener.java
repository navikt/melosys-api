package no.nav.melosys.saksflyt;

import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessinstansOpprettetListener {

    private final BehandleProsessinstansDelegate behandleProsessinstansDelegate;

    public ProsessinstansOpprettetListener(BehandleProsessinstansDelegate behandleProsessinstansDelegate) {
        this.behandleProsessinstansDelegate = behandleProsessinstansDelegate;
    }

    // Oppdatering av entiteten må gjøres før commit, og ikke etter da det ikke finnes en transaksjon da
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void oppdaterProsessinstansstatus(ProsessinstansOpprettetEvent event) {
        behandleProsessinstansDelegate.oppdaterStatusOmSkalPåVent(event.hentProsessinstans());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
        behandleProsessinstansDelegate.behandleProsessinstansHvisKlar(event.hentProsessinstans());
    }
}
