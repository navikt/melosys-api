package no.nav.melosys.service.kontroll.feature.unntaksperiode.kontroll;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class UnntaksperiodeKontrollsett {

    private UnntaksperiodeKontrollsett() {
    }

    public static Set<Function<UnntaksperiodeKontrollData, Kontrollfeil>> hentRegelsett(SedType sedType) {
        return switch (sedType) {
            case A009 -> REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
            default -> Collections.emptySet();
        };
    }

    private static final Set<Function<UnntaksperiodeKontrollData, Kontrollfeil>> REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING =
        Set.of(UnntaksperiodeKontroll::periodeOver24MånederOgEnDag);
}
