package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.time.Period;

public class DokumentasjonSvarfrist {

    private static final int ANTALL_UKER_DOKUMENTASJON_SVARFRIST_MANGELBREV = 4;
    private static final int ANTALL_UKER_DOKUMENTASJON_SVARFRIST_VED_MANUEL_ENDRING = 2;

    private DokumentasjonSvarfrist() {
    }

    public static Instant beregnFristPaaMangelbrevFraDagensDato() {
        return Instant.now().plus(Period.ofWeeks(ANTALL_UKER_DOKUMENTASJON_SVARFRIST_MANGELBREV));
    }


    public static Instant beregnFristFraDagensDatoVedManuelEndring() {
        return Instant.now().plus(Period.ofWeeks(ANTALL_UKER_DOKUMENTASJON_SVARFRIST_VED_MANUEL_ENDRING));
    }
}
