package no.nav.melosys.service.unntaksperiode.kontroll;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import org.springframework.stereotype.Service;

@Service
class KontrollFactory {

    List<Function<KontrollData, Unntak_periode_begrunnelser>> hentKontrollerForSedType(final SedType sedType) {
        switch (sedType) {
            case A003:
                return a003Kontroller();
            case A009:
            case A010:
                return a009Kontroller();
            default:
                throw new UnsupportedOperationException("SedType: " + sedType + " er ikke støttet for automatiske kontroller");
        }
    }

    private List<Function<KontrollData, Unntak_periode_begrunnelser>> a003Kontroller() {
        return Lists.newArrayList(
            UnntaksperiodeKontroller::periodeErÅpen,
            UnntaksperiodeKontroller::periodeEldreEnn5År,
            UnntaksperiodeKontroller::periodeOver24Mnd,
            UnntaksperiodeKontroller::periodeOver1ÅrFremITid,
            UnntaksperiodeKontroller::overlappendeMedlemsperiode,
            UnntaksperiodeKontroller::statsborgerskapIkkeMedlemsland,
            UnntaksperiodeKontroller::personDød,
            UnntaksperiodeKontroller::personBosattINorge,
            UnntaksperiodeKontroller::utbetaltYtelserFraOffentligIPeriode
        );
    }

    private List<Function<KontrollData, Unntak_periode_begrunnelser>> a009Kontroller() {
        return Lists.newArrayList(
            UnntaksperiodeKontroller::periodeErÅpen,
            UnntaksperiodeKontroller::periodeEldreEnn5År,
            UnntaksperiodeKontroller::periodeOver24Mnd,
            UnntaksperiodeKontroller::periodeOver1ÅrFremITid,
            UnntaksperiodeKontroller::overlappendeMedlemsperiode,
            UnntaksperiodeKontroller::lovvalgslandErNorge,
            UnntaksperiodeKontroller::statsborgerskapIkkeMedlemsland,
            UnntaksperiodeKontroller::personDød
        );
    }
}
