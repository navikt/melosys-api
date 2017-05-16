package no.nav.melosys.saksflyt.impl.soeka1_v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.Status;
import no.nav.melosys.saksflyt.api.Steg;
import no.nav.melosys.saksflyt.impl.domain.SakImpl;
import no.nav.melosys.saksflyt.impl.domain.SakUtils;

/**
 * Steget sørger for å
 *   * hente opp og verifisere personinfo
 *   * Koble til ny eller eksisterende sak
 *   * Ferdigstille journalføring
 * 
 * Transisjoner:
 * NY → KLARGJORT: Hvis det lar seg gjøre å klargjøre maskinelt
 * NY → FEILET: Hvis ingen treff i TPS etc.
 * 
 * Status blir uforandret dersom TPS ikke svarer
 * 
 */
@Component
public class KlargjoereSteg implements Steg {

    @Autowired
    private Binge binge;
    
    @Override
    public void finnSakOgutfoerSteg() {
        SakImpl sak = (SakImpl) binge.fjernFørsteSak(SakUtils.sakMedStatus(Status.SOEKA1_V1_NY), SakUtils.kortestFristFørst());
        if (sak == null) {
            // Ingenting å gjøre
            return;
        }
        // FIXME: EESSI2-43
        // Hent personopplysninger fra TPS
        // Sett personopplysninger på sak
        // Endre status på sak
        sak.setStatus(Status.SOEKA1_V1_KLARGJORT);
        binge.leggTilSak(sak);
    }

}
