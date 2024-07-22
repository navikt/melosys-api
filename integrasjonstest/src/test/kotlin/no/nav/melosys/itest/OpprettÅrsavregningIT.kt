package no.nav.melosys.itest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.Test

@SpringBootTest
class OpprettÅrsavregningIT @Autowired constructor(
    private val årsavregningService: ÅrsavregningService,
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository
) : ComponentTestBase() {

    @Test
    @Transactional
    fun `opprettNyÅrsavregning skal lage ny årsavregning når det ikke finnes avregning`() {
        val behandlingsresultat = lagBehandlingsResultat()

        val result = årsavregningService.opprettÅrsavregning(behandlingsresultat.id!!, 2024)

        result.år shouldBe 2024
    }

    @Test
    @Transactional
    fun `opprettNyÅrsavregning should throw exception when existing avregning exists for the same year`() {
        val behandlingsresultat = lagBehandlingsResultat().shouldNotBeNull()

        //Oppretter først en aarsavregning for å simulere at det allerede finnes en årsavregning for samme år
        årsavregningService.opprettÅrsavregning(behandlingsresultat.id, 2024)

        shouldThrow<FunksjonellException> {
            årsavregningService.opprettÅrsavregning(behandlingsresultat.id, 2024)
        }
    }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        val eksempelFagsak = Fagsak(
            saksnummer = "123456",
            type = Sakstyper.FTRL,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            status = Saksstatuser.OPPRETTET
        )
        fagsakRepository.save(eksempelFagsak)

        val eksempelBehandling = Behandling().apply {
            fagsak = eksempelFagsak
            status = Behandlingsstatus.OPPRETTET
            type = Behandlingstyper.ÅRSAVREGNING
            tema = Behandlingstema.YRKESAKTIV
            behandlingsfrist = LocalDate.now()
        }

        behandlingRepository.save(eksempelBehandling)

        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = eksempelBehandling
            behandlingsmåte = Behandlingsmaate.MANUELT
        }
        behandlingsresultatRepository.save(behandlingsresultat)
        return behandlingsresultat
    }
}
