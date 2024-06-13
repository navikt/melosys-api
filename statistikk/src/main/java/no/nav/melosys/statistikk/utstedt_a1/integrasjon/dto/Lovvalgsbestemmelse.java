package no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.dokument.brev.mapper.felles.KonvEftaStorbritanniaLovvalgbestemmelser;

public enum Lovvalgsbestemmelse {
    ART_11_3_a("11.3a"),
    ART_11_3_b("11.3b"),
    ART_11_4("11.4"),
    ART_12_1("12.1"),
    ART_12_2("12.2"),
    ART_13_1("13.1"),
    ART_13_2("13.2"),
    ART_13_3("13.3"),
    ART_13_4("13.4"),
    ART_16("16");

    private static final Map<LovvalgBestemmelse, Lovvalgsbestemmelse> LOVVALGSBESTEMMELSE_MAP =
        Maps.newHashMap(ImmutableMap.<LovvalgBestemmelse, Lovvalgsbestemmelse>builder()
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, Lovvalgsbestemmelse.ART_11_3_a)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B, Lovvalgsbestemmelse.ART_11_3_b)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2, Lovvalgsbestemmelse.ART_11_4)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Lovvalgsbestemmelse.ART_12_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2, Lovvalgsbestemmelse.ART_12_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Lovvalgsbestemmelse.ART_13_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, Lovvalgsbestemmelse.ART_13_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2, Lovvalgsbestemmelse.ART_13_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3, Lovvalgsbestemmelse.ART_13_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4, Lovvalgsbestemmelse.ART_13_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, Lovvalgsbestemmelse.ART_13_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B, Lovvalgsbestemmelse.ART_13_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, Lovvalgsbestemmelse.ART_13_3)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4, Lovvalgsbestemmelse.ART_13_4)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, Lovvalgsbestemmelse.ART_16)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Lovvalgsbestemmelse.ART_16)
            .build());

    private final String kode;

    @JsonValue
    public String getKode() {
        return kode;
    }

    public static Lovvalgsbestemmelse av(LovvalgBestemmelse lovvalgBestemmelse) {
        return Optional.ofNullable(LOVVALGSBESTEMMELSE_MAP.get(lovvalgBestemmelse))
            .orElseThrow(() -> new UnsupportedOperationException(
                String.format("Lovvalgsbestemmelse %s støttes ikke for melding om utstedt A1", lovvalgBestemmelse)));
    }

    public static Lovvalgsbestemmelse avKonvensjonEftaStorbritannia(KonvEftaStorbritanniaLovvalgbestemmelser lovvalgBestemmelse) {
        return av(KonvEftaStorbritanniaLovvalgbestemmelser.GB_KONV_LOVVALGBESTEMMELSE_MAP.get(lovvalgBestemmelse));
    }

    Lovvalgsbestemmelse(String kode) {
        this.kode = kode;
    }
}
