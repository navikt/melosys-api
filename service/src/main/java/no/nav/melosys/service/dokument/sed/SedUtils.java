package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.eux.BucType;
import no.nav.melosys.eux.model.SedType;

public class SedUtils {

    public static SedType hentSedTypeFraLovvalgsBestemmelse(LovvalgBestemmelse bestemmelse) {

        if (bestemmelse instanceof LovvalgsBestemmelser_883_2004) {
            LovvalgsBestemmelser_883_2004 b = (LovvalgsBestemmelser_883_2004) bestemmelse;
            switch (b) {
                case FO_883_2004_ART12_1:
                case FO_883_2004_ART12_2:
                    return SedType.A009;
            }
        }

        throw new RuntimeException("Lovvalgsbestemmelse " + bestemmelse.name() + " er ikke støttet enda!");
    }

    public static BucType hentBucFraLovvalgsBestemmelse(LovvalgBestemmelse bestemmelse) {
        if (bestemmelse instanceof LovvalgsBestemmelser_883_2004) {
            LovvalgsBestemmelser_883_2004 b = (LovvalgsBestemmelser_883_2004) bestemmelse;
            switch (b) {
                case FO_883_2004_ART12_1:
                case FO_883_2004_ART12_2:
                    return BucType.LA_BUC_04;
            }
        }
        throw new RuntimeException("Lovvalgsbestemmelse " + bestemmelse.name() + " er ikke støttet enda!");
    }
}
