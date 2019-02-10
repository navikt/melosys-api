package no.nav.melosys.service.dokument.sed.mapper;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_987_2009;
import no.nav.melosys.integrasjon.eessi.dto.Bestemmelse;

public class LovvalgTilBestemmelseDtoMapper {


    public static Bestemmelse mapMelosysLovvalgTilBestemmelseDto(LovvalgBestemmelse lovvalgBestemmelse) {
        if (lovvalgBestemmelse instanceof LovvalgBestemmelse_883_2004) {
            return mapLovvalgBestemmelse_883_2004TilBestemmelseDto((LovvalgBestemmelse_883_2004) lovvalgBestemmelse);
        } else if (lovvalgBestemmelse instanceof LovvalgBestemmelse_987_2009) {
            return mapLovvalgBestemmelse_987_2009TilBestemmelseDto((LovvalgBestemmelse_987_2009) lovvalgBestemmelse);
        }

        throw new RuntimeException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }

    private static Bestemmelse mapLovvalgBestemmelse_987_2009TilBestemmelseDto(LovvalgBestemmelse_987_2009 lovvalgBestemmelse) {
        switch (lovvalgBestemmelse) {
            case FO_987_2009_ART14_11:
                return Bestemmelse.ART_14_11;
        }

        throw new RuntimeException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }

    private static Bestemmelse mapLovvalgBestemmelse_883_2004TilBestemmelseDto(LovvalgBestemmelse_883_2004 lovvalgBestemmelse) {
        switch (lovvalgBestemmelse) {
            case FO_883_2004_ART11_1:
                return Bestemmelse.ART_11_1;
            case FO_883_2004_ART11_3A:
                return Bestemmelse.ART_11_3_a;
            case FO_883_2004_ART11_3B:
                return Bestemmelse.ART_11_3_b;
            case FO_883_2004_ART11_3C:
                return Bestemmelse.ART_11_3_c;
            case FO_883_2004_ART11_3D:
                return Bestemmelse.ART_11_3_d;
            case FO_883_2004_ART11_3E:
                return Bestemmelse.ART_11_3_e;
            case FO_883_2004_ART11_4_2:
                return Bestemmelse.ART_11_4_2;
            case FO_883_2004_ART12_1:
                return Bestemmelse.ART_12_1;
            case FO_883_2004_ART12_2:
                return Bestemmelse.ART_12_2;
            case FO_883_2004_ART13_1A:
                return Bestemmelse.ART_13_1_a;
            case FO_883_2004_ART13_1B1:
                return Bestemmelse.ART_13_1_b_1;
            case FO_883_2004_ART13_1B2:
                return Bestemmelse.ART_13_1_b_2;
            case FO_883_2004_ART13_1B3:
                return Bestemmelse.ART_13_1_b_3;
            case FO_883_2004_ART13_1B4:
                return Bestemmelse.ART_13_1_b_4;
            case FO_883_2004_ART13_2A:
                return Bestemmelse.ART_13_2_a;
            case FO_883_2004_ART13_2B:
                return Bestemmelse.ART_13_2_b;
            case FO_883_2004_ART13_3:
                return Bestemmelse.ART_13_3;
            case FO_883_2004_ART13_4:
                return Bestemmelse.ART_13_4;
            case FO_883_2004_ART16_1:
                return Bestemmelse.ART_16_1;
            case FO_883_2004_ART16_2:
                return Bestemmelse.ART_16_2;
        }

        throw new RuntimeException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }
}
