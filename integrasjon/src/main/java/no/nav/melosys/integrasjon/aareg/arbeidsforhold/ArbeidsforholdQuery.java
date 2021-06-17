package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.time.LocalDate;
import java.util.Optional;

public class ArbeidsforholdQuery {
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

    private ArbeidsforholdQuery() {
    }

    public String getRegelverk() {
        return regelverk;
    }

    public Optional<String> getArbeidsforholdType() {
        return Optional.ofNullable(arbeidsforholdType);
    }

    public Optional<String> getAnsettelsesperiodeFom() {
        return Optional.ofNullable(ansettelsesperiodeFom);
    }

    public Optional<String> getAnsettelsesperiodeTom() {
        return Optional.ofNullable(ansettelsesperiodeTom);
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
        private LocalDate ansettelsesperiodeFom;
        private LocalDate ansettelsesperiodeTom;
        private Boolean historikk;
        private Boolean sporingsinformasjon;

        /**
         * @param regelverk Filter for regelverk (default = ALLE)
         * @return Builder
         */
        public Builder regelverk(Regelverk regelverk) {
            this.regelverk = regelverk;
            return this;
        }

        /***
         * @param arbeidsforholdType Filter for regelverk (default = ALLE)
         * @return Builder
         */
        public Builder arbeidsforholdType(ArbeidsforholdType arbeidsforholdType) {
            this.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        /***
         * @param ansettelsesperiodeFom
         * Filter for fra-og-med-dato for ansettelsesperiode
         * @return Builder
         */
        public Builder ansettelsesperiodeFom(LocalDate ansettelsesperiodeFom) {
            this.ansettelsesperiodeFom = ansettelsesperiodeFom;
            return this;
        }

        /***
         * @param ansettelsesperiodeTom Filter for regelverk (default = ALLE)
         * Filter for til-og-med-dato for ansettelsesperiode
         * @return Builder
         */
        public Builder ansettelsesperiodeTom(LocalDate ansettelsesperiodeTom) {
            this.ansettelsesperiodeTom = ansettelsesperiodeTom;
            return this;
        }

        public ArbeidsforholdQuery build() {
            ArbeidsforholdQuery arbeidsfoholdQuery = new ArbeidsforholdQuery();
            arbeidsfoholdQuery.regelverk = regelverk.toString();
            arbeidsfoholdQuery.arbeidsforholdType = arbeidsforholdType.toString();
            if (ansettelsesperiodeFom != null) {
                arbeidsfoholdQuery.ansettelsesperiodeFom = ansettelsesperiodeFom.toString();
            }
            if (ansettelsesperiodeTom != null) {
                arbeidsfoholdQuery.ansettelsesperiodeTom = ansettelsesperiodeTom.toString();
            }
            return arbeidsfoholdQuery;
        }
    }
}
