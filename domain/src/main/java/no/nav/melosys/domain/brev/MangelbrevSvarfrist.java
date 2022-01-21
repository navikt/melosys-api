package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.time.Period;
import java.util.Collections;
import java.util.List;

public class MangelbrevSvarfrist {

    // Svarfrist mangelbrev 4 uker fra dato brevet blir generert.
    public static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    public static Instant beregnFristFraDato(Instant brevdato) {
        return brevdato.plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV));
    }
}
