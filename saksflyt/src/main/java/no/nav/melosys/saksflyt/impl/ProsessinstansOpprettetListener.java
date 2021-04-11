package no.nav.melosys.saksflyt.impl;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansLåsReferanse;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessinstansOpprettetListener {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansOpprettetListener.class);

    private final ProsessinstansBehandler prosessinstansBehandler;
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansOpprettetListener(ProsessinstansBehandler prosessinstansBehandler,
                                           ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansBehandler = prosessinstansBehandler;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void oppdaterProsessinstansstatus(ProsessinstansOpprettetEvent event) {
        if (skalSettesPåVent(event.hentProsessinstans())) {
            event.hentProsessinstans().setStatus(ProsessStatus.PÅ_VENT);
            log.info("Prosessinstans {} satt på vent", event.hentProsessinstans().getId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
        if (event.hentProsessinstans().getStatus() != ProsessStatus.PÅ_VENT) {
            prosessinstansBehandler.behandleProsessinstans(event.hentProsessinstans());
        }
    }

    private boolean skalSettesPåVent(Prosessinstans prosessinstans) {
        if (prosessinstans.getLåsType() == null || prosessinstans.getLåsReferanse() == null) {
            return false;
        }

        final var låsReferanse = ProsessinstansLåsReferanse.tilReferanseObjekt(prosessinstans.getLåsType(), prosessinstans.getLåsReferanse());

        final var aktiveLåsReferanser = prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(Set.of(ProsessStatus.FERDIG), låsReferanse.getReferanse()).stream()
            .filter(p -> !p.getUuid().equals(prosessinstans.getId()))
            .map(p -> ProsessinstansLåsReferanse.tilReferanseObjekt(p.getLåsType(), p.getLåsReferanse()))
            .collect(Collectors.toSet());

        if (aktiveLåsReferanser.contains(låsReferanse)) {
            return false;
        } else {
            return aktiveLåsReferanser.stream().anyMatch(p -> p.getReferanse().equals(låsReferanse.getReferanse()));
        }
    }
}
