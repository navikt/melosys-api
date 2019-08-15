package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.IV_FORKORT_PERIODE;
import static no.nav.melosys.domain.ProsessSteg.IV_VALIDERING;

/**
 * Legger til avklarte fakta med informasjon om endringsbegrunnelse.
 *
 * Transisjoner:
 * IV_FORKORT_PERIODE -> IV_VALIDERING eller FEILET_MASKINELT hvis feil
 */
@Component
public class ForkortPeriode extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ForkortPeriode.class);

    private AvklartefaktaService avklartefakteService;

    @Autowired
    public ForkortPeriode(AvklartefaktaService avklartefaktaService) {
        this.avklartefakteService = avklartefaktaService;
        log.info("ForkortPeriode initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_FORKORT_PERIODE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        Endretperioder endretperiode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperioder.class);

        avklartefakteService.leggTilBegrunnelse(behandling.getId(), Avklartefaktatype.AARSAK_ENDRING_PERIODE, endretperiode.getKode());

        prosessinstans.setSteg(IV_VALIDERING);
        log.info("Oppdatert avklarteFakta for prosessinstans {}.", prosessinstans.getId());
    }
}
