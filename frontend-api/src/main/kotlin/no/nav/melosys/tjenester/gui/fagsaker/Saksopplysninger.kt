package no.nav.melosys.tjenester.gui.fagsaker

import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger

data class Saksopplysninger(
    val sakstype: Sakstyper,
    val saksgrunnlagsbehandlingId: Long? = null,
    val sedDokument: SedDokument? = null,
    val mottatteOpplysninger: MottatteOpplysninger? = null,
)
