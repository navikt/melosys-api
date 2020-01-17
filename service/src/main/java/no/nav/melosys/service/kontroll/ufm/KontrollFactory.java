package no.nav.melosys.service.kontroll.ufm;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import org.springframework.stereotype.Service;

@Service
class KontrollFactory {

    List<Function<UfmKontrollData, Unntak_periode_begrunnelser>> hentKontrollerForSedType(final SedType sedType) {
        switch (sedType) {
            case A001:
                return a001Kontroller();
            case A003:
                return a003Kontroller();
            case A009:
                return a009Kontroller();
            case A010:
                return a010Kontroller();
            default:
                throw new UnsupportedOperationException("SedType: " + sedType + " er ikke støttet for automatiske kontroller");
        }
    }

    private List<Function<UfmKontrollData, Unntak_periode_begrunnelser>> a001Kontroller() {
        return Lists.newArrayList(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeEldreEnn3År,
            UfmKontroller::periodeOver5År,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiode,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::personBosattINorge,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser
        );
    }

    private List<Function<UfmKontrollData, Unntak_periode_begrunnelser>> a003Kontroller() {
        return Lists.newArrayList(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeEldreEnn3År,
            UfmKontroller::periodeOver24Mnd,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiode,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::personBosattINorge,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser
        );
    }

    private List<Function<UfmKontrollData, Unntak_periode_begrunnelser>> a009Kontroller() {
        return Lists.newArrayList(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeEldreEnn3År,
            UfmKontroller::periodeOver24Mnd,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiode,
            UfmKontroller::lovvalgslandErNorge,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser
        );
    }

    private List<Function<UfmKontrollData, Unntak_periode_begrunnelser>> a010Kontroller() {
        return Lists.newArrayList(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeEldreEnn3År,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiode,
            UfmKontroller::lovvalgslandErNorge,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser
        );
    }
}
