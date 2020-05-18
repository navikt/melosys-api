package no.nav.melosys.saksflyt.steg.aou.ut.svar;

import java.util.Collections;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vedtak.VedtakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.metrics.MetrikkerNavn.SVAR_AOU;
import static no.nav.melosys.metrics.MetrikkerNavn.TAG_RESULTAT;

@Component
public class OppdaterBehandling extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandling.class);

    private static final String INNVILGELSE = "godkjent";
    private static final String INNVILGELSE_YTTERLIGEREINFO = "godkjentmedinfo";
    private static final String DELVIS_INNVILGELSE = "delvisinnvilgelse";
    private static final String AVSLAG = "avslag";

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final VedtakService vedtakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public OppdaterBehandling(AnmodningsperiodeService anmodningsperiodeService,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService,
                              @Qualifier("system") VedtakService vedtakService,
                              LovvalgsperiodeService lovvalgsperiodeService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.vedtakService = vedtakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    static {
        Stream.of(INNVILGELSE, INNVILGELSE_YTTERLIGEREINFO, DELVIS_INNVILGELSE, AVSLAG).forEach(s -> Metrics.counter(SVAR_AOU, TAG_RESULTAT, s));
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_SVAR_OPPDATER_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());

        final long behandlingID = prosessinstans.getBehandling().getId();
        Anmodningsperiode anmodningsperiode = anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream().findFirst()
            .orElseThrow(() -> new TekniskException("Finner ingen anmodningsperiode for behandling " + behandlingID));
        boolean erInnvilgelse = anmodningsperiode.getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType() == Anmodningsperiodesvartyper.INNVILGELSE;
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID,
            Collections.singleton(Lovvalgsperiode.av(anmodningsperiode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG))
        );

        if (erInnvilgelse && !inneholderYtterligereInformasjon(melosysEessiMelding)) {
            log.info("Mottatt svar {} på anmodning om unntak for behandling {}. Iverksetter vedtak",
                Anmodningsperiodesvartyper.INNVILGELSE, behandlingID);
            fattVedtak(behandlingID);
        } else {
            log.info("Mottatt svar {} på anmodning om unntak for behandling {}. Endrer behandlingsstatus til {}",
                anmodningsperiode.getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType(),
                behandlingID,
                Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
            oppdaterBehandlingsstatusUnderBehandling(behandlingID);
        }
        registrerMetrikk(melosysEessiMelding);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }

    private void fattVedtak(long behandlingID) throws MelosysException {
        try {
            vedtakService.fattVedtak(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        } catch (ValideringException e) {
            log.info("Kan ikke fatte vedtak automatisk pga. treff i vedtakkontroller: {}. Endrer behandlingsstatus til {}",
                String.join(", ", e.getFeilkoder()),
                Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
            oppdaterBehandlingsstatusUnderBehandling(behandlingID);
            return;
        }

        behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.DELVIS_AUTOMATISERT);
    }

    private void oppdaterBehandlingsstatusUnderBehandling(long behandlingID) throws FunksjonellException, TekniskException {
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
    }

    private boolean inneholderYtterligereInformasjon(MelosysEessiMelding melosysEessiMelding) {
        return StringUtils.isNotEmpty(melosysEessiMelding.getYtterligereInformasjon());
    }

    private void registrerMetrikk(MelosysEessiMelding melosysEessiMelding) {
        SvarAnmodningUnntak.Beslutning beslutning = melosysEessiMelding.getSvarAnmodningUnntak().getBeslutning();
        if (beslutning == SvarAnmodningUnntak.Beslutning.AVSLAG) {
            Metrics.counter(SVAR_AOU, TAG_RESULTAT, AVSLAG).increment();
        } else if (beslutning == SvarAnmodningUnntak.Beslutning.DELVIS_INNVILGELSE) {
            Metrics.counter(SVAR_AOU, TAG_RESULTAT, DELVIS_INNVILGELSE).increment();
        } else if (beslutning == SvarAnmodningUnntak.Beslutning.INNVILGELSE) {
            Metrics.counter(
                SVAR_AOU, TAG_RESULTAT, inneholderYtterligereInformasjon(melosysEessiMelding) ? INNVILGELSE_YTTERLIGEREINFO : INNVILGELSE
            ).increment();
        }
    }
}
