package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004.*;


public interface PeriodeOmLovvalg extends ErPeriode {
    LovvalgBestemmelse getBestemmelse();

    Landkoder getLovvalgsland();

    LovvalgBestemmelse getTilleggsbestemmelse();

    Trygdedekninger getDekning();

    Long getMedlPeriodeID();

    default boolean erArtikkel12() {
        return getBestemmelse() == FO_883_2004_ART12_1 || getBestemmelse() == FO_883_2004_ART12_2;
    }

    default boolean erArtikkel13() {
        return getBestemmelse() == FO_883_2004_ART13_1A
            || getBestemmelse() == FO_883_2004_ART13_1B1 || getBestemmelse() == FO_883_2004_ART13_1B2
            || getBestemmelse() == FO_883_2004_ART13_1B3 || getBestemmelse() == FO_883_2004_ART13_1B4
            || getBestemmelse() == FO_883_2004_ART13_2A || getBestemmelse() == FO_883_2004_ART13_2B
            || getBestemmelse() == FO_883_2004_ART13_3
            || getBestemmelse() == FO_883_2004_ART13_4;
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
            || getTilleggsbestemmelse() == FO_883_2004_ART11_5;
    }

    default boolean erArtikkel11_4() {
        return getBestemmelse() == FO_883_2004_ART11_4_2
            || getTilleggsbestemmelse() == FO_883_2004_ART11_4_1;
    }

    default boolean erArtikkel11_3aMed11_5Tilleggsbestemmelse() {
        return getBestemmelse() == FO_883_2004_ART11_3A && getTilleggsbestemmelse() == FO_883_2004_ART11_5;
    }
}
