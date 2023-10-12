package no.nav.melosys.domain.dokument.inntekt

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class ArbeidsInntektInformasjon(
    @JvmField var inntektListe: List<Inntekt> = listOf(),
    @JvmField @JsonProperty("arbeidsforholdFrilanserListe") var arbeidsforholdListe: List<ArbeidsforholdFrilanser>? = emptyList()
) {
    @JsonIgnore
    fun getMutableInntektListe() : MutableList<Inntekt> {
        return inntektListe.toMutableList()
    }
    @JsonIgnore
    fun getMutableArbeidsforholdListe() : MutableList<ArbeidsforholdFrilanser>? {
        return arbeidsforholdListe?.toMutableList()
    }
}
