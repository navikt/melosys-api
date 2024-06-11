package no.nav.melosys.service.dokument.brev.mapper.felles;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.eessi.sed.Bestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia;

import java.util.Map;

public class KonvEftaStorbritanniaLovvalgbestemmelser {
    public static final Map<LovvalgBestemmelse, Lovvalgbestemmelser_883_2004> GB_KONV_LOVVALGBESTEMMELSE_MAP =
        Maps.newHashMap(ImmutableMap.<LovvalgBestemmelse, Lovvalgbestemmelser_883_2004>builder()
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3B, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3C, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_2, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1A, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1B1, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B2, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B3, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B4, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2A, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2B, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_3, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_4, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_5, Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1)
            .put(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_2, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2)
            .build());

    public static final Map<LovvalgBestemmelse, Tilleggsbestemmelser_883_2004> GB_KONV_TILLEGGBESTEMMELSE_MAP =
        Maps.newHashMap(ImmutableMap.<LovvalgBestemmelse, Tilleggsbestemmelser_883_2004>builder()
            .put(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2)
            .put(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1)
            .put(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5)
            .build());
}
