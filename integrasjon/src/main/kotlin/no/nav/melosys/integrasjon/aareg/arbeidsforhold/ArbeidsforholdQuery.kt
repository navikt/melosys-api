package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import java.time.LocalDate

class ArbeidsforholdQuery private constructor(builder: Builder) {
    // Filter for regelverk (default = ALLE)
    // Available values : ALLE, A_ORDNINGEN, FOER_A_ORDNINGEN
    val regelverk: String

    // Filter for regelverk (default = ALLE)
    // Available values : forenkletOppgjoersordning, frilanserOppdragstakerHonorarPersonerMm, maritimtArbeidsforhold, ordinaertArbeidsforhold
    val arbeidsforholdType: String?

    // Filter for fra-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    var ansettelsesperiodeFom: String?

    // Filter for til-og-med-dato for ansettelsesperiode, format (ISO-8601): yyyy-MM-dd
    var ansettelsesperiodeTom: String?

    init {
        this.regelverk = builder.regelverk.toString()
        this.arbeidsforholdType = builder.arbeidsforholdType.queryParam
        if (builder.ansettelsesperiodeFom != null) {
            this.ansettelsesperiodeFom = builder.ansettelsesperiodeFom.toString()
        } else {
            this.ansettelsesperiodeFom = null
        }
        if (builder.ansettelsesperiodeTom != null) {
            this.ansettelsesperiodeTom = builder.ansettelsesperiodeTom.toString()
        } else {
            this.ansettelsesperiodeTom = null
        }
    }

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

    class Builder {
        internal var regelverk: Regelverk = Regelverk.ALLE // set til default
        internal var arbeidsforholdType: ArbeidsforholdType = ArbeidsforholdType.ALLE
        internal var ansettelsesperiodeFom: LocalDate? = null
        internal var ansettelsesperiodeTom: LocalDate? = null

        /**
         * @param regelverk Filter for regelverk (default = ALLE)
         * @return Builder
         */
        fun regelverk(regelverk: Regelverk): Builder {
            this.regelverk = regelverk
            return this
        }

        /***
         * @param arbeidsforholdType Filter for regelverk (default = ALLE)
         * @return Builder
         */
        fun arbeidsforholdType(arbeidsforholdType: ArbeidsforholdType): Builder {
            this.arbeidsforholdType = arbeidsforholdType
            return this
        }

        /***
         * @param ansettelsesperiodeFom
         * Filter for fra-og-med-dato for ansettelsesperiode
         * @return Builder
         */
        fun ansettelsesperiodeFom(ansettelsesperiodeFom: LocalDate?): Builder {
            this.ansettelsesperiodeFom = ansettelsesperiodeFom
            return this
        }

        /***
         * @param ansettelsesperiodeTom Filter for regelverk (default = ALLE)
         * Filter for til-og-med-dato for ansettelsesperiode
         * @return Builder
         */
        fun ansettelsesperiodeTom(ansettelsesperiodeTom: LocalDate?): Builder {
            this.ansettelsesperiodeTom = ansettelsesperiodeTom
            return this
        }

        fun build(): ArbeidsforholdQuery {
            return ArbeidsforholdQuery(this)
        }
    }
}
