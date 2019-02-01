package no.nav.melosys.saksflyt.agent.iv.validering;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

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
    public static boolean sedSkalSendes(Behandlingsresultattyper behandlingsresultatType, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultatType == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.getInnvilgelsesresultat().equals(InnvilgelsesResultat.INNVILGET)
            && lovvalgsperiode.getLovvalgsland() == Landkoder.NO
            && erGyldigBestemmelse(lovvalgsperiode.getBestemmelse());
    }

    public static boolean erGyldigBestemmelse(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1
            || bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2;
    }
}
