package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.stereotype.Component

/**
 * Utgått: brukes kun av prosessinstanser av typen MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD.
 * Nye prosessinstanser bruker AvklarSakOgBehandlingDigitalSøknad.
 */
@Component
class HåndterEksisterendeSakDigitalSøknad(
    private val eksisterendeSakHåndterer: DigitalSøknadEksisterendeSakHåndterer
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) = eksisterendeSakHåndterer.håndter(prosessinstans)
}
