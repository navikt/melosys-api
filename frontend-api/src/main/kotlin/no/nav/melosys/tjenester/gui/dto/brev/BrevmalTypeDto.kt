package no.nav.melosys.tjenester.gui.dto.brev

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

class BrevmalTypeDto private constructor(val type: Produserbaredokumenter?, val felter: List<BrevmalFeltDto>?) {
    class Builder {
        private var type: Produserbaredokumenter? = null
        private var felter: List<BrevmalFeltDto>? = null

        fun medType(type: Produserbaredokumenter?): Builder {
            this.type = type
            return this
        }

        fun medFelter(felter: List<BrevmalFeltDto>?): Builder {
            this.felter = felter
            return this
        }

        fun build(): BrevmalTypeDto {
            return BrevmalTypeDto(type, felter)
        }
    }
}
