package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.ProsessSteg.OPPDATER_AVKLARTE_FAKTA_ENDRETPERIODE_BEGRUNNELSE;

/**
 * Legger til avklarte fakta med informasjon om endringsbegrunnelse.
 *
 * Transisjoner:
 * OPPDATER_AVKLARTE_FAKTA_ENDRETPERIODE_BEGRUNNELSE -> IV_OPPDATER_RESULTAT eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterAvklarteFaktaEndretPeriodeBegrunnelse extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterAvklarteFaktaEndretPeriodeBegrunnelse.class);

    private AvklartefaktaService avklartefakteService;

    @Autowired
    public OppdaterAvklarteFaktaEndretPeriodeBegrunnelse(AvklartefaktaService avklartefaktaService) {
        this.avklartefakteService = avklartefaktaService;
        log.info("OppdaterAvlklarteFakta initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPDATER_AVKLARTE_FAKTA_ENDRETPERIODE_BEGRUNNELSE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        Endretperioder endretperiode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperioder.class);

        avklartefakteService.leggTilEndretPeriodeAvklarteFakta(behandling.getId(), endretperiode);

        prosessinstans.setSteg(IV_OPPDATER_RESULTAT);
        log.info("Oppdatert avklarteFakta for prosessinstans {}.", prosessinstans.getId());
    }
}
