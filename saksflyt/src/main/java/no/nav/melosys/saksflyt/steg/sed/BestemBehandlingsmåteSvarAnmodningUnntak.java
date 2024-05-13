package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
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
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.service.vedtak.VedtaksfattingFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.metrics.MetrikkerNavn.SVAR_AOU;
import static no.nav.melosys.metrics.MetrikkerNavn.TAG_RESULTAT;

@Component
public class BestemBehandlingsmåteSvarAnmodningUnntak implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(BestemBehandlingsmåteSvarAnmodningUnntak.class);

    private static final String INNVILGELSE_AUTO_VEDTAK = "godkjent.automatisk";
    private static final String INNVILGELSE_MANUELL_BEHANDLING = "godkjent.manuellbehandling";
    private static final String DELVIS_INNVILGELSE = "delvisinnvilgelse";
    private static final String AVSLAG = "avslag";

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final VedtaksfattingFasade vedtaksfattingFasade;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;

    public BestemBehandlingsmåteSvarAnmodningUnntak(AnmodningsperiodeService anmodningsperiodeService,
                                                    BehandlingService behandlingService,
                                                    BehandlingsresultatService behandlingsresultatService,
                                                    VedtaksfattingFasade vedtaksfattingFasade,
                                                    LovvalgsperiodeService lovvalgsperiodeService,
                                                    FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.vedtaksfattingFasade = vedtaksfattingFasade;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
    }

    static {
        Stream.of(INNVILGELSE_AUTO_VEDTAK, INNVILGELSE_MANUELL_BEHANDLING, DELVIS_INNVILGELSE, AVSLAG).forEach(s -> Metrics.counter(SVAR_AOU, TAG_RESULTAT, s));
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.BESTEM_BEHANDLINGSMÅTE_SVAR_ANMODNING_UNNTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        Anmodningsperiode anmodningsperiode = anmodningsperiodeService.hentAnmodningsperioder(behandlingID)
            .stream().findFirst()
            .orElseThrow(() -> new TekniskException("Finner ingen anmodningsperiode for behandling " + behandlingID));
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID,
            Collections.singleton(Lovvalgsperiode.av(anmodningsperiode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG))
        );

        Collection<Kontrollfeil> kontrollfeil = ferdigbehandlingKontrollFacade.kontroller(behandlingID, true, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, Collections.emptySet());

        boolean vedtakFattesAutomatisk = vedtakFattesAutomatisk(behandlingID, anmodningsperiode, melosysEessiMelding);
        if (kontrollfeil.isEmpty() && vedtakFattesAutomatisk) {
            log.info("Mottatt svar {} på anmodning om unntak for behandling {}. Iverksetter vedtak",
                Anmodningsperiodesvartyper.INNVILGELSE, behandlingID);
            fattVedtak(behandlingID);
        } else {
            log.info("Mottatt svar {} på anmodning om unntak for behandling {}. Endrer behandlingsstatus til {}",
                anmodningsperiode.getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType(),
                behandlingID,
                Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
            behandlingService.endreStatus(behandlingID, Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
        }
        registrerMetrikk(melosysEessiMelding, vedtakFattesAutomatisk);
    }

    private boolean vedtakFattesAutomatisk(long behandlingID,
                                           Anmodningsperiode anmodningsperiode,
                                           MelosysEessiMelding melosysEessiMelding) {
        return anmodningsperiode.getAnmodningsperiodeSvar().erInnvilgelse()
            && !melosysEessiMelding.inneholderYtterligereInformasjon()
            && behandlingService.hentBehandlingMedSaksopplysninger(behandlingID).harStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
    }

    private void fattVedtak(long behandlingID) {
        try {
            vedtaksfattingFasade.fattVedtak(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
            behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.DELVIS_AUTOMATISERT);
        } catch (ValideringException e) {
            log.info("Kan ikke fatte vedtak automatisk pga. treff i vedtakkontroller: {}. Endrer behandlingsstatus til {}",
                e.getFeilkoder().stream().map(KontrollfeilDto::getKode).collect(Collectors.joining(", ")),
                Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
            behandlingService.endreStatus(behandlingID, Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
        }
    }

    private void registrerMetrikk(MelosysEessiMelding melosysEessiMelding, boolean vedtakFattesAutomatisk) {
        SvarAnmodningUnntak.Beslutning beslutning = melosysEessiMelding.getSvarAnmodningUnntak().getBeslutning();
        if (beslutning == SvarAnmodningUnntak.Beslutning.AVSLAG) {
            Metrics.counter(SVAR_AOU, TAG_RESULTAT, AVSLAG).increment();
        } else if (beslutning == SvarAnmodningUnntak.Beslutning.DELVIS_INNVILGELSE) {
            Metrics.counter(SVAR_AOU, TAG_RESULTAT, DELVIS_INNVILGELSE).increment();
        } else if (beslutning == SvarAnmodningUnntak.Beslutning.INNVILGELSE) {
            Metrics.counter(
                SVAR_AOU,
                TAG_RESULTAT,
                vedtakFattesAutomatisk ? INNVILGELSE_AUTO_VEDTAK : INNVILGELSE_MANUELL_BEHANDLING
            ).increment();
        }
    }
}
