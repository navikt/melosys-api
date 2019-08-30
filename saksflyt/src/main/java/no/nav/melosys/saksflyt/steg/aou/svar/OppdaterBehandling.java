package no.nav.melosys.saksflyt.steg.aou.svar;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vedtak.VedtakService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OppdaterBehandling extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandling.class);

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final VedtakService vedtakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    @Autowired
    public OppdaterBehandling(AnmodningsperiodeService anmodningsperiodeService,
                              BehandlingService behandlingService,
                              BehandlingsresultatService behandlingsresultatService,
                              @Qualifier("system") VedtakService vedtakService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.vedtakService = vedtakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_SVAR_OPPDATER_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
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
                Anmodningsperiodesvartyper.INNVILGELSE  , behandlingID);
            fattVedtak(behandlingID);
        } else {
            log.info("Mottatt svar {} på anmodning om unntak for behandling {}. Endrer behandlingsstatus til {}",
                anmodningsperiode.getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType(),
                behandlingID,
                Behandlingsstatus.VURDER_DOKUMENT);
            oppdaterBehandlingsstatusVurderDokument(prosessinstans);
        }
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }

    private void fattVedtak(long behandlingID) throws FunksjonellException, TekniskException {
        behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.DELVIS_AUTOMATISERT);
        vedtakService.fattVedtak(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
    }

    private void oppdaterBehandlingsstatusVurderDokument(Prosessinstans prosessinstans) throws FunksjonellException {
        behandlingService.oppdaterStatus(prosessinstans.getBehandling().getId(), Behandlingsstatus.VURDER_DOKUMENT);
    }

    private boolean inneholderYtterligereInformasjon(MelosysEessiMelding melosysEessiMelding) {
        return StringUtils.isNotEmpty(melosysEessiMelding.getYtterligereInformasjon());
    }
}
