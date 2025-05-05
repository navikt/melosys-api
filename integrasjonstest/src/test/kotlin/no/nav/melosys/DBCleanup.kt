package no.nav.melosys

import io.kotest.matchers.optional.shouldBePresent
import mu.KotlinLogging
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.repository.*
import no.nav.melosys.saksflyt.ProsessinstansRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

@Component
class DBCleanup(
    private val fagsakRepository: FagsakRepository,
    private val avklarteFaktaRepository: AvklarteFaktaRepository,
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsResultRepository: BehandlingsresultatRepository,
    private val prosessinstansRepository: ProsessinstansRepository,
    private val behandlingRepository: BehandlingRepository,
    private val medlemskapsperiodeRepository: MedlemskapsperiodeRepositoryForTest
) {

    fun slettSakMedAvhengigheter(saksnummer: String) {
        fagsakRepository.findBySaksnummer(saksnummer).shouldBePresent()
            .also { fagsak ->
                fagsak.behandlinger.forEach { behandling ->
                    aarsavregningRepository.findByBehandlingsresultatId(behandling.id)?.let {
                        aarsavregningRepository.delete(it)
                    }
                }
            }.also { fagsak ->
                fagsak.behandlinger.forEach { behandling ->
                    avklarteFaktaRepository.findByBehandlingsresultatId(behandling.id).forEach {
                        avklarteFaktaRepository.delete(it)
                    }
                    medlemskapsperiodeRepository.findByBehandlingsresultatId(behandling.id).forEach {
                        medlemskapsperiodeRepository.delete(it)
                    }
                    behandlingsResultRepository.findById(behandling.id).shouldBePresent().also {
                        behandlingsResultRepository.delete(it)
                    }
                    prosessinstansRepository.findAll().filter { it?.behandling?.id == behandling.id }.forEach { prosessinstansRepository.delete(it) }
                    behandlingRepository.delete(behandling)
                }
            }.also { fagsakRepository.delete(it) }
    }

    fun slettProsessinstans(id: UUID) {
        prosessinstansRepository.findById(id).getOrNull()?.let {
            prosessinstansRepository.delete(it)
        } ?: log.warn { "Fant ikke prosessinstans med id: $id" }
    }
}

interface MedlemskapsperiodeRepositoryForTest : JpaRepository<Medlemskapsperiode, Long> {
    fun findByBehandlingsresultatId(id: Long): List<Medlemskapsperiode>
}
