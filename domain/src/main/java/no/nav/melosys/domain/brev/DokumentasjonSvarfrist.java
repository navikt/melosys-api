package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.time.Period;

public class DokumentasjonSvarfrist {

    private static final int DOKUMENTASJON_SVARFRIST_MANGELBREV_UKER = 4;
    private static final int DOKUMENTASJON_SVARFRIST_UKER = 2;

    private DokumentasjonSvarfrist() {
    }

    public static Instant beregnFristPaaMangelbrevFraDagensDato() {
        return Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_MANGELBREV_UKER));
    }


    public static Instant beregnFristFraDagensDatoVedManuelEndring() {
        return Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER));
    }
}
