package no.nav.melosys.service.kontroll.ufm;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import org.springframework.stereotype.Service;

@Service
public class UfmRegelsett {

    static Set<Function<UfmKontrollData, Kontroll_begrunnelser>> hentRegelsettForSedType(final SedType sedType) {
        return switch (sedType) {
            case A001 -> REGELSETT_A001;
            case A003 -> REGELSETT_A003;
            case A009 -> REGELSETT_A009;
            case A010 -> REGELSETT_A010;
            default ->
                throw new UnsupportedOperationException("SedType: %s er ikke støttet for automatiske kontroller".formatted(sedType));
        };
    }

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A001 = Set.of(
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

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A003 = Set.of(
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

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A009 = Set.of(
        UfmKontroller::periodeErÅpen,
        UfmKontroller::periodeStarterFørFørsteJuni2012,
        UfmKontroller::periodeOver24Mnd,
        UfmKontroller::periodeOver1ÅrFremITid,
        UfmKontroller::overlappendeMedlemsperiode,
        UfmKontroller::lovvalgslandErNorge,
        UfmKontroller::statsborgerskapIkkeMedlemsland,
        UfmKontroller::personDød,
        UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroller::utbetaltBarnetrygdytelser,
        UfmKontroller::arbeidssted
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A010 = Set.of(
        UfmKontroller::periodeErÅpen,
        UfmKontroller::periodeStarterFørFørsteJuni2012,
        UfmKontroller::periodeOver5År,
        UfmKontroller::periodeOver1ÅrFremITid,
        UfmKontroller::overlappendeMedlemsperiode,
        UfmKontroller::lovvalgslandErNorge,
        UfmKontroller::statsborgerskapIkkeMedlemsland,
        UfmKontroller::personDød,
        UfmKontroller::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroller::utbetaltBarnetrygdytelser,
        UfmKontroller::arbeidssted
    );
}
