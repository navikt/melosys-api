package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;

public class GyldigBehandlingstema {
    public static boolean kanIkkeEndreBehandling(Behandlingstema behandlingstema) {
        return ikkeGyldigeBehandlingsTemaer().contains(behandlingstema);
    }

    private static Set<Behandlingstema> ikkeGyldigeBehandlingsTemaer() {
        return Set.of(
            REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
            BESLUTNING_LOVVALG_NORGE,
            BESLUTNING_LOVVALG_ANNET_LAND,
            ANMODNING_OM_UNNTAK_HOVEDREGEL
        );
    }
}

