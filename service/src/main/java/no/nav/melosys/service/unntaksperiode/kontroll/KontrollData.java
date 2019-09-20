package no.nav.melosys.service.unntaksperiode.kontroll;

import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;

class KontrollData {

    SedDokument sedDokument;
    PersonDokument personDokument;
    MedlemskapDokument medlemskapDokument;
    InntektDokument inntektDokument;
    UtbetalingDokument utbetalingDokument;

    KontrollData(SedDokument sedDokument,
                 PersonDokument personDokument,
                 MedlemskapDokument medlemskapDokument,
                 InntektDokument inntektDokument,
                 UtbetalingDokument utbetalingDokument) {
        this.sedDokument = sedDokument;
        this.personDokument = personDokument;
        this.medlemskapDokument = medlemskapDokument;
        this.inntektDokument = inntektDokument;
        this.utbetalingDokument = utbetalingDokument;
    }
}