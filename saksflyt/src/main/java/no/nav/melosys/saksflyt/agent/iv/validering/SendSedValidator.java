package no.nav.melosys.saksflyt.agent.iv.validering;

import no.nav.melosys.domain.BehandlingsresultatType;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;

public enum SendSedValidator {
    INSTANCE;

    /**
     * Finn ut om SED skal sendes.
     * <p>
     * SED skal sendes dersom behandlingen har resultert i:
     * <ul>
     * <li>Lovvalgsland er avklart</li>
     * <li>Innvilget lovvalgsland er Norge</li>
     * <li>Lovvalgbestemmelsen er 12.1 eller 12.2</li>
     */
    public static boolean sedSkalSendes(BehandlingsresultatType behandlingsresultatType, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultatType == BehandlingsresultatType.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.getInnvilgelsesresultat().equals(InnvilgelsesResultat.INNVILGET)
            && lovvalgsperiode.getLovvalgsland() == Landkoder.NO
            && erGyldigBestemmelse(lovvalgsperiode.getBestemmelse());
    }

    public static boolean erGyldigBestemmelse(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1
            || bestemmelse == LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2;
    }
}
