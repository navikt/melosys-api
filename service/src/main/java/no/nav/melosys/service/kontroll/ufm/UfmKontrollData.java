package no.nav.melosys.service.kontroll.ufm;

import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.service.kontroll.KontrollData;

class UfmKontrollData extends KontrollData {

    private SedDokument sedDokument;
    private PersonDokument personDokument;
    private InntektDokument inntektDokument;
    private UtbetalingDokument utbetalingDokument;

    UfmKontrollData(SedDokument sedDokument,
                    PersonDokument personDokument,
                    MedlemskapDokument medlemskapDokument,
                    InntektDokument inntektDokument,
                    UtbetalingDokument utbetalingDokument) {
        super(medlemskapDokument);
        this.sedDokument = sedDokument;
        this.personDokument = personDokument;
        this.inntektDokument = inntektDokument;
        this.utbetalingDokument = utbetalingDokument;
    }

    SedDokument getSedDokument() {
        return sedDokument;
    }

    PersonDokument getPersonDokument() {
        return personDokument;
    }

    InntektDokument getInntektDokument() {
        return inntektDokument;
    }

    UtbetalingDokument getUtbetalingDokument() {
        return utbetalingDokument;
    }
}