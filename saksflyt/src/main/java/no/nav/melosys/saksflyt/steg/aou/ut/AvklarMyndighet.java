package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktAvklarMyndighet;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_OPPDATER_MEDL;

/**
 * Avklarer hvilken utenlandsk myndighet er part i saken.
 *
 * Transisjoner:
 *  AOU_AVKLAR_MYNDIGHET -> AOU_OPPDATER_MEDL eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakAvklarMyndighet")
public class AvklarMyndighet extends AbstraktAvklarMyndighet {
    private static final Logger log = LoggerFactory.getLogger(AvklarMyndighet.class);

    @Autowired
    public AvklarMyndighet(BehandlingService behandlingService,
                           BehandlingsresultatService behandlingsresultatService,
                           UtenlandskMyndighetService utenlandskMyndighetService) {
        super(behandlingService, behandlingsresultatService,
            utenlandskMyndighetService);
        log.info("AvklarMyndighet initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_AVKLAR_MYNDIGHET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        super.utfør(prosessinstans);
        prosessinstans.setSteg(AOU_OPPDATER_MEDL);
    }
}