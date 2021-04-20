package no.nav.melosys.saksflyt.impl;

import java.util.Comparator;
import java.util.Set;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.SedLåsReferanse;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.service.saksflyt.ProsessinstansFerdigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansFerdigListener {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansFerdigListener.class);

    private final ProsessinstansRepository prosessinstansRepository;
    private final ProsessinstansBehandler prosessinstansBehandler;

    public ProsessinstansFerdigListener(ProsessinstansRepository prosessinstansRepository,
                                        ProsessinstansBehandler prosessinstansBehandler) {
        this.prosessinstansRepository = prosessinstansRepository;
        this.prosessinstansBehandler = prosessinstansBehandler;
    }

    @EventListener
    public void prosessinstansFerdig(ProsessinstansFerdigEvent prosessinstansFerdigEvent) {
        log.info("Prosessinstans {} ferdig", prosessinstansFerdigEvent.getUuid());
        if (prosessinstansFerdigEvent.getLåsReferanse() != null && !finnesAktivReferanse(prosessinstansFerdigEvent.getLåsReferanse())) {
            startNesteProsessinstans(prosessinstansFerdigEvent);
        }
    }

    private boolean finnesAktivReferanse(String referanse) {
        return prosessinstansRepository.existsByStatusNotInAndLåsReferanse(Set.of(ProsessStatus.FERDIG), referanse);
    }

    private void startNesteProsessinstans(ProsessinstansFerdigEvent prosessinstansFerdigEvent) {
        log.info("Forsøker å starte neste prosessinstans, låsreferanse {}", prosessinstansFerdigEvent.getLåsReferanse());
        var ferdigReferanse = new SedLåsReferanse(prosessinstansFerdigEvent.getLåsReferanse());

        var prosessinstanserPåVent = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT);

        prosessinstanserPåVent.stream()
            .filter(p -> harSammeReferanse(p, ferdigReferanse))
            .min(Comparator.comparing(Prosessinstans::getRegistrertDato))
            .ifPresent(this::oppdaterStatusOgBehandleProsessinstans);
    }

    private void oppdaterStatusOgBehandleProsessinstans(Prosessinstans prosessinstans) {
        log.info("Prosessinstans {} startes opp etter å ha vært på vent", prosessinstans.getId());
        prosessinstans.setStatus(ProsessStatus.KLAR);
        prosessinstansRepository.save(prosessinstans);
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
    }

    private boolean harSammeReferanse(Prosessinstans prosessinstans, SedLåsReferanse ferdigLåsreferanse) {
        var låsReferanse = new SedLåsReferanse(prosessinstans.getLåsReferanse());
        return låsReferanse.getReferanse().equals(ferdigLåsreferanse.getReferanse());
    }
}
