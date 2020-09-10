package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.saksflyt.api.ProsessinstansKø;
import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessInstansEventListener {

    private final ProsessinstansKø binge;

    public ProsessInstansEventListener(ProsessinstansKø binge) {
        this.binge = binge;
    }

    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void settIBingen(ProsessinstansOpprettetEvent event) {
        binge.leggTil(event.getProsessInstans());
    }

}
