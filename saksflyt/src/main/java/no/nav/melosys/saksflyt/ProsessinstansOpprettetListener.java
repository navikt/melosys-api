package no.nav.melosys.saksflyt;

import java.util.UUID;

import no.nav.melosys.saksflytapi.ProsessinstansOpprettetEvent;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProsessinstansOpprettetListener {
    private static final Logger log = LoggerFactory.getLogger(ProsessinstansOpprettetListener.class);

    private final ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate;
    private final ProsessinstansBehandler prosessinstansBehandler;
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansOpprettetListener(ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate,
                                           ProsessinstansBehandler prosessinstansBehandler,
                                           ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansBehandlerDelegate = prosessinstansBehandlerDelegate;
        this.prosessinstansBehandler = prosessinstansBehandler;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    // Oppdatering av entiteten må gjøres før commit, og ikke etter da det ikke finnes en transaksjon da
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void oppdaterProsessinstansstatus(ProsessinstansOpprettetEvent event) {
        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(event.hentProsessinstans());
    }

    /**
     * Starter behandling av prosessinstans etter at transaksjonen som opprettet den er committed.
     * <p>
     * VIKTIG: Vi laster prosessinstansen på nytt fra databasen i stedet for å bruke objektet fra eventen.
     * Dette sikrer at vi har ferske entitetsreferanser (spesielt Behandling) og unngår race conditions
     * der saga-steg kunne fått stale data fra HTTP-transaksjonens Hibernate-sesjon.
     *
     * @see <a href="docs/prosessinstans-reload-fix.md">Prosessinstans Reload Fix dokumentasjon</a>
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void behandleOpprettetProsessinstans(ProsessinstansOpprettetEvent event) {
        UUID prosessinstansId = event.hentProsessinstans().getId();

        // Last prosessinstans på nytt fra DB for å få ferske entitetsreferanser
        // Dette forhindrer race conditions med stale Behandling-referanser
        Prosessinstans prosessinstans = prosessinstansRepository.findById(prosessinstansId)
            .orElseThrow(() -> {
                log.error("Prosessinstans {} ikke funnet i database etter commit", prosessinstansId);
                return new IllegalStateException("Prosessinstans ikke funnet etter commit: " + prosessinstansId);
            });

        if (!prosessinstans.erPåVent()) {
            prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        }
    }
}
