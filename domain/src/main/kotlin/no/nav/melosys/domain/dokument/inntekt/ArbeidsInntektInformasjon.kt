package no.nav.melosys.domain.dokument.inntekt

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class ArbeidsInntektInformasjon(
    var inntektListe: List<Inntekt> = listOf(),
    @JsonProperty("arbeidsforholdFrilanserListe") var arbeidsforholdListe: List<ArbeidsforholdFrilanser>? = emptyList()
) {
    @JsonIgnore
    fun getMutableArbeidsforholdListe() : MutableList<ArbeidsforholdFrilanser>? {
        return arbeidsforholdListe?.toMutableList()
    }
}
