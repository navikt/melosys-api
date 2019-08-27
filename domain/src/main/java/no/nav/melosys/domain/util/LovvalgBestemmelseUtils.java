package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;

public final class LovvalgBestemmelseUtils {

    private LovvalgBestemmelseUtils() {
        throw new IllegalStateException("Utility");
    }

    public static LovvalgBestemmelse dbDataTilLovvalgBestemmelse(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Lovvalgbestemmelser_883_2004.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
        }
        try {
            return Lovvalgbestemmelser_987_2009.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
        }
        return Tilleggsbestemmelser_883_2004.valueOf(dbData);
    }
}
