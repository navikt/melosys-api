package no.nav.melosys.saksflyt;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.saksflytapi.domain.SedLåsReferanse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansBehandlerDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansBehandlerDelegate.class);

    private final ProsessinstansBehandler prosessinstansBehandler;
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansBehandlerDelegate(ProsessinstansBehandler prosessinstansBehandler, ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansBehandler = prosessinstansBehandler;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    public void behandleProsessinstans(Prosessinstans prosessinstans) {
        oppdaterStatusOmSkalPåVent(prosessinstans);
        if (!prosessinstans.erPåVent()) {
            prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        }
    }

    void oppdaterStatusOmSkalPåVent(Prosessinstans prosessinstans) {
        if (skalSettesPåVent(prosessinstans)) {
            prosessinstans.setStatus(ProsessStatus.PÅ_VENT);
            prosessinstans.setEndretDato(LocalDateTime.now());
            prosessinstansRepository.save(prosessinstans);
            log.info("Prosessinstans {} satt på vent", prosessinstans.getId());
        }
    }

    /*
    Settes på vent om det finnes en prosessinstans med samme referanse,
     men ikke lik identifikator i prosess (ikke på vent/ferdig).

    Settes ikke på vent om
        1. Prosessinstansen ikke har en låsreferanse
        2. Det finnes ingen prosessinstans med samme referanse
        3. Det finnes en prosessinstans med lik referanse og identifikator.
     */
    private boolean skalSettesPåVent(Prosessinstans prosessinstans) {
        if (prosessinstans.getLåsReferanse() == null) {
            return false;
        }

        final var låsReferanse = new SedLåsReferanse(prosessinstans.getLåsReferanse());
        final var aktiveLåsReferanser = finnAndreAktiveLåsMedSammeReferanse(prosessinstans.getId(), låsReferanse);

        if (aktiveLåsReferanser.contains(låsReferanse)) {
            return false;
        }
        return aktiveLåsReferanser.stream().anyMatch(
            sedLåsReferanse -> sedLåsReferanse.getReferanse().equals(låsReferanse.getReferanse()));
    }

    private Collection<SedLåsReferanse> finnAndreAktiveLåsMedSammeReferanse(UUID id, SedLåsReferanse låsReferanse) {
        return prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
            id, Set.of(ProsessStatus.PÅ_VENT, ProsessStatus.FERDIG), låsReferanse.getReferanse()
        )
            .stream()
            .map(p -> new SedLåsReferanse(p.getLåsReferanse()))
            .collect(Collectors.toSet());
    }
}
