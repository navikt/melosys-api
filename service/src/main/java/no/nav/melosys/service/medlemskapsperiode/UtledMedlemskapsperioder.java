package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public final class UtledMedlemskapsperioder {

    private UtledMedlemskapsperioder() {}

    static Collection<Medlemskapsperiode> lagMedlemskapsperioder(final ErPeriode søknadperiode,
                                                                 final Trygdedekninger søktTrygdedekning,
                                                                 final LocalDate mottaksdatoSøknad,
                                                                 final Folketrygdloven_kap2_bestemmelser folketrygdloven_kap2_bestemmelser,
                                                                 final String arbeidsland) {
        return Collections.emptyList();
    }

}
