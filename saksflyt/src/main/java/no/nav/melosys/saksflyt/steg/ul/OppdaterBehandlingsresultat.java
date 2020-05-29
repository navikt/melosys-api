package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("ULOppdaterBehandlingsresultat")
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

    private final UtpekingService utpekingService;
    private final BehandlingsresultatService behandlingsresultatService;

    public OppdaterBehandlingsresultat(UtpekingService utpekingService, BehandlingsresultatService behandlingsresultatService) {
        this.utpekingService = utpekingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.UL_OPPDATER_BEHANDLINGSRESULTAT;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final long behandlingID = prosessinstans.getBehandling().getId();

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        utpekingService.oppdaterSendtUtland(behandlingsresultat.hentValidertUtpekingsperiode());

        behandlingsresultat.setType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);
        behandlingsresultat.setBegrunnelseFritekst(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST));
        behandlingsresultatService.lagre(behandlingsresultat);
        log.info("Behandlingsresultat {} oppdatert til type {}, og utpekingsperiode markert som sendt til utland",
            behandlingID, behandlingsresultat.getType());

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
