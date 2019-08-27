package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Landkoder;

public interface Medlemskapsperiode extends ErPeriode {

    LovvalgBestemmelse getBestemmelse();

    Landkoder getLovvalgsland();

    LovvalgBestemmelse getTilleggsbestemmelse();

    Trygdedekninger getDekning();
}
