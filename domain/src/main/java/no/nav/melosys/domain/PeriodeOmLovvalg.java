package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia.*;


public interface PeriodeOmLovvalg extends ErPeriode, HarBestemmelse<LovvalgBestemmelse> {
    LovvalgBestemmelse getBestemmelse();

    Behandlingsresultat getBehandlingsresultat();

    Land_iso2 getLovvalgsland();

    LovvalgBestemmelse getTilleggsbestemmelse();

    Trygdedekninger getDekning();

    Long getMedlPeriodeID();

    default boolean erArtikkel12() {
        return getBestemmelse() == FO_883_2004_ART12_1 || getBestemmelse() == FO_883_2004_ART12_2
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART14_1 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART14_2
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART16_1 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART16_3;
    }

    default boolean erArtikkel14_1_eller_14_2() {
        return getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART14_1 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART14_2;
    }

    default boolean erEftaStorbritannia() {
        return getBestemmelse().name().startsWith("KONV_EFTA");
    }

    default boolean erArbeidKunNorge() {
        return getBestemmelse() == FO_883_2004_ART11_3A;
    }

    default boolean erArtikkel13_3_a_eller_13_4() {
        return getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_3A || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_4 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_4_2;
    }

    default boolean erArtikkel11_3_a() {
        return  getBestemmelse() == FO_883_2004_ART11_3A;
    }

    default boolean erArtikkel11_3_a_eller_13_3a() {
        return getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_3A || getBestemmelse() == FO_883_2004_ART11_3A;
    }

    default boolean erArtikkel16_1_eller_16_3() {
        return getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART16_1 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART16_3;
    }

    default boolean erArtikkel18_1() {
        return getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART18_1;
    }


    default boolean erArtikkel13() {
        return erArtikkel13_1()
            || getBestemmelse() == FO_883_2004_ART13_2A || getBestemmelse() == FO_883_2004_ART13_2B
            || getBestemmelse() == FO_883_2004_ART13_3
            || getBestemmelse() == FO_883_2004_ART13_4
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_2A || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_2B
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_3
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_4;
    }

    default boolean erArtikkel13_1() {
        return getBestemmelse() == FO_883_2004_ART13_1A
            || getBestemmelse() == FO_883_2004_ART13_1B1 || getBestemmelse() == FO_883_2004_ART13_1B2
            || getBestemmelse() == FO_883_2004_ART13_1B3 || getBestemmelse() == FO_883_2004_ART13_1B4
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_1A
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_1B1 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_1_B2
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_1_B3 || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART15_1_B4;
    }

    default boolean erArtikkel11() {
        return getBestemmelse() == FO_883_2004_ART11_1
            || getBestemmelse() == FO_883_2004_ART11_3A
            || getBestemmelse() == FO_883_2004_ART11_3B
            || getBestemmelse() == FO_883_2004_ART11_3C
            || getBestemmelse() == FO_883_2004_ART11_3E
            || getBestemmelse() == FO_883_2004_ART11_4_2
            || getBestemmelse() == FO_883_2004_ART11_5
            || getTilleggsbestemmelse() == FO_883_2004_ART11_2
            || getTilleggsbestemmelse() == FO_883_2004_ART11_4_1
            || getTilleggsbestemmelse() == FO_883_2004_ART11_5
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_3A
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_3B
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_3D
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_4_2
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_5
            || getTilleggsbestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_2
            || getTilleggsbestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_4_2
            || getTilleggsbestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_5;
    }

    default boolean erArtikkel11_4() {
        return getBestemmelse() == FO_883_2004_ART11_4_2
            || getTilleggsbestemmelse() == FO_883_2004_ART11_4_1
            || getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_4_2
            || getTilleggsbestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_4_1;
    }

    default boolean erArtikkel11_3aMed11_5Tilleggsbestemmelse() {
        return (getBestemmelse() == FO_883_2004_ART11_3A && getTilleggsbestemmelse() == FO_883_2004_ART11_5)
            || (getBestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_3A && getTilleggsbestemmelse() == KONV_EFTA_STORBRITANNIA_ART13_5);
    }

    default boolean erNyPeriodeForMedl() {
        return getMedlPeriodeID() == null;
    }

    default boolean harForskjelligMedlID(Long medlPeriodeIdSomSjekkes) {
        return getMedlPeriodeID() == null || !getMedlPeriodeID().equals(medlPeriodeIdSomSjekkes);
    }
}
