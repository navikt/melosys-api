package no.nav.melosys.service.dokument.sed.mapper;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_987_2009;

public class LovvalgTilEuxMapper {

    public static String mapMelosysLovvalgTilEux(LovvalgBestemmelse lovvalgBestemmelse) {
        if (lovvalgBestemmelse instanceof LovvalgsBestemmelser_883_2004) {
            return (String) mapLovvalgBestemmelse_883_2004TilEux((LovvalgsBestemmelser_883_2004) lovvalgBestemmelse);
        } else if (lovvalgBestemmelse instanceof LovvalgsBestemmelser_987_2009) {
            return (String) mapLovvalgBestemmelse_987_2009TilEux((LovvalgsBestemmelser_987_2009) lovvalgBestemmelse);
        }

        throw new RuntimeException("Det er ikke implementert støtte for mapping til eux-kode for klasse: " + lovvalgBestemmelse.getClass());
    }

    private static String mapLovvalgBestemmelse_987_2009TilEux(LovvalgsBestemmelser_987_2009 lovvalgBestemmelse) {
        switch (lovvalgBestemmelse) {
            case FO_987_2009_ART14_11:
                return "14_11";
        }

        throw new RuntimeException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }

    private static String mapLovvalgBestemmelse_883_2004TilEux(LovvalgsBestemmelser_883_2004 lovvalgBestemmelse) {
        switch (lovvalgBestemmelse) {
            case FO_883_2004_ART11_1:
                return "11_1";
            case FO_883_2004_ART11_3A:
                return "11_3_a";
            case FO_883_2004_ART11_3B:
                return "11_3_b";
            case FO_883_2004_ART11_3C:
                return "11_3_c";
            //FIXME : endring i kodeverk template fil
            /*case FO_883_2004_ART11_3D:
                return "11_3_d";*/
            case FO_883_2004_ART11_3E:
                return "11_3_e";
            case FO_883_2004_ART11_4_2:
                return "11_4_2";
            case FO_883_2004_ART12_1:
                return "12_1";
            case FO_883_2004_ART12_2:
                return "12_2";
            case FO_883_2004_ART13_1A:
                return "13_1_a";
            case FO_883_2004_ART13_1B1:
                return "13_1_b_i";
         /*   case FO_883_2004_ART13_1B2:
                return "13_1_b_ii";
            case FO_883_2004_ART13_1B3:
                return "13_1_b_iii";
            case FO_883_2004_ART13_1B4:
                return "13_1_b_iv";*/
            case FO_883_2004_ART13_2A:
                return "13_2_a";
            case FO_883_2004_ART13_2B:
                return "13_2_b";
            case FO_883_2004_ART13_3:
                return "13_3";
            case FO_883_2004_ART13_4:
                return "13_4";
            case FO_883_2004_ART16_1:
                return "16_1";
            case FO_883_2004_ART16_2:
                return "16_2";
            case FO_883_2004_ANNET:
                return "annet";
        }

        throw new RuntimeException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }
}
