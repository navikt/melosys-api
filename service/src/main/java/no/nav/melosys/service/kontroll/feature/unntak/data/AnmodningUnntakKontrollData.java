package no.nav.melosys.service.kontroll.feature.unntak.data;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;

public record AnmodningUnntakKontrollData(Persondata persondata,
                                          MottatteOpplysningerData mottatteOpplysningerData,
                                          Anmodningsperiode anmodningsperiode,
                                          int antallArbeidsgivere) {
}
