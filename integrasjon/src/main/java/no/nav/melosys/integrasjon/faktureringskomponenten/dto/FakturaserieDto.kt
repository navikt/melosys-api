package no.nav.melosys.integrasjon.faktureringskomponenten.dto

data class FakturaserieDto(
    val vedtaksId: String?,
    val fodselsnummer: String?,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,
    val referanseNAV: String?,
    val fakturaGjelder: String?,
    val intervall: FaktureringsIntervall? = FaktureringsIntervall.MANEDLIG,
    val perioder: List<FakturaseriePeriodeDto>?
) {
    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var vedtaksId: String? = null
        private var fodselsnummer: String? = null
        private var fullmektig: FullmektigDto? = null
        private var referanseBruker: String? = null
        private var referanseNAV: String? = null
        private var fakturaGjelder: String? = null
        private var intervall: FaktureringsIntervall? = FaktureringsIntervall.MANEDLIG
        private var perioder: List<FakturaseriePeriodeDto>? = listOf()

        fun medVedtaksId(vedtaksId: String?): Builder {
            this.vedtaksId = vedtaksId
            return this
        }

        fun medFodselsnummer(fodselsnummer: String?): Builder {
            this.fodselsnummer = fodselsnummer
            return this
        }

        fun medFullmektig(fullmektig: FullmektigDto?): Builder {
            this.fullmektig = fullmektig
            return this
        }

        fun medReferanseBruker(referanseBruker: String?): Builder {
            this.referanseBruker = referanseBruker
            return this
        }

        fun medReferanseNAV(referanseNAV: String?): Builder {
            this.referanseNAV = referanseNAV
            return this
        }

        fun medFakturaGjelder(fakturaGjelder: String?): Builder {
            this.fakturaGjelder = fakturaGjelder
            return this
        }

        fun medIntervall(intervall: FaktureringsIntervall?): Builder {
            this.intervall = intervall
            return this
        }

        fun medPerioder(perioder: List<FakturaseriePeriodeDto>?): Builder {
            this.perioder = perioder
            return this
        }


        fun build(): FakturaserieDto {
            return FakturaserieDto(
                vedtaksId,
                fodselsnummer,
                fullmektig,
                referanseBruker,
                referanseNAV,
                fakturaGjelder,
                intervall,
                perioder
            )
        }
    }
}
