package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.time.LocalDate;
import java.util.Optional;

public class ArbeidsforholdQuery {
    // Filter for regelverk (default = ALLE)
    // Available values : ALLE, A_ORDNINGEN, FOER_A_ORDNINGEN
    private final String regelverk;

    // Filter for regelverk (default = ALLE)
    // Available values : forenkletOppgjoersordning, frilanserOppdragstakerHonorarPersonerMm, maritimtArbeidsforhold, ordinaertArbeidsforhold
    private final String arbeidsforholdType;

    // Filter for fra-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    private final String ansettelsesperiodeFom;

    // Filter for til-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    private final String ansettelsesperiodeTom;

    private ArbeidsforholdQuery(Builder builder) {
        this.regelverk = builder.regelverk.toString();
        this.arbeidsforholdType = builder.arbeidsforholdType.toString();
        if (builder.ansettelsesperiodeFom != null) {
            this.ansettelsesperiodeFom = builder.ansettelsesperiodeFom.toString();
        } else {
            this.ansettelsesperiodeFom = null;
        }
        if (builder.ansettelsesperiodeTom != null) {
            this.ansettelsesperiodeTom = builder.ansettelsesperiodeTom.toString();
        } else {
            this.ansettelsesperiodeTom = null;
        }
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

    public static class Builder {
        private Regelverk regelverk = Regelverk.ALLE; // set til default
        private ArbeidsforholdType arbeidsforholdType = ArbeidsforholdType.ALLE;
        private LocalDate ansettelsesperiodeFom;
        private LocalDate ansettelsesperiodeTom;

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
            return new ArbeidsforholdQuery(this);
        }
    }
}
