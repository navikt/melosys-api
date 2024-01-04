package no.nav.melosys.service.kontroll.feature.ufm.data

import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.person.Persondata
import java.util.*


@JvmRecord
data class UfmKontrollData(
    val sedDokument: SedDokument,
    val persondata: Persondata,
    val medlemskapDokument: MedlemskapDokument,
    val inntektDokument: InntektDokument,
    val utbetalingDokument: UtbetalingDokument,
    val mottatteOpplysningerData: Optional<MottatteOpplysningerData>? = null,
    val personhistorikkDokumenter: List<PersonhistorikkDokument?>
)
