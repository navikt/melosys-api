package no.nav.melosys.domain.brev;

import java.time.LocalDate;
import java.time.Period;

public class TrygdeavgiftBetalingsfrist {

    public static final int TRYGDEAVGIFT_BETALINGSFRIST_UKER = 6;

    private TrygdeavgiftBetalingsfrist() {
    }

    public static LocalDate beregnTrygdeavgiftBetalingsfrist(LocalDate forsendelseMottatt) {
        return forsendelseMottatt.plus(Period.ofWeeks(TRYGDEAVGIFT_BETALINGSFRIST_UKER));
    }
}
