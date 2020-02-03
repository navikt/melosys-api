package no.nav.melosys.saksflyt.steg.iv;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_FORKORT_PERIODE;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_VALIDERING;

/**
 * Legger til avklarte fakta med informasjon om endringsbegrunnelse.
 *
 * Transisjoner:
 * IV_FORKORT_PERIODE -> IV_VALIDERING eller FEILET_MASKINELT hvis feil
 */
@Component
public class ForkortPeriode extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ForkortPeriode.class);

    private final AvklartefaktaService avklartefakteService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EessiService eessiService;

    @Autowired
    public ForkortPeriode(AvklartefaktaService avklartefaktaService, BehandlingsresultatService behandlingsresultatService, @Qualifier("system") EessiService eessiService) {
        this.avklartefakteService = avklartefaktaService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiService = eessiService;
        log.info("ForkortPeriode initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_FORKORT_PERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        Endretperiode endretperiode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class);

        avklartefakteService.leggTilBegrunnelse(behandling.getId(), Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiode.getKode());

        BucType bucType = LovvalgBestemmelseUtils.hentBucTypeFraBestemmelse(
            behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).hentValidertLovvalgsperiode().getBestemmelse());
        String mottakerinstitusjonFraTidlBuc = eessiService.hentMottakerinstitusjonFraBuc(prosessinstans.getBehandling().getFagsak(), bucType);
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of(mottakerinstitusjonFraTidlBuc));

        prosessinstans.setSteg(IV_VALIDERING);
        log.info("Oppdatert avklarteFakta for prosessinstans {}.", prosessinstans.getId());
    }
}
