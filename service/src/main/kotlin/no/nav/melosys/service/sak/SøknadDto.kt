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
        return Søknad(periode?.tilPeriode(), land?.tilSoknadsland())
    }
}

