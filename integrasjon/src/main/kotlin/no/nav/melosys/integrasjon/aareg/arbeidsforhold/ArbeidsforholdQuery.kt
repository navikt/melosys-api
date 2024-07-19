package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import java.time.LocalDate

data class ArbeidsforholdQuery(
    val regelverk: Regelverk = Regelverk.ALLE,
    val arbeidsforholdType: ArbeidsforholdType = ArbeidsforholdType.ALLE,
    // Filter for fra-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    val ansettelsesperiodeFom: LocalDate? = null,
    // Filter for til-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    val ansettelsesperiodeTom: LocalDate? = null
)  {
    enum class Regelverk {
        ALLE, A_ORDNINGEN, FOER_A_ORDNINGEN
    }

    enum class ArbeidsforholdType(internal val queryParam: String?) {
        FORENKLET_OPPGJOERSORDNING("forenkletOppgjoersordning"),
        FRILANSER_OPPDRAGSTAKER_HONORARPE_RSONER_MM("frilanserOppdragstakerHonorarPersonerMm"),
        MARITIMT_ARBEIDSFORHOLD("maritimtArbeidsforhold"),
        ORDINAERT_ARBEIDSFORHOLD("ordinaertArbeidsforhold"),
        ALLE(null);
    }
}
