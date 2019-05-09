package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.TilleggsBestemmelser_883_2004;

public final class LovvalgBestemmelseUtil {

    private LovvalgBestemmelseUtil() {
        throw new IllegalStateException("Utility");
    }

    public static LovvalgBestemmelse dbDataTilLovvalgBestemmelse(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return LovvalgsBestemmelser_883_2004.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
        }
        try {
            return LovvalgsBestemmelser_987_2009.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
        }
        return TilleggsBestemmelser_883_2004.valueOf(dbData);
    }
}
