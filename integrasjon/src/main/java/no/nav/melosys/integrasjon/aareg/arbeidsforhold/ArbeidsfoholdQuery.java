package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

public class ArbeidsfoholdQuery {
    // Filter for regelverk (default = ALLE)
    // Available values : ALLE, A_ORDNINGEN, FOER_A_ORDNINGEN
    String regelverk;

    // Filter for regelverk (default = ALLE)
    // Available values : forenkletOppgjoersordning, frilanserOppdragstakerHonorarPersonerMm, maritimtArbeidsforhold, ordinaertArbeidsforhold
    String arbeidsforholdType;

    // Filter for fra-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    String ansettelsesperiodeFom;

    // Filter for til-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    String ansettelsesperiodeTom;

    // Skal historikk inkluderes i respons? (default = false)
    Boolean historikk;

    // Skal sporingsinformasjon inkluderes i respons? (default = true)
    Boolean sporingsinformasjon;
}
