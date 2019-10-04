package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.integrasjon.eessi.dto.Bestemmelse;
import org.springframework.util.Assert;

public class LovvalgTilBestemmelseDtoMapper {

    private static final BiMap<LovvalgBestemmelse, Bestemmelse> mapper = HashBiMap.create();

    private LovvalgTilBestemmelseDtoMapper() {
        throw new IllegalArgumentException("Utility");
    }

    static {
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1, Bestemmelse.ART_11_1);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, Bestemmelse.ART_11_3_a);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B, Bestemmelse.ART_11_3_b);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C, Bestemmelse.ART_11_3_c);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E, Bestemmelse.ART_11_3_e);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2, Bestemmelse.ART_11_4_2);
        mapper.put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5, Bestemmelse.ART_11_5);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Bestemmelse.ART_12_1);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2, Bestemmelse.ART_12_2);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Bestemmelse.ART_13_1_a);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, Bestemmelse.ART_13_1_b_1);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, Bestemmelse.ART_13_2_a);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B, Bestemmelse.ART_13_2_b);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, Bestemmelse.ART_16_1);
        mapper.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Bestemmelse.ART_16_2);
    }

    public static Bestemmelse mapMelosysLovvalgTilBestemmelseDto(LovvalgBestemmelse lovvalgBestemmelse) {
        Assert.notNull(lovvalgBestemmelse, "LovvalgBestemmelse er null.");

        if (mapper.containsKey(lovvalgBestemmelse)) {
            return mapper.get(lovvalgBestemmelse);
        }

        throw new IllegalArgumentException("Støtte for kode: " + lovvalgBestemmelse.getKode() + " er ikke implementert");
    }

    public static LovvalgBestemmelse mapBestemmelseVerdiTilMelosysLovvalgBestemmelse(String bestemmelse) {
        Bestemmelse bestemmelseEnum = Arrays.stream(Bestemmelse.values()).filter(b -> b.getValue().equals(bestemmelse))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Enum Bestemmelse finnes ikke av verdi " + bestemmelse));
        return mapBestemmelseDtoTilMelosysLovvalgBestemmelse(bestemmelseEnum);
    }

    private static LovvalgBestemmelse mapBestemmelseDtoTilMelosysLovvalgBestemmelse(Bestemmelse bestemmelse) {
        Assert.notNull(bestemmelse, "LovvalgBestemmelse er null.");

        if (mapper.inverse().containsKey(bestemmelse)) {
            return mapper.inverse().get(bestemmelse);
        }

        throw new IllegalArgumentException("Støtte for kode: " + bestemmelse + " er ikke implementert");
    }
}
