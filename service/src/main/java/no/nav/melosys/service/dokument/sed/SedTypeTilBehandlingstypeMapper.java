package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;
import java.util.Optional;

import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public final class SedTypeTilBehandlingstypeMapper {

    public static Optional<Behandlingstyper> finnBehandlingstypeForSedType(String sedType, String lovvalgsland) {
        SedType sedTypeEnum = finnSedType(sedType);

        if (sedTypeEnum == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(finnBehandlingstype(sedTypeEnum, lovvalgsland));
    }

    private static SedType finnSedType(String sedType) {
        if (sedType == null) return null;

        return Arrays.stream(SedType.values())
            .filter(s -> s.name().equals(sedType))
            .findFirst().orElse(null);
    }

    private static Behandlingstyper finnBehandlingstype(SedType sedType, String lovvalgsland) {
        switch (sedType) {
            case A001:
                return Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL;
            case A003:
                return behandlingstypeForA003(lovvalgsland);
            case A009:
                return Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
            case A010:
                return Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE;
            default:
                return null;
        }
    }

    private static Behandlingstyper behandlingstypeForA003(String lovvalgsland) {
        return Landkoder.NO.getKode().equalsIgnoreCase(lovvalgsland)
            ? Behandlingstyper.UTL_MYND_UTPEKT_NORGE : Behandlingstyper.UTL_MYND_UTPEKT_SEG_SELV;
    }
}
