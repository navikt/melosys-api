package no.nav.melosys.domain.util;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LovvalgBestemmelseUtils {

    private LovvalgBestemmelseUtils() {
        throw new IllegalStateException("Utility");
    }

    private static final Map<String, LovvalgBestemmelse> kodeTilLovvalgBestemmelse = new HashMap<>();

    static {
        for (var lovBestemelser : List.of(
            Lovvalgbestemmelser_883_2004.values(),
            Lovvalgbestemmelser_987_2009.values(),
            Tilleggsbestemmelser_883_2004.values(),
            Lovvalgbestemmelser_trygdeavtale_uk.values()
        )) {
            for (var lovBestemmelse : lovBestemelser) {
                kodeTilLovvalgBestemmelse.put(lovBestemmelse.getKode(), lovBestemmelse);
            }
        }
    }

    public static LovvalgBestemmelse dbDataTilLovvalgBestemmelse(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Optional.ofNullable(kodeTilLovvalgBestemmelse.get(dbData))
            .orElseThrow(() -> new IllegalArgumentException("Lovvalgbestemmelse kode:" + dbData + " ikke funnet"));
    }
}
