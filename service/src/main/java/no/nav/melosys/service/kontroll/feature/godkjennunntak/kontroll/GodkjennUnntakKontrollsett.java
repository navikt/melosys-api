package no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.GodkjennUnntakKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class GodkjennUnntakKontrollsett {

    private GodkjennUnntakKontrollsett() {
    }

    public static Set<Function<GodkjennUnntakKontrollData, Kontrollfeil>> hentRegelsett(Behandlingstema behandlingstema,
                                                                                        SedType sedType) {
        if (skalSjekkeGodkjennUnntak(behandlingstema, sedType)) {
            return REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        }
        return Collections.emptySet();
    }

    private static boolean skalSjekkeGodkjennUnntak(Behandlingstema behandlingstema, SedType sedType) {
        return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.equals(behandlingstema)
            && SedType.A009.equals(sedType);
    }

    private static final Set<Function<GodkjennUnntakKontrollData, Kontrollfeil>> REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING =
        Set.of(GodkjennUnntakKontroll::periodeOver24MånederOgEnDag);
}
