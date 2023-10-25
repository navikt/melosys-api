package no.nav.melosys.saksflyt;

import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessinstansOpprettetListener {
    private final ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate;
    private final ProsessinstansBehandler prosessinstansBehandler;

    public ProsessinstansOpprettetListener(ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate,
                                           ProsessinstansBehandler prosessinstansBehandler) {
        this.prosessinstansBehandlerDelegate = prosessinstansBehandlerDelegate;
        this.prosessinstansBehandler = prosessinstansBehandler;
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
            prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        }
    }
}
