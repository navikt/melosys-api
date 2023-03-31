package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.data.util.Pair;

import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.AVSLAATT;
import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET;
import static no.nav.melosys.domain.kodeverk.Medlemskapstyper.PLIKTIG;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;

final class UtledMedlemskapsperioder {

    private UtledMedlemskapsperioder() {
    }

    static Collection<Medlemskapsperiode> lagMedlemskapsperioder(UtledMedlemskapsperioderRequest request) {
        final ErPeriode søknadsperiode = request.getSøknadsperiode();

        final LocalDate enMånedFørMottaksdato = request.getMottaksdatoSøknad().minusMonths(1);
        if (søknadsperiode.getFom().equals(enMånedFørMottaksdato) || søknadsperiode.getFom().isAfter(enMånedFørMottaksdato)) {
            return Collections.singleton(lagPeriode(søknadsperiode, request.getTrygdedekning(), request.getBestemmelse(), request.getArbeidsland(), INNVILGET));
        }

        if (søknadsperiode.getFom().isBefore(request.getMottaksdatoSøknad().minusYears(2))) {
            return Collections.singleton(lagPeriode(søknadsperiode, request.getTrygdedekning(), request.getBestemmelse(), request.getArbeidsland(), AVSLAATT));
        }

        return lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(request);
    }

    private static Collection<Medlemskapsperiode> lagMedlemskapsperioderPeriodeStarterMindreEnn2ÅrFørMottaksdato(UtledMedlemskapsperioderRequest request) {
        final ErPeriode søknadsperiode = request.getSøknadsperiode();
        if (erKunPensjonsdel(request.getTrygdedekning())) {
            return Collections.singleton(lagPeriode(søknadsperiode, request.getTrygdedekning(), request.getBestemmelse(), request.getArbeidsland(), INNVILGET));
        }

        if (søknadsperiode.getTom() != null && søknadsperiode.getTom().isBefore(request.getMottaksdatoSøknad())) {
            return lagMedlemskapsperioderForPeriodeFørMottaksdato(søknadsperiode, request);
        }

        final Pair<ErPeriode, ErPeriode> splittetPeriode = splitPeriode(søknadsperiode, request.getMottaksdatoSøknad());
        return Stream
            .concat(
                lagMedlemskapsperioderForPeriodeFørMottaksdato(splittetPeriode.getFirst(), request).stream(),
                Stream.of(lagPeriode(splittetPeriode.getSecond(), request.getTrygdedekning(), request.getBestemmelse(), request.getArbeidsland(), INNVILGET)))
            .collect(Collectors.toSet());
    }


    private static Collection<Medlemskapsperiode> lagMedlemskapsperioderForPeriodeFørMottaksdato(ErPeriode periode, UtledMedlemskapsperioderRequest request) {
        if (harPensjonsdel(request.getTrygdedekning())) {
            return Set.of(
                lagPeriode(periode, trygdedekningUtenPensjondel(request.getTrygdedekning()), request.getBestemmelse(), request.getArbeidsland(), AVSLAATT),
                lagPeriode(periode, PENSJONSDEL, request.getBestemmelse(), request.getArbeidsland(), INNVILGET)
            );
        }
        return Set.of(lagPeriode(periode, request.getTrygdedekning(), request.getBestemmelse(), request.getArbeidsland(), AVSLAATT));
    }

    private static Medlemskapsperiode lagPeriode(final ErPeriode søknadsperiode,
                                                 final Trygdedekninger trygdedekning,
                                                 final Folketrygdloven_kap2_bestemmelser bestemmelse,
                                                 final String arbeidsland,
                                                 final InnvilgelsesResultat innvilgelsesResultat) {
        return new Medlemskapsperiode(
            søknadsperiode.getFom(), søknadsperiode.getTom(), arbeidsland, bestemmelse, innvilgelsesResultat, PLIKTIG, trygdedekning
        );
    }

    private static Pair<ErPeriode, ErPeriode> splitPeriode(final ErPeriode periode,
                                                           final LocalDate splitFra) {
        return Pair.of(
            new Periode(periode.getFom(), splitFra.minusDays(1)),
            new Periode(splitFra, periode.getTom())
        );
    }

    private static Trygdedekninger trygdedekningUtenPensjondel(final Trygdedekninger trygdedekning) {
        if (trygdedekning == HELSE_OG_PENSJONSDEL) {
            return HELSEDEL;
        }
        if (trygdedekning == HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER) {
            return HELSEDEL_MED_SYKE_OG_FORELDREPENGER;
        }
        throw new FunksjonellException("Trygdedekning " + trygdedekning + " har ikke pensjonsdel");
    }

    private static boolean harPensjonsdel(final Trygdedekninger trygdedekninger) {
        return trygdedekninger == HELSE_OG_PENSJONSDEL || trygdedekninger == HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER;
    }

    private static boolean erKunPensjonsdel(final Trygdedekninger trygdedekning) {
        return trygdedekning == PENSJONSDEL;
    }

}
