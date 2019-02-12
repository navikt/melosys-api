package no.nav.melosys.service.dokument.sed.mapper;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_987_2009;
import no.nav.melosys.integrasjon.eessi.dto.Bestemmelse;

public class LovvalgTilBestemmelseDtoMapper {

    private static final Map<LovvalgBestemmelse, Bestemmelse> mapper = new HashMap<>();

    static {
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_1, Bestemmelse.ART_11_1);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3A, Bestemmelse.ART_11_3_a);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3B, Bestemmelse.ART_11_3_b);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3C, Bestemmelse.ART_11_3_c);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3D, Bestemmelse.ART_11_3_d);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3E, Bestemmelse.ART_11_3_e);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1, Bestemmelse.ART_12_1);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2, Bestemmelse.ART_12_2);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1A, Bestemmelse.ART_13_1_a);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B1, Bestemmelse.ART_13_1_b_1);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B2, Bestemmelse.ART_13_1_b_2);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B3, Bestemmelse.ART_13_1_b_3);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B4, Bestemmelse.ART_13_1_b_4);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_2A, Bestemmelse.ART_13_2_a);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_2B, Bestemmelse.ART_13_2_b);
        mapper.put(LovvalgBestemmelse_987_2009.FO_987_2009_ART14_11, Bestemmelse.ART_14_11);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART16_1, Bestemmelse.ART_16_1);
        mapper.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART16_2, Bestemmelse.ART_16_2);
    }

    public static Bestemmelse mapMelosysLovvalgTilBestemmelseDto(LovvalgBestemmelse lovvalgBestemmelse) {

        if (lovvalgBestemmelse != null && mapper.containsKey(lovvalgBestemmelse)) {
            return mapper.get(lovvalgBestemmelse);
        }

        throw new RuntimeException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }
}
