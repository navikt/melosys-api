package no.nav.melosys.integrasjon.ereg

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.exception.IkkeFunnetException
import java.util.*

/**
 * Fasade mot Enhetsregisteret (EREG)
 */
interface EregFasade {

    @Throws(IkkeFunnetException::class)
    fun hentOrganisasjon(orgnr: String): Saksopplysning

    fun finnOrganisasjon(orgnr: String): Optional<Saksopplysning>

    @Throws(IkkeFunnetException::class)
    fun hentOrganisasjonNavn(orgnummer: String): String
}
