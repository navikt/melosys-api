package no.nav.melosys.saksflyt;

import no.nav.melosys.saksflytapi.ProsessinstansOpprettetEvent;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessinstansOpprettetListener {
    private final ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate;
    private final ProsessinstansDispatcher prosessinstansDispatcher;

    public ProsessinstansOpprettetListener(ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate,
                                           ProsessinstansDispatcher prosessinstansDispatcher) {
        this.prosessinstansBehandlerDelegate = prosessinstansBehandlerDelegate;
        this.prosessinstansDispatcher = prosessinstansDispatcher;
    }

    // Oppdatering av entiteten må gjøres før commit, og ikke etter da det ikke finnes en transaksjon da
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void oppdaterProsessinstansstatus(ProsessinstansOpprettetEvent event) {
        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(event.hentProsessinstans());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
        Prosessinstans prosessinstans = event.hentProsessinstans();
        if (!prosessinstans.erPåVent()) {
            prosessinstansDispatcher.dispatch(prosessinstans);
        }
    }
}
