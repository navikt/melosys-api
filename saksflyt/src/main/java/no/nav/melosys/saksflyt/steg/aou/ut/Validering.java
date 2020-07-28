package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_VALIDERING;

/**
 * Validerer opplysning bli brukt for anmodning om unntak.
 *
 * Transisjoner:
 *
 * ProsessType.ANMODNING_OM_UNNTAK
 *  AOU_VALIDERING -> AOU_OPPDATER_RESULTAT eller FEILET_MASKINELT hvis feil
 */
@Component("AnmodningOmUnntakValidering")
public class Validering implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(Validering.class);

    @Autowired
    public Validering() {
        log.info("AnmodningOmUnntakValidering initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AOU_VALIDERING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.ANMODNING_OM_UNNTAK) {
            throw new TekniskException("ProsessType " + prosessType + " er ikke støttet");
        }

        String saksbehandlerID = prosessinstans.getData(SAKSBEHANDLER);
        if (saksbehandlerID == null) {
            throw new TekniskException("SaksbehandlerID er ikke oppgitt.", null);
        }

        prosessinstans.setSteg(AOU_OPPDATER_RESULTAT);
    }
}
