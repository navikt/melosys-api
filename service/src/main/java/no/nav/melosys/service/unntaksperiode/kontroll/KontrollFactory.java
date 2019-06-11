package no.nav.melosys.service.unntaksperiode.kontroll;

import java.util.Collections;
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
                return a009Kontroller();
        }
        return Collections.emptyList();
    }

    private List<Function<KontrollData, Unntak_periode_begrunnelser>> a003Kontroller() {
        return Lists.newArrayList(
            PeriodeKontroller::periodeErÅpen,
            PeriodeKontroller::periodeEldreEnn5År,
            PeriodeKontroller::periodeMaks24Mnd,
            PeriodeKontroller::periodeOver1ÅrFremITid,
            MedlemskapKontroller::overlappendeMedlemsperiode,
            MedlemskapKontroller::statsborgerskapIkkeMedlemsland,
            PersonKontroller::personDød,
            PersonKontroller::personBosattINorge,
            InntektKontroller::utbetaltYtelserFraOffentligIPeriode
        );
    }

    private List<Function<KontrollData, Unntak_periode_begrunnelser>> a009Kontroller() {
        return Lists.newArrayList(
            PeriodeKontroller::periodeErÅpen,
            PeriodeKontroller::periodeEldreEnn5År,
            PeriodeKontroller::periodeMaks24Mnd,
            PeriodeKontroller::periodeOver1ÅrFremITid,
            MedlemskapKontroller::overlappendeMedlemsperiode,
            MedlemskapKontroller::lovvalgslandErNorge,
            MedlemskapKontroller::statsborgerskapIkkeMedlemsland,
            PersonKontroller::personDød
        );
    }
}
