package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Objects;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;

public final class YtelseRegler {

    private YtelseRegler() {
    }

    public static boolean utbetaltYtelserFraOffentligIPeriode(InntektDokument inntektDokument, LocalDate fom, LocalDate tom) {

        if (inntektDokument == null || inntektDokument.getArbeidsInntektMaanedListe().isEmpty()) {
            return false;
        }

        YearMonth fra = YearMonth.from(fom);
        YearMonth til = tom != null ? YearMonth.from(tom) : null;

        for (YtelseFraOffentlige ytelseFraOffentlige : hentYtelseFraOffentlige(inntektDokument)) {
            if (erUtbetaltIPeriode(ytelseFraOffentlige, fra, til)) {
                return true;
            }
        }

        return false;
    }

    private static boolean erUtbetaltIPeriode(YtelseFraOffentlige ytelseFraOffentlige, YearMonth fom, YearMonth tom) {
        YearMonth utbetaltIPeriode = ytelseFraOffentlige.utbetaltIPeriode;

        if (utbetaltIPeriode == null) {
            return false;
        }

        if (tom == null) {
            tom = fom.plusYears(2);
        }

        if (utbetaltIPeriode.isAfter(fom) && utbetaltIPeriode.isBefore(tom)) {
            return true;
        } else {
            return utbetaltIPeriode.equals(fom) || utbetaltIPeriode.equals(tom);
        }
    }

    private static Collection<YtelseFraOffentlige> hentYtelseFraOffentlige(InntektDokument inntektDokument) {
        return inntektDokument.getArbeidsInntektMaanedListe().stream()
            .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
            .filter(Objects::nonNull)
            .map(ArbeidsInntektInformasjon::getInntektListe)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(YtelseFraOffentlige.class::isInstance)
            .map(YtelseFraOffentlige.class::cast)
            .filter(Objects::nonNull)
            .toList();
    }
}
