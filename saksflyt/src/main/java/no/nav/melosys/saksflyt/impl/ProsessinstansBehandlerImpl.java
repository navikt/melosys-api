package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.saksflyt.prosessflyt.ProsessFlyt;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytFactory;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.sikkerhet.context.SaksflytSubjektHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProsessinstansBehandlerImpl implements ProsessinstansBehandler {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansBehandlerImpl.class);

    private final Map<ProsessSteg, StegBehandler> stegbehandlerMap = new EnumMap<>(ProsessSteg.class);
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansBehandlerImpl(Collection<StegBehandler> stegbehandlere, ProsessinstansRepository prosessinstansRepository) {
        stegbehandlere.forEach(s -> stegbehandlerMap.put(s.inngangsSteg(), s));
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void behandleProsessinstans(@NotNull Prosessinstans prosessinstans) {
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (!prosessinstans.statusErKlar()) {
            log.warn("Prosessinstans {} har status {}. Skal ikke behandles", prosessinstans.getId(), prosessinstans.getStatus());
            return;
        }

        ProsessflytFactory.lag(prosessinstans.getType()).ifPresentOrElse(
            prosessFlyt -> this.utførSteg(prosessinstans, prosessFlyt),
            () -> this.behandleFeilFlytIkkeFunnet(prosessinstans)
        );
    }

    private void utførSteg(Prosessinstans prosessinstans, ProsessFlyt prosessFlyt) {
        ProsessSteg nesteSteg = null;

        try {
            SaksflytSubjektHolder.set(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
            while ((nesteSteg = prosessFlyt.nesteSteg(prosessinstans.getSistFullførtSteg())) != null) {
                utførSteg(finnStegBehandler(nesteSteg), prosessinstans);
            }

            settTilFerdig(prosessinstans);
        } catch (Exception e) {
            behandleFeil(prosessinstans, nesteSteg, e);
        } finally {
            SaksflytSubjektHolder.reset();
        }
    }

    private void behandleFeilFlytIkkeFunnet(Prosessinstans prosessinstans) {
        log.error("Finner ingen definert flyt for ProsessType {}", prosessinstans.getType());
        prosessinstans.setStatus(ProsessStatus.FEILET);
        lagreProsessinstans(prosessinstans);
    }

    private void utførSteg(StegBehandler stegBehandler, Prosessinstans prosessinstans) throws MelosysException {
        log.info("Utfører steg {} for prosessinstans {}", stegBehandler.inngangsSteg(), prosessinstans.getId());
        stegBehandler.utfør(prosessinstans);
        prosessinstans.setSistFullførtSteg(stegBehandler.inngangsSteg());
        lagreProsessinstans(prosessinstans);
    }

    private void settTilFerdig(Prosessinstans prosessinstans) {
        log.info("Prosessinstans {} behandlet ferdig", prosessinstans.getId());
        prosessinstans.setStatus(ProsessStatus.FERDIG);
        lagreProsessinstans(prosessinstans);
    }

    private void behandleFeil(Prosessinstans prosessinstans, ProsessSteg steg, Exception e) {
        log.error("Feil ved behandling av prosessinstans {} på steg {}", prosessinstans.getId(), steg, e);
        prosessinstans.leggTilHendelse(steg, e);
        prosessinstans.setStatus(ProsessStatus.FEILET);
        lagreProsessinstans(prosessinstans);
    }

    private void lagreProsessinstans(Prosessinstans prosessinstans) {
        prosessinstans.setEndretDato(LocalDateTime.now());
        prosessinstansRepository.save(prosessinstans);
    }

    private StegBehandler finnStegBehandler(ProsessSteg prosessSteg) {
        return stegbehandlerMap.get(prosessSteg);
    }
}
