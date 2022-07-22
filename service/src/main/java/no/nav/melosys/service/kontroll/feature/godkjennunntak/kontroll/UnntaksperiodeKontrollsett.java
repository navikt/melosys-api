package no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

import static java.util.Objects.nonNull;

public final class UnntaksperiodeKontrollsett {

    private UnntaksperiodeKontrollsett() {
    }

    public static Set<Function<UnntaksperiodeKontrollData, Kontrollfeil>> hentRegelsett(Behandling behandling) {
        if (skalBehandlingKontrollereGodkjennUnntak(behandling)) {
            return REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        }
        return Collections.emptySet();
    }

    private static boolean skalBehandlingKontrollereGodkjennUnntak(Behandling behandling) {
        SedDokument sedDokument = behandling.hentSedDokument();
        return nonNull(sedDokument)
            && Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.equals(behandling.getTema())
            && SedType.A009.equals(sedDokument.getSedType());
    }

    private static final Set<Function<UnntaksperiodeKontrollData, Kontrollfeil>> REGELSETT_UNNTAK_NORSK_TRYGD_UTSTASJONERING =
        Set.of(UnntaksperiodeKontroll::periodeOver24MånederOgEnDag);
}
