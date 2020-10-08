package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessInstansEventListener {

    private final SaksflytAsyncDelegate saksflytAsyncDelegate;

    public ProsessInstansEventListener(SaksflytAsyncDelegate saksflytAsyncDelegate) {
        this.saksflytAsyncDelegate = saksflytAsyncDelegate;
    }

    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void behandleProsessinstansAsynkront(ProsessinstansOpprettetEvent event) {
        saksflytAsyncDelegate.behandleProsessinstans(event.getProsessInstans());
    }
}
