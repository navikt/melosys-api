package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import org.springframework.data.util.Pair;

import static no.nav.melosys.domain.InnvilgelsesResultat.*;
import static no.nav.melosys.domain.kodeverk.Medlemskapstyper.FRIVILLIG;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;

final class UtledMedlemskapsperioder {

    private UtledMedlemskapsperioder() {
    }

    static Collection<Medlemskapsperiode> lagMedlemskapsperioder(final ErPeriode søknadsperiode,
                                                                 final Trygdedekninger søktTrygdedekning,
                                                                 final LocalDate mottaksdatoSøknad,
                                                                 final Folketrygdloven_kap2_bestemmelser bestemmelse,
                                                                 final String arbeidsland) {

        final LocalDate enMånedFørMottaksdato = mottaksdatoSøknad.minusMonths(1);
        if (søknadsperiode.getFom().equals(enMånedFørMottaksdato) || søknadsperiode.getFom().isAfter(enMånedFørMottaksdato)) {
            return Collections.singleton(lagPeriode(søknadsperiode, søktTrygdedekning, bestemmelse, arbeidsland, INNVILGET));
        }

        if (søknadsperiode.getFom().isBefore(mottaksdatoSøknad.minusYears(2))) {
            return Collections.singleton(lagPeriode(søknadsperiode, søktTrygdedekning, bestemmelse, arbeidsland, AVSLAATT));
        } else {
            return lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(søknadsperiode, søktTrygdedekning, mottaksdatoSøknad, bestemmelse, arbeidsland);
        }
    }

    private static Collection<Medlemskapsperiode> lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(final ErPeriode søknadsperiode,
                                                                                                                 final Trygdedekninger søktTrygdedekning,
                                                                                                                 final LocalDate mottaksdatoSøknad,
                                                                                                                 final Folketrygdloven_kap2_bestemmelser bestemmelse,
                                                                                                                 final String arbeidsland) {
        if (erPensjonsdel(søktTrygdedekning)) {
            return Collections.singleton(lagPeriode(søknadsperiode, søktTrygdedekning, bestemmelse, arbeidsland, INNVILGET));
        }

        final Pair<Trygdedekninger, InnvilgelsesResultat> trygdeDekningOgresultat = utledDekningOgResultatPeriodeStarterFørMottaksdato(søktTrygdedekning);
        if (søknadsperiode.getTom() != null && søknadsperiode.getTom().isBefore(mottaksdatoSøknad)) {
            return Collections.singleton(lagPeriode(søknadsperiode, trygdeDekningOgresultat.getFirst(), bestemmelse, arbeidsland, trygdeDekningOgresultat.getSecond()));
        }

        final Pair<ErPeriode, ErPeriode> splittetPeriode = splitPeriode(søknadsperiode, mottaksdatoSøknad);
        return Set.of(
            lagPeriode(splittetPeriode.getFirst(), trygdeDekningOgresultat.getFirst(), bestemmelse, arbeidsland, trygdeDekningOgresultat.getSecond()),
            lagPeriode(splittetPeriode.getSecond(), søktTrygdedekning, bestemmelse, arbeidsland, INNVILGET)
        );
    }

    private static Pair<Trygdedekninger, InnvilgelsesResultat> utledDekningOgResultatPeriodeStarterFørMottaksdato(final Trygdedekninger søktTrygdedekning) {
        if (erHelsedelOgPensjonsdel(søktTrygdedekning)) {
            return Pair.of(PENSJONSDEL, DELVIS_INNVILGET);
        } else if (erPensjonsdel(søktTrygdedekning)) {
            return Pair.of(søktTrygdedekning, INNVILGET);
        }

        return Pair.of(søktTrygdedekning, AVSLAATT);
    }

    private static Medlemskapsperiode lagPeriode(final ErPeriode søknadsperiode,
                                                 final Trygdedekninger trygdedekning,
                                                 final Folketrygdloven_kap2_bestemmelser bestemmelse,
                                                 final String arbeidsland,
                                                 final InnvilgelsesResultat innvilgelsesResultat) {
        return new Medlemskapsperiode(
            søknadsperiode.getFom(), søknadsperiode.getTom(), arbeidsland, bestemmelse, innvilgelsesResultat, FRIVILLIG, trygdedekning
        );
    }

    private static Pair<ErPeriode, ErPeriode> splitPeriode(final ErPeriode periode,
                                                           final LocalDate splitFra) {
        return Pair.of(
            new Periode(periode.getFom(), splitFra.minusDays(1)),
            new Periode(splitFra, periode.getTom())
        );
    }

    private static boolean erHelsedelOgPensjonsdel(final Trygdedekninger trygdedekning) {
        return trygdedekning == HELSE_OG_PENSJONSDEL || trygdedekning == HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
    }

    private static boolean erPensjonsdel(final Trygdedekninger trygdedekning) {
        return trygdedekning == PENSJONSDEL;
    }

}
