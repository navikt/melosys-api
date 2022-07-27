package no.nav.melosys.service.kontroll.feature.unntaksperiode.kontroll;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class UnntaksperiodeKontrollsett {

    private UnntaksperiodeKontrollsett() {
    }

    public static Set<Function<UnntaksperiodeKontrollData, Kontrollfeil>> hentRegelsett(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING -> REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
            default -> Collections.emptySet();
        };
    }

    private static final Set<Function<UnntaksperiodeKontrollData, Kontrollfeil>> REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING =
        Set.of(UnntaksperiodeKontroll::periodeOver24MånederOgEnDag);
}
