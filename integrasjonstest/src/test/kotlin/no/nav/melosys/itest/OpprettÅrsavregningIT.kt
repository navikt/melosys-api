package no.nav.melosys.itest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.Test

class OpprettÅrsavregningIT @Autowired constructor(
    private val årsavregningService: ÅrsavregningService,
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
    fun `opprettNyÅrsavregning skal kaste feil når man prøver å endre samme behandling til samme år som allerede er lagret`() {
        val behandlingsresultat = lagBehandlingsResultat()

        årsavregningService.opprettÅrsavregning(behandlingsresultat.id, 2024)

        shouldThrow<FunksjonellException> {
            årsavregningService.opprettÅrsavregning(behandlingsresultat.id, 2024)
        }.message shouldBe "Året 2024 er allerede lagret på denne årsavregningen"
    }

    @Test
    @Transactional
    fun `opprettNyÅrsavregning skal kaste feil når man prøver å endre en annen behandling til et år som allerede finnes på en aktiv behandling`() {
        val behandlingsresultat1 = lagBehandlingsResultat()
        val behandlingsresultat2 = lagBehandlingsResultat()

        årsavregningService.opprettÅrsavregning(behandlingsresultat1.id, 2024)

        shouldThrow<FunksjonellException> {
            årsavregningService.opprettÅrsavregning(behandlingsresultat2.id, 2024)
        }.message shouldBe "Det finnes en annen åpen årsavregningsbehandling for samme år på saken. " +
            "Vurder hvilken behandling du vil fortsette med og avslutt den som ikke er aktuell via behandlingsmenyen."
    }

    @Test
    @Transactional
    fun `opprettNyÅrsavregning skal feile dersom man prøver å endre til for gammelt år`() {
        val behandlingsresultat = lagBehandlingsResultat()

        shouldThrow<FunksjonellException> {
            // i fjor - 7 år (som er ett år eldre enn det vi skal støtte)
            årsavregningService.opprettÅrsavregning(behandlingsresultat.id, LocalDate.now().year - 8)
        }.message shouldBe "Årsavregning kan ikke opprettes for år eldre enn 6 år før inneværende år."
    }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        val eksempelFagsak = Fagsak(
            saksnummer = "123456",
            type = Sakstyper.FTRL,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            status = Saksstatuser.OPPRETTET
        )
        fagsakRepository.save(eksempelFagsak)

        val eksempelBehandling = Behandling.forTest {
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
