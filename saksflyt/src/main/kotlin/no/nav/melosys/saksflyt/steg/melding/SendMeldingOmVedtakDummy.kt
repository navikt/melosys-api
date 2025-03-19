package no.nav.melosys.saksflyt.steg.melding

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
@Profile("local-q1", "local-q2")
class SendMeldingOmVedtakDummy(
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_MELDING_OM_VEDTAK
    }

    override fun utfør(prosessinstans: Prosessinstans) {
       log.info { "Prossesteget ${ProsessSteg.SEND_MELDING_OM_VEDTAK} skal ikke eksekvere ved lokal kjøring mot q1/q2" }
    }
}
