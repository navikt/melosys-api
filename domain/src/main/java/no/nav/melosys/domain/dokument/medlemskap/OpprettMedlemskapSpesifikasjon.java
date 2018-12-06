package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatType;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;

public class OpprettMedlemskapSpesifikasjon {

    public static boolean erPeriodeEndelig(Behandlingsresultat behandlingsresultat, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultat.getType() == BehandlingsresultatType.FASTSATT_LOVVALGSLAND && lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET;
    }

    public static boolean erPeriodeUnderAvklaring(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getType() == BehandlingsresultatType.ANMODNING_OM_UNNTAK;
    }
}
