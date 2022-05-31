package no.nav.melosys.service.kontroll.feature.ufm.data;

import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.person.Persondata;

public class UfmKontrollData {

    private final MedlemskapDokument medlemskapDokument;
    private final SedDokument sedDokument;
    private final Persondata persondata;
    private final InntektDokument inntektDokument;
    private final UtbetalingDokument utbetalingDokument;

    UfmKontrollData(SedDokument sedDokument,
                    Persondata persondata,
                    MedlemskapDokument medlemskapDokument,
                    InntektDokument inntektDokument,
                    UtbetalingDokument utbetalingDokument) {
        this.medlemskapDokument = medlemskapDokument;
        this.sedDokument = sedDokument;
        this.persondata = persondata;
        this.inntektDokument = inntektDokument;
        this.utbetalingDokument = utbetalingDokument;
    }

    MedlemskapDokument getMedlemskapDokument() {
        return medlemskapDokument;
    }

    SedDokument getSedDokument() {
        return sedDokument;
    }

    Persondata getPersonDokument() {
        return persondata;
    }

    InntektDokument getInntektDokument() {
        return inntektDokument;
    }

    UtbetalingDokument getUtbetalingDokument() {
        return utbetalingDokument;
    }
}
