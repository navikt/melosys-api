package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public interface ErPeriodeMedBestemmelse extends ErPeriode {

    LovvalgBestemmelse getBestemmelse();

    Landkoder getLovvalgsland();

    LovvalgBestemmelse getTilleggsbestemmelse();

    Trygdedekninger getDekning();
}
