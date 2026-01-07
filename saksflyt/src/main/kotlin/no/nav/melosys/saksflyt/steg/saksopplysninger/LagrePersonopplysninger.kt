package no.nav.melosys.saksflyt.steg.saksopplysninger

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.saksopplysninger.PersonopplysningerLagrer
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som lagrer personopplysninger (PDL_PERSOPL, PDL_PERS_SAKS) på behandlingen.
 *
 * Dette steget håndterer IVERKSETTER_VEDTAK-status for å eliminere race conditions mellom
 * den synkrone event-listeneren og asynkron saga-kjøring.
 *
 * @see no.nav.melosys.service.saksopplysninger.SaksoppplysningEventListener
 * @see no.nav.melosys.service.saksopplysninger.PersonopplysningerLagrer
 */
@Component
class LagrePersonopplysninger(
    private val behandlingService: BehandlingService,
    private val personopplysningerLagrer: PersonopplysningerLagrer
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.LAGRE_PERSONOPPLYSNINGER

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandlingId = prosessinstans.hentBehandling.id
        log.debug { "Kjører LAGRE_PERSONOPPLYSNINGER for behandling $behandlingId" }

        // Last alltid behandling på nytt for å få oppdatert tilstand etter at HTTP-transaksjonen er committet
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        personopplysningerLagrer.lagreHvisMangler(behandling)
    }
}
