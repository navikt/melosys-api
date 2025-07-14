package no.nav.melosys.saksflyt.steg.arsavregning

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Steg for å oppdatere åpne årsavregninger når en ny vurdering har blitt vedtatt.
 * Dette gjør at årsavregningene som er åpne får riktig grunnlag.
 */
@Component
class ResetÅpneÅrsavregningBehandlinger(
    private val årsavregningService: ÅrsavregningService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.RESET_ÅPNE_ÅRSAVREGNINGER
    }

    @Transactional
    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        if (!behandling.erNyVurdering()) {
            // Hvis det ikke er en ny vurdering, så skal vi ikke oppdatere åpne årsavregninger
            return
        }

        val saksnummer = behandling.fagsak.saksnummer

        val årsavregninger = årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, null, Behandlingsresultattyper.IKKE_FASTSATT)

        årsavregninger.forEach {
            årsavregningService.resetEksisterendeÅrsavregning(it.id)
        }
    }
}
