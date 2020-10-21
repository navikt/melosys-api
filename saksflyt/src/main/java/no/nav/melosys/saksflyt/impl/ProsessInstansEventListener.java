package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.saksflyt.api.ProsessinstansKø;
import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessInstansEventListener {

    private final ProsessinstansKø prosessinstansKø;

    public ProsessInstansEventListener(ProsessinstansKø prosessinstansKø) {
        this.prosessinstansKø = prosessinstansKø;
    }

    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void settIBingen(ProsessinstansOpprettetEvent event) {
        prosessinstansKø.leggTil(event.getProsessInstans());
    }

}
