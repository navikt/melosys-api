package no.nav.melosys.saksflyt;

import java.time.LocalDateTime;
import java.util.*;
import javax.validation.constraints.NotNull;

import no.nav.melosys.saksflyt.prosessflyt.ProsessFlyt;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.ProsessinstansOpprettetEvent;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.saksflyt.ProsessinstansFerdigEvent;
import no.nav.melosys.sikkerhet.context.SaksflytSubjektHolder;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.MDCOperations.CORRELATION_ID;
import static no.nav.melosys.MDCOperations.putToMDC;
import static no.nav.melosys.saksflytapi.domain.ProsessStatus.UNDER_BEHANDLING;

@Component
public class ProsessinstansBehandler {

    public static final long ANTALL_TIMER_FØR_GJENOPPRETTELSE = 24;
    private static final Logger log = LoggerFactory.getLogger(ProsessinstansBehandler.class);

    private final Map<ProsessSteg, StegBehandler> stegbehandlerMap = new EnumMap<>(ProsessSteg.class);
    private final ProsessinstansRepository prosessinstansRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProsessinstansBehandler(Collection<StegBehandler> stegbehandlere,
                                   ProsessinstansRepository prosessinstansRepository,
                                   ApplicationEventPublisher applicationEventPublisher) {
        stegbehandlere.forEach(s -> stegbehandlerMap.put(s.inngangsSteg(), s));
        this.prosessinstansRepository = prosessinstansRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Async("saksflytThreadPoolTaskExecutor")
    public void behandleProsessinstans(@NotNull Prosessinstans prosessinstans) {
        log.info("Starter behandling av prosessinstans {} med lås {}", prosessinstans.getId(), prosessinstans.getLåsReferanse());

        if (prosessinstans.erFerdig() || prosessinstans.erFeilet()) {
            log.warn("Prosessinstans {} har status {}. Skal ikke behandles", prosessinstans.getId(), prosessinstans.getStatus());
            return;
        } else if (prosessinstans.erUnderBehandling()) {
            log.warn("Prosessinstans {} behandles allerede", prosessinstans.getId());
            return;
        }

        prosessinstans.setStatus(UNDER_BEHANDLING);
        lagreProsessinstans(prosessinstans);

        ProsessflytDefinisjon.finnFlytForProsessType(prosessinstans.getType()).ifPresentOrElse(
            prosessFlyt -> this.utførFlyt(prosessinstans, prosessFlyt),
            () -> this.behandleFlytIkkeFunnet(prosessinstans)
        );
    }

    @EventListener
    @Transactional
    public void gjenopprettProsesserSomHengerVedOppstart(ApplicationReadyEvent applicationReady) {
        Collection<Prosessinstans> prosesser = prosessinstansRepository.findAllByStatusIn(
            ProsessStatus.hentAktiveStatuser());
        List<Prosessinstans> prosessinstansSomHenger = prosesser.stream().filter(
            prosess -> prosess.getEndretDato().isBefore(
                LocalDateTime.now().minusHours(ANTALL_TIMER_FØR_GJENOPPRETTELSE))).toList();
        if (!prosessinstansSomHenger.isEmpty()) {
            log.info("Funnet {} prosessinstanse(r) som har hengt", prosessinstansSomHenger.size());
            prosessinstansSomHenger.forEach(this::gjenopprett);
        }
    }

    public void gjenopprett(Prosessinstans prosessinstans) {
        prosessinstans.setStatus(ProsessStatus.RESTARTET);
        prosessinstans.setEndretDato(LocalDateTime.now());
        prosessinstansRepository.save(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));
        log.warn("Prosessinstans {} gjenopprettet etter {} timer", prosessinstans.getId(), ANTALL_TIMER_FØR_GJENOPPRETTELSE);
    }

    private void utførFlyt(Prosessinstans prosessinstans, ProsessFlyt prosessFlyt) {
        ProsessSteg nesteSteg = null;

        try {
            MDC.put("pid", prosessinstans.getId().toString());
            putToMDC(CORRELATION_ID, prosessinstans.getData(ProsessDataKey.CORRELATION_ID_SAKSFLYT));
            SaksflytSubjektHolder.set(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
            while ((nesteSteg = prosessFlyt.nesteSteg(prosessinstans.getSistFullførtSteg())) != null) {
                prosessinstans = utførSteg(hentStegBehandler(nesteSteg), prosessinstans);
            }

            settTilFerdig(prosessinstans);
        } catch (Exception e) {
            behandleFeil(prosessinstans, nesteSteg, e);
        } finally {
            MDC.remove("pid");
            MDC.remove(CORRELATION_ID);
            SaksflytSubjektHolder.reset();
        }
    }

    private void behandleFlytIkkeFunnet(Prosessinstans prosessinstans) {
        log.error("Finner ingen definert flyt for ProsessType {}", prosessinstans.getType());
        prosessinstans.setStatus(ProsessStatus.FEILET);
        lagreProsessinstans(prosessinstans);
    }

    private Prosessinstans utførSteg(StegBehandler stegBehandler, Prosessinstans prosessinstans) {
        log.info("Utfører steg {} for prosessinstans {}", stegBehandler.inngangsSteg(), prosessinstans.getId());
        String saksbehandler = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER);
        String saksbehandlerNavn = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER_NAVN);
        ThreadLocalAccessInfo.beforeExecuteProcess(prosessinstans.getId(), stegBehandler.inngangsSteg().getKode(), saksbehandler, saksbehandlerNavn);
        try {
            stegBehandler.utfør(prosessinstans);
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(prosessinstans.getId());
        }
        prosessinstans.setSistFullførtSteg(stegBehandler.inngangsSteg());
        return lagreProsessinstans(prosessinstans);
    }

    private void settTilFerdig(Prosessinstans prosessinstans) {
        log.info("Prosessinstans {} behandlet ferdig", prosessinstans.getId());
        prosessinstans.setStatus(ProsessStatus.FERDIG);
        lagreProsessinstans(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansFerdigEvent(prosessinstans));
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
