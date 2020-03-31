package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktAvklarMyndighet;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_AVKLAR_ARBEIDSGIVER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_AVKLAR_MYNDIGHET;

/**
 * Avklarer hvilken utenlandsk myndighet er part i saken.
 *
 * Transisjoner:
 *  IV_AVKLAR_MYNDIGHET -> IV_AVKLAR_ARBEIDSGIVER eller FEILET_MASKINELT hvis feil
 */
@Component("IverksettVedtakAvklarMyndighet")
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
        return IV_AVKLAR_MYNDIGHET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        super.utfør(prosessinstans);
        prosessinstans.setSteg(IV_AVKLAR_ARBEIDSGIVER);
    }
}