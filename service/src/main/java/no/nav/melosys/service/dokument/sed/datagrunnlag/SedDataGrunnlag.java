package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.dokument.DataGrunnlag;

public interface SedDataGrunnlag extends DataGrunnlag {
    Persondata getPersondata();
}
