package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;
import java.util.Optional;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

final class SedTypeTilBehandlingstemaMapper {

    private SedTypeTilBehandlingstemaMapper() {
    }

    static Optional<Behandlingstema> finnBehandlingstemaForSedType(String sedType, String lovvalgsland) {
        SedType sedTypeEnum = finnSedType(sedType);

        if (sedTypeEnum == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(finnBehandlingstema(sedTypeEnum, lovvalgsland));
    }

    private static SedType finnSedType(String sedType) {
        if (sedType == null) return null;

        return Arrays.stream(SedType.values())
            .filter(s -> s.name().equals(sedType))
            .findFirst().orElse(null);
    }

    private static Behandlingstema finnBehandlingstema(SedType sedType, String lovvalgsland) {
        switch (sedType) {
            case A001:
                return Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL;
            case A003:
                return behandlingstemaForA003(lovvalgsland);
            case A009:
                return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
            case A010:
                return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE;
            default:
                return null;
        }
    }

    private static Behandlingstema behandlingstemaForA003(String lovvalgsland) {
        return Landkoder.NO.getKode().equalsIgnoreCase(lovvalgsland)
            ? Behandlingstema.BESLUTNING_LOVVALG_NORGE : Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
    }
}
