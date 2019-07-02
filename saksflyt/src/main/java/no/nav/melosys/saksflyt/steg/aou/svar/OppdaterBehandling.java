package no.nav.melosys.saksflyt.steg.aou.svar;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppdaterBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandling.class);

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;

    @Autowired
    public OppdaterBehandling(AnmodningsperiodeService anmodningsperiodeService, BehandlingService behandlingService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_SVAR_OPPDATER_BEHANDLING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.info("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Anmodningsperiode anmodningsperiode = anmodningsperiodeService.hentAnmodningsperioder(prosessinstans.getBehandling().getId())
            .stream().findFirst().orElseThrow(() -> new TekniskException("Finner ingen anmodningsperiode for behandling " + prosessinstans.getBehandling().getId()));
        boolean erInnvilgelse = anmodningsperiode.getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType() == AnmodningsperiodeSvarType.INNVILGELSE;
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (erInnvilgelse && !inneholderYtterligereInformasjon(melosysEessiMelding)) {
            settProsessinstansTilIverksettVedtak(prosessinstans);
        } else {
            oppdaterBehandlingsstatusVurderDokument(prosessinstans);
        }
    }

    private void oppdaterBehandlingsstatusVurderDokument(Prosessinstans prosessinstans) throws FunksjonellException {
        behandlingService.oppdaterStatus(prosessinstans.getBehandling().getId(), Behandlingsstatus.VURDER_DOKUMENT);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }

    private void settProsessinstansTilIverksettVedtak(Prosessinstans prosessinstans) throws FunksjonellException {
        behandlingService.oppdaterStatus(prosessinstans.getBehandling().getId(), Behandlingsstatus.IVERKSETTER_VEDTAK);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);

        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
    }

    private boolean inneholderYtterligereInformasjon(MelosysEessiMelding melosysEessiMelding) {
        return StringUtils.isNotEmpty(melosysEessiMelding.getYtterligereInformasjon());
    }
}
