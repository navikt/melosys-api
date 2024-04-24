package no.nav.melosys.service.kontroll.feature.ufm.kontroll;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData;
import org.springframework.stereotype.Service;

@Service
public class UfmKontrollsett {

    public static Set<Function<UfmKontrollData, Kontroll_begrunnelser>> hentRegelsettForSedType(final SedType sedType, final Boolean CDM4_3toggleEnabled) {
        return switch (sedType) {
            case A001 -> CDM4_3toggleEnabled ? REGELSETT_A001_4_3 : REGELSETT_A001;
            case A003 -> CDM4_3toggleEnabled ? REGELSETT_A003_4_3 : REGELSETT_A003;
            case A009 -> CDM4_3toggleEnabled ? REGELSETT_A009_4_3 : REGELSETT_A009;
            case A010 -> CDM4_3toggleEnabled ? REGELSETT_A010_4_3 : REGELSETT_A010;
            default -> throw new UnsupportedOperationException("SedType: %s er ikke støttet for automatiske kontroller".formatted(sedType));
        };
    }

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A001 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiode,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::personBosattINorge,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidssted
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A003 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeForA003,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::personBosattINorgeIPerioden,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidssted,
        UfmKontroll::unntakForA003
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A009 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver24MånederOgEnDag,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeMerEnn1Dag,
        UfmKontroll::lovvalgslandErNorge,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidssted
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A010 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeMerEnn1Dag,
        UfmKontroll::lovvalgslandErNorge,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidssted
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A001_4_3 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiode,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::personBosattINorge,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A003_4_3 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeForA003,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::personBosattINorgeIPerioden,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland,
        UfmKontroll::unntakForA003
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A009_4_3 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver24MånederOgEnDag,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeMerEnn1Dag,
        UfmKontroll::lovvalgslandErNorge,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland
    );

    private static final Set<Function<UfmKontrollData, Kontroll_begrunnelser>> REGELSETT_A010_4_3 = Set.of(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeMerEnn1Dag,
        UfmKontroll::lovvalgslandErNorge,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland
    );
}
