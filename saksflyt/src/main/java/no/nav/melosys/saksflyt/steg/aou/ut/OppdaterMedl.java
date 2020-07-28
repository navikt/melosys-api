package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_OPPDATER_MEDL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_SEND_BREV;


/**
 * Oppdaterer medlemskap periode i MEDL.
 *
 * Transisjoner:
 * ProsessType.ANMODNING_OM_UNNTAK
 *  AOU_OPPDATER_MEDL -> AOU_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakOppdaterMedl")
public class OppdaterMedl implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public OppdaterMedl(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_OPPDATER_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Anmodningsperiode anmodningsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentValidertAnmodningsperiode();
        medlPeriodeService.opprettPeriodeUnderAvklaring(anmodningsperiode, behandlingID, false);

        prosessinstans.setSteg(AOU_SEND_BREV);
    }
}
