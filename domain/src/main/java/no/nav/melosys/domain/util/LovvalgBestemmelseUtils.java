package no.nav.melosys.domain.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.*;

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
            Lovvalgbestemmelser_konv_efta_storbritannia.values(),
            Tilleggsbestemmelser_konv_efta_storbritannia.values(),

            Lovvalgsbestemmelser_trygdeavtale_au.values(),
            Lovvalgsbestemmelser_trygdeavtale_ba.values(),
            Lovvalgsbestemmelser_trygdeavtale_ca.values(),
            Lovvalgsbestemmelser_trygdeavtale_ca_qc.values(),
            Lovvalgsbestemmelser_trygdeavtale_ch.values(),
            Lovvalgsbestemmelser_trygdeavtale_cl.values(),
            Lovvalgsbestemmelser_trygdeavtale_fr.values(),
            Lovvalgsbestemmelser_trygdeavtale_gb.values(),
            Lovvalgsbestemmelser_trygdeavtale_gr.values(),
            Lovvalgsbestemmelser_trygdeavtale_hr.values(),
            Lovvalgsbestemmelser_trygdeavtale_il.values(),
            Lovvalgsbestemmelser_trygdeavtale_in.values(),
            Lovvalgsbestemmelser_trygdeavtale_it.values(),
            Lovvalgsbestemmelser_trygdeavtale_me.values(),
            Lovvalgsbestemmelser_trygdeavtale_pt.values(),
            Lovvalgsbestemmelser_trygdeavtale_rs.values(),
            Lovvalgsbestemmelser_trygdeavtale_si.values(),
            Lovvalgsbestemmelser_trygdeavtale_tr.values(),
            Lovvalgsbestemmelser_trygdeavtale_us.values(),

            Tilleggsbestemmelser_trygdeavtale_cl.values(),
            Tilleggsbestemmelser_trygdeavtale_ca.values(),
            Overgangsregelbestemmelser.values()
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
