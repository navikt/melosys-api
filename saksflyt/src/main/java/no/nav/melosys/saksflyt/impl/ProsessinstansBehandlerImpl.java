package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.*;
import javax.validation.constraints.NotNull;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.saksflyt.prosessflyt.ProsessFlyt;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.hendelser.FeiletHendelse;
import no.nav.melosys.sikkerhet.context.SaksflytSubjektHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansBehandlerImpl implements ProsessinstansBehandler, FeiletHendelseHandler {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansBehandlerImpl.class);

    private final Map<ProsessSteg, StegBehandler> stegbehandlerMap = new EnumMap<>(ProsessSteg.class);
    private final ProsessinstansRepository prosessinstansRepository;
    private final BehandlingService behandlingService;

    public ProsessinstansBehandlerImpl(Collection<StegBehandler> stegbehandlere, ProsessinstansRepository prosessinstansRepository, BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
        stegbehandlere.forEach(s -> stegbehandlerMap.put(s.inngangsSteg(), s));
        this.prosessinstansRepository = prosessinstansRepository;
    }

    public void behandleProsessinstans(@NotNull Prosessinstans prosessinstans) {
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());

        if (!prosessinstans.statusErKlarEllerRestartet()) {
            log.warn("Prosessinstans {} har status {}. Skal ikke behandles", prosessinstans.getId(), prosessinstans.getStatus());
            return;
        }

        ProsessflytDefinisjon.finnFlytForProsessType(prosessinstans.getType()).ifPresentOrElse(
            prosessFlyt -> this.utførFlyt(prosessinstans, prosessFlyt),
            () -> this.behandleFlytIkkeFunnet(prosessinstans)
        );
    }

    private void utførFlyt(Prosessinstans prosessinstans, ProsessFlyt prosessFlyt) {
        ProsessSteg nesteSteg = null;

        try {
            MDC.put("pid", prosessinstans.getId().toString());
            SaksflytSubjektHolder.set(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
            while ((nesteSteg = prosessFlyt.nesteSteg(prosessinstans.getSistFullførtSteg())) != null) {
                prosessinstans = utførSteg(hentStegBehandler(nesteSteg), prosessinstans);
            }

            settTilFerdig(prosessinstans);
        } catch (Exception e) {
            behandleFeil(prosessinstans, nesteSteg, e);
        } finally {
            MDC.remove("pid");
            SaksflytSubjektHolder.reset();
        }
    }

    @EventListener
    public void behandleFeiletHendelse(FeiletHendelse feiletHendelse) throws IkkeFunnetException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
        prosessinstans.setRegistrertDato(LocalDateTime.now());
        prosessinstans.setEndretDato(LocalDateTime.now());
        prosessinstans.setType(ProsessType.FEILET_HENDELSE);

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(feiletHendelse.getSourceEvent().getBehandlingID());
        prosessinstans.setBehandling(behandling);

        behandleFeil(prosessinstans, ProsessSteg.UKJENT, feiletHendelse.getFeil());
    }

    private void behandleFlytIkkeFunnet(Prosessinstans prosessinstans) {
        if (erFeiletHendelse(prosessinstans.getType())) {
            settTilFerdig(prosessinstans);
        } else {
            log.error("Finner ingen definert flyt for ProsessType {}", prosessinstans.getType());
            prosessinstans.setStatus(ProsessStatus.FEILET);
            lagreProsessinstans(prosessinstans);
        }
    }

    private static boolean erFeiletHendelse(ProsessType type) {
        return ProsessType.FEILET_HENDELSE == type;
    }

    private Prosessinstans utførSteg(StegBehandler stegBehandler, Prosessinstans prosessinstans) throws MelosysException {
        log.info("Utfører steg {} for prosessinstans {}", stegBehandler.inngangsSteg(), prosessinstans.getId());
        stegBehandler.utfør(prosessinstans);
        prosessinstans.setSistFullførtSteg(stegBehandler.inngangsSteg());
        return lagreProsessinstans(prosessinstans);
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

    private Prosessinstans lagreProsessinstans(Prosessinstans prosessinstans) {
        prosessinstans.setEndretDato(LocalDateTime.now());
        return prosessinstansRepository.save(prosessinstans);
    }

    private StegBehandler hentStegBehandler(ProsessSteg prosessSteg) {
        return Optional.ofNullable(stegbehandlerMap.get(prosessSteg))
            .orElseThrow(() -> new NoSuchElementException("Finner ingen stegbehandler for prosessteg " + prosessSteg));
    }
}
