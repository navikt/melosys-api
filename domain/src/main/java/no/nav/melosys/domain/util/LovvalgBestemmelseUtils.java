package no.nav.melosys.domain.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*;

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
            Lovvalgbestemmelser_trygdeavtale_uk.values(),
            Lovvalgbestemmelser_trygdeavtale_usa.values(),
            Lovvalgbestemmelser_trygdeavtale_ca.values(),
            Tilleggsbestemmelser_trygdeavtale_ca.values(),
            Lovvalgbestemmelser_trygdeavtale_au.values()
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
