package no.nav.melosys.tjenester.gui.dto.brev

class FeltvalgAlternativDto {
    val kode: String?
    val beskrivelse: String?
    val isVisFelt: Boolean

    constructor(feltvalgAlternativKode: FeltvalgAlternativKode) {
        this.kode = feltvalgAlternativKode.kode
        this.beskrivelse = feltvalgAlternativKode.beskrivelse
        this.isVisFelt = false
    }

    constructor(feltvalgAlternativKode: FeltvalgAlternativKode, visFelt: Boolean) {
        this.kode = feltvalgAlternativKode.kode
        this.beskrivelse = feltvalgAlternativKode.beskrivelse
        this.isVisFelt = visFelt
    }

    constructor(kode: String?, beskrivelse: String?, visFelt: Boolean) {
        this.kode = kode
        this.beskrivelse = beskrivelse
        this.isVisFelt = visFelt
    }

    class Builder {
        private var kode: String? = null
        private var beskrivelse: String? = null
        private var visFelt = false

        fun medKode(kode: String?): Builder {
            this.kode = kode
            return this
        }

        fun medBeskrivelse(beskrivelse: String?): Builder {
            this.beskrivelse = beskrivelse
            return this
        }

        fun medVisFelt(visFelt: Boolean): Builder {
            this.visFelt = visFelt
            return this
        }

        fun build(): FeltvalgAlternativDto {
            return FeltvalgAlternativDto(kode, beskrivelse, visFelt)
        }
    }
}
