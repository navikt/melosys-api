package no.nav.melosys.service.kontroll.feature.ufm.data;

import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.person.Persondata;

public record UfmKontrollData(SedDokument sedDokument,
                              Persondata persondata,
                              MedlemskapDokument medlemskapDokument,
                              InntektDokument inntektDokument,
                              UtbetalingDokument utbetalingDokument) {
}
