package no.nav.melosys.domain.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.dokument.felles.Land.*;
import static no.nav.melosys.domain.kodeverk.Landkoder.*;

public final class LandkoderUtils {
    private static final BiMap<Landkoder,String> LANDKODER_STRING_BI_MAP = EnumHashBiMap.create(Landkoder.class);

    static {
        LANDKODER_STRING_BI_MAP.put(NO, NORGE);
        LANDKODER_STRING_BI_MAP.put(AT, ØSTERRIKE);
        LANDKODER_STRING_BI_MAP.put(AX, ÅLAND);
        LANDKODER_STRING_BI_MAP.put(BE, BELGIA);
        LANDKODER_STRING_BI_MAP.put(BG, BULGARIA);
        LANDKODER_STRING_BI_MAP.put(CH, SVEITS);
        LANDKODER_STRING_BI_MAP.put(CY, KYPROS);
        LANDKODER_STRING_BI_MAP.put(CZ, TSJEKKIA);
        LANDKODER_STRING_BI_MAP.put(DE, TYSKLAND);
        LANDKODER_STRING_BI_MAP.put(DK, DANMARK);
        LANDKODER_STRING_BI_MAP.put(EE, ESTLAND);
        LANDKODER_STRING_BI_MAP.put(ES, SPANIA);
        LANDKODER_STRING_BI_MAP.put(FI, FINLAND);
        LANDKODER_STRING_BI_MAP.put(FO, FÆRØYENE);
        LANDKODER_STRING_BI_MAP.put(FR, FRANKRIKE);
        LANDKODER_STRING_BI_MAP.put(GB, STORBRITANNIA);
        LANDKODER_STRING_BI_MAP.put(GL, GRØNLAND);
        LANDKODER_STRING_BI_MAP.put(GR, HELLAS);
        LANDKODER_STRING_BI_MAP.put(HR, KROATIA);
        LANDKODER_STRING_BI_MAP.put(HU, UNGARN);
        LANDKODER_STRING_BI_MAP.put(IE, IRLAND);
        LANDKODER_STRING_BI_MAP.put(IS, ISLAND);
        LANDKODER_STRING_BI_MAP.put(IT, ITALIA);
        LANDKODER_STRING_BI_MAP.put(LI, LIECHTENSTEIN);
        LANDKODER_STRING_BI_MAP.put(LT, LITAUEN);
        LANDKODER_STRING_BI_MAP.put(LU, LUXEMBOURG);
        LANDKODER_STRING_BI_MAP.put(LV, LATVIA);
        LANDKODER_STRING_BI_MAP.put(MT, MALTA);
        LANDKODER_STRING_BI_MAP.put(NL, NEDERLAND);
        LANDKODER_STRING_BI_MAP.put(PL, POLEN);
        LANDKODER_STRING_BI_MAP.put(PT, PORTUGAL);
        LANDKODER_STRING_BI_MAP.put(RO, ROMANIA);
        LANDKODER_STRING_BI_MAP.put(SE, SVERIGE);
        LANDKODER_STRING_BI_MAP.put(SI, SLOVENIA);
        LANDKODER_STRING_BI_MAP.put(SJ, SVALBARD_OG_JAN_MAYEN);
        LANDKODER_STRING_BI_MAP.put(SK, SLOVAKIA);
    }



    private LandkoderUtils() {
        throw new IllegalArgumentException("Utility");
    }

    public static String tilIso3(String l) throws TekniskException {
        Landkoder iso2Kode = Landkoder.valueOf(l);
        String iso3Kode = LANDKODER_STRING_BI_MAP.get(iso2Kode);
        if (iso3Kode == null) {
            throw new TekniskException("Landkode " + iso2Kode.getKode() + " støttes ikke.");
        } else {
            return iso3Kode;
        }
    }

    public static Landkoder tilIso2(String iso3Kode) throws TekniskException {
        Landkoder iso2Kode = LANDKODER_STRING_BI_MAP.inverse().get(iso3Kode);

        if (iso2Kode == null) {
            throw new TekniskException("Landkode " + iso3Kode + " støttes ikke.");
        } else {
            return iso2Kode;
        }
    }
}
