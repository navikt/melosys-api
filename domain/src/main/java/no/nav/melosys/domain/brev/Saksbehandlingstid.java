package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Saksbehandlingstid {

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    public static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;

    public static Instant hentDatoBehandlingstid(Instant forsendelseMottatt) {
        return forsendelseMottatt.plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS);
    }
}
