package no.nav.melosys.service.unntaksperiode.kontroll;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;


final class KontrollFactory {

    private KontrollFactory() {
    }

    static List<Function<KontrollData, Unntak_periode_begrunnelser>> hentKontrollerForSedType(final String sedType) {
        switch (sedType) {
            case "A003":
                return a003Kontroller();
            case "A009":
                return a009Kontroller();
        }
        return Collections.emptyList();
    }

    private static List<Function<KontrollData, Unntak_periode_begrunnelser>> a003Kontroller() {
        return Lists.newArrayList(
            UnntaksperiodeKontroller::periodeErÅpen,
            UnntaksperiodeKontroller::periodeEldreEnn5År,
            UnntaksperiodeKontroller::periodeMaks24Mnd,
            UnntaksperiodeKontroller::periodeOver1ÅrFremITid,
            UnntaksperiodeKontroller::overlappendeMedlemsperiode,
            UnntaksperiodeKontroller::lovvalgslandErNorge,
            UnntaksperiodeKontroller::personDød,
            UnntaksperiodeKontroller::personBosattINorge,
            UnntaksperiodeKontroller::statsborgerskapIkkeMedlemsland,
            UnntaksperiodeKontroller::utbetaltYtelserFraOffentligIPeriode
        );
    }

    private static List<Function<KontrollData, Unntak_periode_begrunnelser>> a009Kontroller() {
        return Lists.newArrayList(
            UnntaksperiodeKontroller::periodeErÅpen,
            UnntaksperiodeKontroller::periodeEldreEnn5År,
            UnntaksperiodeKontroller::periodeMaks24Mnd,
            UnntaksperiodeKontroller::periodeOver1ÅrFremITid,
            UnntaksperiodeKontroller::overlappendeMedlemsperiode,
            UnntaksperiodeKontroller::lovvalgslandErNorge,
            UnntaksperiodeKontroller::personDød,
            UnntaksperiodeKontroller::statsborgerskapIkkeMedlemsland
        );
    }
}
