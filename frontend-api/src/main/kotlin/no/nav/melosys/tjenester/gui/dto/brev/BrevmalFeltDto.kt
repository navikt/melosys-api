package no.nav.melosys.tjenester.gui.dto.brev

/**
 * Informasjon om et felt som skal være med i malen.
 *
 * Dersom {@param valg} ikke er null, vil instans av dette feltet være usynlig med mindre brukeren velger
 * et valgalternativ fra {@param valg} som har [FeltvalgAlternativDto.isVisFelt] = true.
 */
class BrevmalFeltDto private constructor(builder: Builder) {
    val kode: String?
    val beskrivelse: String?
    val feltType: FeltType?
    val hjelpetekst: String?
    val isPaakrevd: Boolean
    val valg: FeltValgDto?
    val tegnBegrensning: Int?

    init {
        this.kode = builder.kode
        this.beskrivelse = builder.beskrivelse
        this.feltType = builder.feltType
        this.hjelpetekst = builder.hjelpetekst
        this.isPaakrevd = builder.paakrevd
        this.valg = builder.valg
        this.tegnBegrensning = builder.tegnBegrensning
    }

    class Builder {
        internal var kode: String? = null
        var beskrivelse: String? = null
        var feltType: FeltType? = null
        var hjelpetekst: String? = null
        var paakrevd: Boolean = false
        var valg: FeltValgDto? = null
        var tegnBegrensning: Int? = null

        fun medKodeOgBeskrivelse(brevmalFeltKode: BrevmalFeltKode): Builder {
            this.kode = brevmalFeltKode.kode
            this.beskrivelse = brevmalFeltKode.beskrivelse
            return this
        }

        fun medFeltType(feltType: FeltType?): Builder {
            this.feltType = feltType
            return this
        }

        fun medHjelpetekst(hjelpetekst: String?): Builder {
            this.hjelpetekst = hjelpetekst
            return this
        }

        fun erPåkrevd(): Builder {
            this.paakrevd = true
            return this
        }

        fun medValg(valg: FeltValgDto?): Builder {
            this.valg = valg
            return this
        }

        fun medTegnBegrensning(antallTegn: Int?): Builder {
            this.tegnBegrensning = antallTegn
            return this
        }

        fun build(): BrevmalFeltDto {
            return BrevmalFeltDto(this)
        }
    }
}
