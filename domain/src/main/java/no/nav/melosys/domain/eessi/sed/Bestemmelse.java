package no.nav.melosys.domain.eessi.sed;


import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*;
import org.springframework.util.Assert;

public enum Bestemmelse {

    ART_11_1("11_1"),
    ART_11_2("11_2"),
    ART_11_3_a("11_3_a"),
    ART_11_3_b("11_3_b"),
    ART_11_3_c("11_3_c"),
    ART_11_3_d("11_3_d"),
    ART_11_3_e("11_3_e"),
    ART_11_4("11_4"),
    ART_11_5("11_5"),
    ART_12_1("12_1"),
    ART_12_2("12_2"),
    ART_13_1_a("13_1_a"),
    ART_13_1_b_1("13_1_b_i"),
    ART_13_1_b_2("13_1_b_ii"),
    ART_13_1_b_3("13_1_b_iii"),
    ART_13_1_b_4("13_1_b_iv"),
    ART_13_2_a("13_2_a"),
    ART_13_2_b("13_2_b"),
    ART_13_3("13_3"),
    ART_13_4("13_4"),
    ART_14_2_a("14_2_a"),
    ART_14_2_b("14_2_b"),
    ART_14_a_2("14_a_2"),
    ART_14_c_a("14_c_a"),
    ART_14_c_b("14_c_b"),
    ART_14_11("14_11"),
    ART_15("15"),
    ART_16_1("16_1"),
    ART_16_2("16_2"),
    ART_87_8("87_8"),
    ART_87_a("87_a"),
    ANNET("annet");

    private static final BiMap<LovvalgBestemmelse, Bestemmelse> BESTEMMELSE_MAP =
        HashBiMap.create(ImmutableMap.<LovvalgBestemmelse, Bestemmelse>builder()
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1, Bestemmelse.ART_11_1)
            .put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2, Bestemmelse.ART_11_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, Bestemmelse.ART_11_3_a)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B, Bestemmelse.ART_11_3_b)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C, Bestemmelse.ART_11_3_c)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D, Bestemmelse.ART_11_3_d)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E, Bestemmelse.ART_11_3_e)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4, Bestemmelse.ART_11_4)
            .put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5, Bestemmelse.ART_11_5)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Bestemmelse.ART_12_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2, Bestemmelse.ART_12_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Bestemmelse.ART_13_1_a)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, Bestemmelse.ART_13_1_b_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2, Bestemmelse.ART_13_1_b_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3, Bestemmelse.ART_13_1_b_3)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4, Bestemmelse.ART_13_1_b_4)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, Bestemmelse.ART_13_2_a)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B, Bestemmelse.ART_13_2_b)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, Bestemmelse.ART_13_3)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4, Bestemmelse.ART_13_4)
            .put(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11, Bestemmelse.ART_14_11)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART15, Bestemmelse.ART_15)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, Bestemmelse.ART_16_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Bestemmelse.ART_16_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET, Bestemmelse.ANNET)
            .put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A, Bestemmelse.ART_87_a)
            .put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8, Bestemmelse.ART_87_8)
            .put(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A, Bestemmelse.ART_14_2_a)
            .put(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B, Bestemmelse.ART_14_2_b)
            .put(Overgangsregelbestemmelser.FO_1408_1971_ART14A_2, Bestemmelse.ART_14_a_2)
            .put(Overgangsregelbestemmelser.FO_1408_1971_ART14C_A, Bestemmelse.ART_14_c_a)
            .put(Overgangsregelbestemmelser.FO_1408_1971_ART14C_B, Bestemmelse.ART_14_c_b)
            .build());

    private static final Map<LovvalgBestemmelse, Bestemmelse> GB_KONV_BESTEMMELSE_MAP =
        Maps.newHashMap(ImmutableMap.<LovvalgBestemmelse, Bestemmelse>builder()
            .put(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_2, Bestemmelse.ART_11_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A, Bestemmelse.ART_11_3_a)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3B, Bestemmelse.ART_11_3_b)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3C, Bestemmelse.ART_11_3_d)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D, Bestemmelse.ART_11_3_e)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4, Bestemmelse.ART_11_4)
            .put(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5, Bestemmelse.ART_11_5)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1, Bestemmelse.ART_12_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1, Bestemmelse.ART_12_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2, Bestemmelse.ART_12_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3, Bestemmelse.ART_12_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1A, Bestemmelse.ART_13_1_a)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1B1, Bestemmelse.ART_13_1_b_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B2, Bestemmelse.ART_13_1_b_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B3, Bestemmelse.ART_13_1_b_3)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B4, Bestemmelse.ART_13_1_b_4)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2A, Bestemmelse.ART_13_2_a)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2B, Bestemmelse.ART_13_2_b)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_3, Bestemmelse.ART_13_3)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_4, Bestemmelse.ART_13_4)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1, Bestemmelse.ART_16_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_2, Bestemmelse.ART_16_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_5, Bestemmelse.ANNET)
            .build());

    private String value;

    Bestemmelse(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public LovvalgBestemmelse tilMelosysBestemmelse() {
        if (BESTEMMELSE_MAP.inverse().containsKey(this)) {
            return BESTEMMELSE_MAP.inverse().get(this);
        }

        throw new IllegalArgumentException("Støtte for kode: " + this + " er ikke implementert");
    }

    public static Bestemmelse fraMelosysBestemmelse(LovvalgBestemmelse lovvalgBestemmelse) {
        Assert.notNull(lovvalgBestemmelse, "LovvalgBestemmelse er null.");

        if (lovvalgBestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            || lovvalgBestemmelse == Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2
            || lovvalgBestemmelse == Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1
            || lovvalgBestemmelse == Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_2) {
            return Bestemmelse.ART_11_4;
        }

        if (BESTEMMELSE_MAP.containsKey(lovvalgBestemmelse)) {
            return BESTEMMELSE_MAP.get(lovvalgBestemmelse);
        }

        if (GB_KONV_BESTEMMELSE_MAP.containsKey(lovvalgBestemmelse)) {
            return GB_KONV_BESTEMMELSE_MAP.get(lovvalgBestemmelse);
        }

        throw new IllegalArgumentException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }

    public static Bestemmelse fraBestemmelseString(String bestemmelse) {
        return Arrays.stream(Bestemmelse.values()).filter(b -> b.getValue().equals(bestemmelse))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Enum Bestemmelse finnes ikke av verdi " + bestemmelse));
    }
}
