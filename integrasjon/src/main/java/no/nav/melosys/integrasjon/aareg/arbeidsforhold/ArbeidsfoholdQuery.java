package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.util.Optional;

public class ArbeidsfoholdQuery {
    // Filter for regelverk (default = ALLE)
    // Available values : ALLE, A_ORDNINGEN, FOER_A_ORDNINGEN
    private String regelverk;

    // Filter for regelverk (default = ALLE)
    // Available values : forenkletOppgjoersordning, frilanserOppdragstakerHonorarPersonerMm, maritimtArbeidsforhold, ordinaertArbeidsforhold
    private String arbeidsforholdType;

    // Filter for fra-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    private String ansettelsesperiodeFom;

    // Filter for til-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    private String ansettelsesperiodeTom;

    // Skal historikk inkluderes i respons? (default = false)
    private Boolean historikk;

    // Skal sporingsinformasjon inkluderes i respons? (default = true)
    private Boolean sporingsinformasjon;

    private ArbeidsfoholdQuery() {
    }

    public String getRegelverk() {
        return regelverk;
    }

    public Optional<String> getArbeidsforholdType() {
        if(arbeidsforholdType==null) {
            return Optional.empty();
        }
        return Optional.of(arbeidsforholdType);
    }

    public String getAnsettelsesperiodeFom() {
        return ansettelsesperiodeFom;
    }

    public String getAnsettelsesperiodeTom() {
        return ansettelsesperiodeTom;
    }

    public Boolean getHistorikk() {
        return historikk;
    }

    public Boolean getSporingsinformasjon() {
        return sporingsinformasjon;
    }

    public enum Regelverk {
        ALLE, A_ORDNINGEN, FOER_A_ORDNINGEN
    }

    public enum ArbeidsforholdType {
        FORENKLET_OPPGJOERSORDNING("forenkletOppgjoersordning"),
        FRILANSER_OPPDRAGSTAKER_HONORARPE_RSONER_MM("frilanserOppdragstakerHonorarPersonerMm"),
        MARITIMT_ARBEIDSFORHOLD("maritimtArbeidsforhold"),
        ORDINAERT_ARBEIDSFORHOLD("ordinaertArbeidsforhold"),
        ALLE(null);

        private final String queryParam;

        ArbeidsforholdType(String queryParam) {
            this.queryParam = queryParam;
        }

        @Override
        public String toString() {
            return queryParam;
        }
    }

    static public class Builder {
        private Regelverk regelverk = Regelverk.ALLE; // set til default
        private ArbeidsforholdType arbeidsforholdType = ArbeidsforholdType.ALLE;
        private String ansettelsesperiodeFom;
        private String ansettelsesperiodeTom;
        private Boolean historikk;
        private Boolean sporingsinformasjon;

        /**
         * Filter for regelverk (default = ALLE)
         * @param regelverk
         * @return
         */
        public Builder regelverk(Regelverk regelverk) {
            this.regelverk = regelverk;
            return this;
        }

        /***
         *  Filter for regelverk (default = ALLE)
         * @param arbeidsforholdType
         * @return
         */
        public Builder arbeidsforholdType(ArbeidsforholdType arbeidsforholdType) {
            this.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        public ArbeidsfoholdQuery build() {
            ArbeidsfoholdQuery arbeidsfoholdQuery = new ArbeidsfoholdQuery();
            arbeidsfoholdQuery.regelverk = regelverk.toString();
            arbeidsfoholdQuery.arbeidsforholdType = arbeidsforholdType.toString();
            return arbeidsfoholdQuery;
        }
    }
}
