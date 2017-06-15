package no.nav.melosys.saksflyt.impl.steg.a1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.Status;
import no.nav.melosys.saksflyt.api.Steg;
import no.nav.melosys.saksflyt.impl.domain.SakImpl;
import no.nav.melosys.saksflyt.impl.domain.SakUtils;
import no.nav.melosys.service.OpprettSakService;

/**
 * Steget sørger for å * hente opp og verifisere personinfo * Koble til ny eller eksisterende sak * Ferdigstille journalføring
 * 
 * Transisjoner: NY → KLARGJORT: Hvis det lar seg gjøre å klargjøre maskinelt NY → FEILET: Hvis ingen treff i TPS etc.
 * 
 * Status blir uforandret dersom TPS ikke svarer
 * 
 */
@Component
public class KlargjoereSteg implements Steg {

    private static final Logger log = LoggerFactory.getLogger(KlargjoereSteg.class);

    @Autowired
    private OpprettSakService opprettSakService;

    @Autowired
    private Binge binge;

    @Override
    public void finnSakOgutfoerSteg() {
        SakImpl sak = (SakImpl) binge.fjernFørsteSak(SakUtils.sakMedStatus(Status.SOEKA1_V1_NY), SakUtils.kortestFristFørst());
        if (sak == null) {
            // Ingenting å gjøre
            return;
        }

        // Hent personopplysninger fra TPS
        // Sett personopplysninger på sak
        // Endre status på sak
        String fnr = "FJERNET"; // FIXME: EESSI2-43
        try {
            opprettSakService.opprettSak(fnr);
        } catch (Throwable t) {
            log.error("Feil ", t.getCause()); // TODO Exceptions i Melosys
        }

        sak.setStatus(Status.SOEKA1_V1_KLARGJORT);
        binge.leggTilSak(sak);
    }

}
