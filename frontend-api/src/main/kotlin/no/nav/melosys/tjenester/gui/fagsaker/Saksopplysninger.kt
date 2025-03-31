package no.nav.melosys.tjenester.gui.fagsaker

import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger

data class Saksopplysninger(
    val sakstype: Sakstyper,
    val saksgrunnlagsbehandlingId: Long,
    val sedDokument: SedDokument?,
    val motatteOpplysninger: MottatteOpplysninger?
)
