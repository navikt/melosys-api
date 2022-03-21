package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.time.Period;

public class Saksbehandlingstid {

    public static final int SAKSBEHANDLINGSTID_UKER = 12;

    private Saksbehandlingstid() {
    }

    public static Instant beregnSaksbehandlingsfrist(Instant forsendelseMottatt) {
        return forsendelseMottatt.plus(Period.ofWeeks(SAKSBEHANDLINGSTID_UKER));
    }
}
