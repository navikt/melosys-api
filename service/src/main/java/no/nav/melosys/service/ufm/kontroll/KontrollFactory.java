package no.nav.melosys.service.ufm.kontroll;

import java.util.List;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import org.springframework.stereotype.Service;

@Service
public class KontrollFactory {

    List<Function<UfmKontrollData, Kontroll_begrunnelser>> hentKontrollerForSedType(final SedType sedType) {
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

    private List<Function<UfmKontrollData, Kontroll_begrunnelser>> a001Kontroller() {
        return List.of(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeStarterFørFørsteJuni2012,
            UfmKontroller::periodeOver5År,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiode,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::personBosattINorge,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser,
            UfmKontroller::arbeidssted
        );
    }

    private List<Function<UfmKontrollData, Kontroll_begrunnelser>> a003Kontroller() {
        return List.of(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeStarterFørFørsteJuni2012,
            UfmKontroller::periodeOver5År,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiodeMerEnn1Dag,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::personBosattINorge,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser,
            UfmKontroller::arbeidssted
        );
    }

    private List<Function<UfmKontrollData, Kontroll_begrunnelser>> a009Kontroller() {
        return List.of(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeStarterFørFørsteJuni2012,
            UfmKontroller::periodeOver24MånederOgEnDag,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiodeMerEnn1Dag,
            UfmKontroller::lovvalgslandErNorge,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser,
            UfmKontroller::arbeidssted
        );
    }

    private List<Function<UfmKontrollData, Kontroll_begrunnelser>> a010Kontroller() {
        return List.of(
            UfmKontroller::periodeErÅpen,
            UfmKontroller::periodeStarterFørFørsteJuni2012,
            UfmKontroller::periodeOver5År,
            UfmKontroller::periodeOver1ÅrFremITid,
            UfmKontroller::overlappendeMedlemsperiodeMerEnn1Dag,
            UfmKontroller::lovvalgslandErNorge,
            UfmKontroller::statsborgerskapIkkeMedlemsland,
            UfmKontroller::personDød,
            UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
            UfmKontroller::utbetaltBarnetrygdytelser,
            UfmKontroller::arbeidssted
        );
    }
}
