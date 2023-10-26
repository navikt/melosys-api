package no.nav.melosys.service.sak

import no.nav.melosys.saksflytapi.journalfoering.Søknad
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto

class SøknadDto {
    @JvmField
    var periode: PeriodeDto? = null

    @JvmField
    var land: SoeknadslandDto? = null

    fun tilSøknadRequest(): Søknad {
        val tilPeriode = periode?.tilPeriode()
        val tilSoknadsland = land?.tilSoknadsland()
        return Søknad().apply {
            this.periode = tilPeriode
            this.land = tilSoknadsland
        }
    }
}

