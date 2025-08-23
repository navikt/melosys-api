package no.nav.melosys.domain.brev.utkast

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.brev.StandardvedleggType
import no.nav.melosys.domain.brev.utkast.Utkast.FritekstVedlegg
import no.nav.melosys.domain.brev.utkast.Utkast.Saksvedlegg
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
@JvmRecord
data class BrevbestillingUtkast(
    val produserbardokument: Produserbaredokumenter?,
    val mottaker: Mottakerroller?,
    val orgnr: String?,
    val orgnrNorskMyndighet: List<String?>?,
    val institusjonID: String?,
    val innledningFritekst: String?,
    val manglerFritekst: String?,
    val begrunnelseFritekst: String?,
    val ektefelleFritekst: String?,
    val barnFritekst: String?,
    val trygdeavgiftFritekst: String?,
    val kontaktpersonNavn: String?,
    val kopiMottakere: List<KopiMottakerUtkast?>?,
    val fritekstTittel: String?,
    val fritekst: String?,
    val distribusjonstype: Distribusjonstype?,
    val kontaktopplysninger: Boolean,
    val nyVurderingBakgrunn: String?,
    val saksVedlegg: MutableList<Saksvedlegg?>?,
    val standardvedleggType: StandardvedleggType?,
    val fritekstVedlegg: MutableList<FritekstVedlegg?>?,
    val dokumentTittel: String?,
    val saksbehandlerNrToIdent: String?,
    val skalViseStandardTekstOmOpplysninger: Boolean
) {
    @JsonIgnore
    fun getTittel(): String? = when {
        dokumentTittel.isNullOrEmpty() -> produserbardokument?.beskrivelse
        else -> dokumentTittel
    }
}
