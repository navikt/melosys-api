package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.stereotype.Component

/**
 * Utgått: brukes kun av prosessinstanser av typen MELOSYS_MOTTAK_DIGITAL_SØKNAD_NY_SAK.
 * Nye prosessinstanser bruker AvklarSakOgBehandlingDigitalSøknad.
 */
@Component
class OpprettSakOgBehandlingDigitalSøknad(
    private val nySakOppretter: DigitalSøknadNySakOppretter
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) = nySakOppretter.opprett(prosessinstans)
}
