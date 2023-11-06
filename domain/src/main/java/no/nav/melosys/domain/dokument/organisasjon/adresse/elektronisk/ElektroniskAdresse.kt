package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk

import no.nav.melosys.domain.dokument.felles.Periode

abstract class ElektroniskAdresse {
    private var bruksperiode: Periode? = null
    private var gyldighetsperiode: Periode? = null
    fun getBruksperiode(): Periode? {
        return bruksperiode
    }

    fun setBruksperiode(bruksperiode: Periode?) {
        this.bruksperiode = bruksperiode
    }

    fun getGyldighetsperiode(): Periode? {
        return gyldighetsperiode
    }

    fun setGyldighetsperiode(gyldighetsperiode: Periode?) {
        this.gyldighetsperiode = gyldighetsperiode
    }
}
