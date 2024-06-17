package no.nav.melosys

import io.kotest.matchers.optional.shouldBePresent
import no.nav.melosys.repository.AvklarteFaktaRepository
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import org.springframework.stereotype.Component

@Component
class DBCleanup(
    private val fagsakRepository: FagsakRepository,
    private val avklarteFaktaRepository: AvklarteFaktaRepository,
    private val behandlingsResultRepository: BehandlingsresultatRepository,
    private val prosessinstansRepository: ProsessinstansRepository,
    private val behandlingRepository: BehandlingRepository
    ) {

    fun slettSakMedAvhengigheter(saksnummer: String) {
        fagsakRepository.findBySaksnummer(saksnummer).shouldBePresent().also { fagsak ->
            fagsak.behandlinger.forEach { behandling ->
                avklarteFaktaRepository.findByBehandlingsresultatId(behandling.id).forEach {
                    avklarteFaktaRepository.delete(it)
                }
                behandlingsResultRepository.findById(behandling.id).shouldBePresent().also {
                    behandlingsResultRepository.delete(it)
                }
                prosessinstansRepository.findAll()
                    .filter { it?.behandling?.id == behandling.id }
                    .forEach { prosessinstansRepository.delete(it) }
                behandlingRepository.delete(behandling)
            }
            fagsakRepository.delete(fagsak)
        }
    }
}
