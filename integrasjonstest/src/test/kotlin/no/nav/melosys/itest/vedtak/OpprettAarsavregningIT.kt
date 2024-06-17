package no.nav.melosys.itest.vedtak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.itest.ComponentTestBase
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
class OpprettAarsavregningIT : ComponentTestBase() {

    @Autowired
    lateinit var årsavregningService: ÅrsavregningService

    @Autowired
    lateinit var aarsavregningRepository: AarsavregningRepository

    @Autowired
    lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @AfterEach
    fun cleanup() {
        aarsavregningRepository.deleteAll()
        behandlingsresultatRepository.deleteAll()
        behandlingRepository.deleteAll()
        fagsakRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `opprettNyÅrsavregning should create a new årsavregning when no existing avregning exists`() {
        // Given
        val fagsak = Fagsak(
            saksnummer = "123456",
            type = Sakstyper.FTRL,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            status = Saksstatuser.OPPRETTET
        )
        fagsakRepository.save(fagsak)

        val behandling = Behandling()
        behandling.fagsak = fagsak
        behandling.status = Behandlingsstatus.OPPRETTET
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.tema = Behandlingstema.YRKESAKTIV
        behandling.behandlingsfrist = LocalDate.now()

        behandlingRepository.save(behandling)

        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.behandling = behandling
        behandlingsresultat.behandlingsmåte = Behandlingsmaate.MANUELT
        behandlingRepository.save(behandling) // Save behandling with the mapped behandlingsresultat
        behandlingsresultatRepository.save(behandlingsresultat)

        // When
        val result = årsavregningService.opprettNyÅrsavregning(behandlingsresultat.id!!, 2024)

        // Then
        val avregning = aarsavregningRepository.findById(result).orElseThrow()
        assert(avregning.aar == 2024)
    }

    @Test
    @Transactional
    fun `opprettNyÅrsavregning should throw exception when existing avregning exists for the same year`() {
        // Given
        val fagsak = Fagsak(
            saksnummer = "123456",
            type = Sakstyper.FTRL,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            status = Saksstatuser.OPPRETTET
        )
        fagsakRepository.save(fagsak)

        val behandling = Behandling()
        behandling.fagsak = fagsak
        behandling.status = Behandlingsstatus.OPPRETTET
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.tema = Behandlingstema.YRKESAKTIV
        behandling.behandlingsfrist = LocalDate.now()

        behandlingRepository.save(behandling)

        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.behandling = behandling
        behandlingsresultat.behandlingsmåte = Behandlingsmaate.MANUELT
        behandlingRepository.save(behandling) // Save behandling with the mapped behandlingsresultat
        behandlingsresultatRepository.save(behandlingsresultat)

        val aarsavregning = Aarsavregning()
        aarsavregning.behandlingsresultat = behandlingsresultat
        aarsavregning.aar = 2024

        aarsavregningRepository.save(aarsavregning)

        // When & Then
        assertThrows(IllegalStateException::class.java) {
            årsavregningService.opprettNyÅrsavregning(behandlingsresultat.id!!, 2024)
        }
    }
}
