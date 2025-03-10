package no.nav.melosys.service.ftrl

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import java.util.*
import kotlin.test.Test

class FinnSakerForÅrsavregningTest {
    private lateinit var repository: SakerForÅrsavregningRepository
    private lateinit var kafkaProducer: KafkaMelosysHendelseProducer
    private lateinit var persondataService: PersondataService
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    private lateinit var finnSaker: FinnSakerForÅrsavregning

    @BeforeEach
    fun setUp() {
        repository = mockk()
        kafkaProducer = mockk(relaxed = true)
        persondataService = mockk()
        behandlingsresultatService = mockk()
        finnSaker = FinnSakerForÅrsavregning(
            repository,
            kafkaProducer,
            persondataService,
            behandlingsresultatService
        )
    }

    @Test
    @Disabled
    fun `finnFolkeregisteridentMedBehandlinger returns expected pairs`() {
        // Given
        val fakeIdent = "12345678901"
        val fakeAktørId = "aktør1"


        val fagsak = FagsakTestFactory.builder().build()

        every { repository.finnFagsaker(any(), any(), any(), any()) } returns listOf(fagsak)
        every { persondataService.finnFolkeregisterident(fakeAktørId) } returns Optional.of(fakeIdent)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns mockk(relaxed = true    )

        // When
        val result = finnSaker.finnFolkeregisteridentMedBehandlinger()

        // Then
//        result.size shouldBe 1
        result.first().first shouldBe fakeIdent
    }

//    @Test
//    fun `finnSakerOgLeggPåKø does not produce bestillingsmelding in dryrun mode`() {
//        // Given
//        val fakeIdent = "12345678901"
//        val fakeAktørId = "aktør1"
//
//        val behandling = Behandling(
//            id = 1,
//            fagsak = Fagsak(
//                saksnummer = "saks1",
//                type = "type1",
//                tema = "tema1",
//                hentBrukersAktørID = { fakeAktørId },
//                behandlinger = emptyList() // will be replaced below
//            )
//        )
//        val fagsak = behandling.fagsak.copy(behandlinger = listOf(behandling))
//
//        every { repository.finnFagsaker(any(), any(), any(), any()) } returns listOf(fagsak)
//        every { persondataService.finnFolkeregisterident(fakeAktørId) } returns Result.success(fakeIdent)
//
//        val dummyResultat = Behandlingsresultat(
//            id = 1,
//            type = "resultatType",
//            vedtakMetadata = VedtakMetadata(vedtakstype = "vedtakstype"),
//            medlemskapsperioder = listOf(
//                Medlemskapsperiode(
//                    fom = LocalDate.of(2023, 1, 1),
//                    tom = LocalDate.of(2023, 12, 31),
//                    innvilgelsesresultat = "JA"
//                )
//            )
//        )
//        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns dummyResultat
//
//        // When – dryrun should skip sending bestillingsmelding.
//        finnSaker.finnSakerOgLeggPåKø(dryrun = true)
//
//        // Then – verify that the Kafka producer was never called.
//        verify(exactly = 0) { kafkaProducer.produserBestillingsmelding(any()) }
//    }
}
