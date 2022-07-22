package no.nav.melosys.domain.saksflyt;

import java.util.EnumSet;
import java.util.Set;

public enum ProsessStatus {
    KLAR,
    FEILET,
    FERDIG,
    PÅ_VENT,
    UNDER_BEHANDLING,
    RESTARTET;

    private static final Set<ProsessStatus> AKTIVE_STATUSER = EnumSet.noneOf(ProsessStatus.class);

    public static Set<ProsessStatus> hentAktiveStatuser() {
        return AKTIVE_STATUSER;
    }

    static {
        AKTIVE_STATUSER.addAll(EnumSet.of(KLAR, UNDER_BEHANDLING, PÅ_VENT, RESTARTET));
    }
}
