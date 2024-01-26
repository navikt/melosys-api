package no.nav.melosys.tjenester.gui.dto

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.tjenester.gui.dto.eessi.SedDokumentDto
import no.nav.melosys.tjenester.gui.dto.inntekt.InntektDto

@JsonPropertyOrder("arbeidsforhold", "organisasjoner", "medlemskap", "inntekt", "sakOgBehandling", "sed")
class SaksopplysningerDto {
    // Frontend ønsker å motta et objekt, selv når saksopplysninger ikke finnes.
    @JvmField
    var arbeidsforhold: ArbeidsforholdDokument = ArbeidsforholdDokument()

    @JvmField
    var organisasjoner: List<OrganisasjonDokument> = ArrayList()

    @JvmField
    var medlemskap: MedlemskapDokument = MedlemskapDokument()

    @JvmField
    var inntekt: InntektDto = InntektDto()


    @JvmField
    var sed: SedDokumentDto = SedDokumentDto()
}
