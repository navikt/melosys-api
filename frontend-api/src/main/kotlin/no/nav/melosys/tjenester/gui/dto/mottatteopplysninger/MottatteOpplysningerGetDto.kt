package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger

import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import java.time.LocalDate

class MottatteOpplysningerGetDto(mottatteOpplysninger: MottatteOpplysninger) {
    val data: MottatteOpplysningerData = mottatteOpplysninger.mottatteOpplysningerData
    val type: Mottatteopplysningertyper = mottatteOpplysninger.type
    val mottaksdato: LocalDate = mottatteOpplysninger.mottaksdato
}
