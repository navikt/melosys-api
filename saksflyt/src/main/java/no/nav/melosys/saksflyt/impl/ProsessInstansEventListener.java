package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessInstansEventListener {

    private final SaksflytAsyncArbeider saksflytAsyncArbeider;

    public ProsessInstansEventListener(SaksflytAsyncArbeider saksflytAsyncArbeider) {
        this.saksflytAsyncArbeider = saksflytAsyncArbeider;
    }

    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void settIBingen(ProsessinstansOpprettetEvent event) {
        saksflytAsyncArbeider.behandleProsessinstans(event.getProsessInstans());
    }
}
