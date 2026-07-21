package no.nav.melosys.service.sak

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusRequest
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusResultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class SkjemaSaksstatusSyncServiceTest {

    @MockK
    lateinit var skjemaSakMappingRepository: SkjemaSakMappingRepository

    @MockK
    lateinit var melosysSkjemaApiClient: MelosysSkjemaApiClient

    private lateinit var service: SkjemaSaksstatusSyncService

    @BeforeEach
    fun setup() {
        service = SkjemaSaksstatusSyncService(skjemaSakMappingRepository, melosysSkjemaApiClient)
    }

    private fun lagFagsak(saksnummer: String = "MEL-100", status: Saksstatuser = Saksstatuser.OPPRETTET): Fagsak =
        Fagsak.forTest {
            this.saksnummer = saksnummer
            this.status = status
        }

    private fun lagMapping(fagsak: Fagsak, skjemaId: UUID = UUID.randomUUID()): SkjemaSakMapping =
        SkjemaSakMapping(
            skjemaId = skjemaId,
            fagsak = fagsak,
            mottatteOpplysninger = null,
            originalData = "{}"
        )

    @Nested
    inner class Statusmapping {

        @Test
        fun `OPPRETTET mappes til MOTTATT`() {
            SkjemaSaksstatusSyncService.tilSkjemaSaksstatus(Saksstatuser.OPPRETTET) shouldBe Saksstatus.MOTTATT
        }

        @ParameterizedTest
        @EnumSource(Saksstatuser::class, names = ["OPPRETTET"], mode = EnumSource.Mode.EXCLUDE)
        fun `alle andre statuser mappes til AVSLUTTET`(saksstatus: Saksstatuser) {
            SkjemaSaksstatusSyncService.tilSkjemaSaksstatus(saksstatus) shouldBe Saksstatus.AVSLUTTET
        }
    }

    @Nested
    inner class SynkroniserSaksstatusForFagsak {

        @Test
        fun `sak uten mapping-rad gir ingen kall til skjema-api`() {
            val fagsak = lagFagsak()
            every { skjemaSakMappingRepository.findByFagsak_Saksnummer(fagsak.saksnummer) } returns emptyList()

            service.synkroniserSaksstatusForFagsak(fagsak)

            verify(exactly = 0) { melosysSkjemaApiClient.oppdaterSaksstatus(any(), any(), any()) }
            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }

        @Test
        fun `en mapping-rad gir enkeltkall med mappet status`() {
            val fagsak = lagFagsak(status = Saksstatuser.LOVVALG_AVKLART)
            val skjemaId = UUID.randomUUID()
            every { skjemaSakMappingRepository.findByFagsak_Saksnummer(fagsak.saksnummer) } returns
                listOf(lagMapping(fagsak, skjemaId))
            every { melosysSkjemaApiClient.oppdaterSaksstatus(any(), any(), any()) } returns Unit

            service.synkroniserSaksstatusForFagsak(fagsak)

            verify(exactly = 1) {
                melosysSkjemaApiClient.oppdaterSaksstatus(skjemaId, fagsak.saksnummer, Saksstatus.AVSLUTTET)
            }
            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }

        @Test
        fun `flere mapping-rader gir bulk-kall med alle skjemaId-ene`() {
            val fagsak = lagFagsak(status = Saksstatuser.OPPRETTET)
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()
            every { skjemaSakMappingRepository.findByFagsak_Saksnummer(fagsak.saksnummer) } returns
                listOf(lagMapping(fagsak, skjemaId1), lagMapping(fagsak, skjemaId2))

            val requestSlot = mutableListOf<BulkOppdaterSaksstatusRequest>()
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(capture(requestSlot)) } returns
                BulkOppdaterSaksstatusResultat(2, emptyList())

            service.synkroniserSaksstatusForFagsak(fagsak)

            verify(exactly = 0) { melosysSkjemaApiClient.oppdaterSaksstatus(any(), any(), any()) }
            requestSlot.size shouldBe 1
            requestSlot.first().oppdateringer.map { it.skjemaId } shouldContainExactlyInAnyOrder listOf(skjemaId1, skjemaId2)
            requestSlot.first().oppdateringer.forEach {
                it.saksnummer shouldBe fagsak.saksnummer
                it.saksstatus shouldBe Saksstatus.MOTTATT
            }
        }
    }

    @Nested
    inner class Massesynk {

        @Test
        fun `dry-run bygger rapport uten kall til skjema-api`() {
            val opprettetSak = lagFagsak(saksnummer = "MEL-1", status = Saksstatuser.OPPRETTET)
            val avsluttetSak = lagFagsak(saksnummer = "MEL-2", status = Saksstatuser.AVSLUTTET)
            val henlagtSak = lagFagsak(saksnummer = "MEL-3", status = Saksstatuser.HENLAGT)
            every { skjemaSakMappingRepository.findAllMedFagsak() } returns listOf(
                lagMapping(opprettetSak),
                lagMapping(avsluttetSak),
                lagMapping(avsluttetSak),
                lagMapping(henlagtSak)
            )

            val rapport = service.massesynk(dryRun = true)

            rapport.dryRun shouldBe true
            rapport.antallTotalt shouldBe 4
            rapport.antallMottatt shouldBe 1
            rapport.antallAvsluttet shouldBe 3
            rapport.perMelosysStatus shouldBe mapOf(
                Saksstatuser.OPPRETTET to 1,
                Saksstatuser.AVSLUTTET to 2,
                Saksstatuser.HENLAGT to 1
            )
            rapport.antallOppdatert shouldBe null
            rapport.ukjenteSkjemaIder shouldBe null

            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
            verify(exactly = 0) { melosysSkjemaApiClient.oppdaterSaksstatus(any(), any(), any()) }
        }

        @Test
        fun `reell synk sender bulk-kall og aggregerer resultat i rapporten`() {
            val fagsak = lagFagsak(saksnummer = "MEL-1", status = Saksstatuser.OPPRETTET)
            val ukjentSkjemaId = UUID.randomUUID()
            every { skjemaSakMappingRepository.findAllMedFagsak() } returns listOf(
                lagMapping(fagsak),
                lagMapping(fagsak, ukjentSkjemaId)
            )
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) } returns
                BulkOppdaterSaksstatusResultat(3, listOf(ukjentSkjemaId))

            val rapport = service.massesynk(dryRun = false)

            rapport.dryRun shouldBe false
            rapport.antallTotalt shouldBe 2
            rapport.antallOppdatert shouldBe 3
            rapport.ukjenteSkjemaIder shouldContainExactly listOf(ukjentSkjemaId)

            verify(exactly = 1) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }

        @Test
        fun `over 1000 rader batches i flere bulk-kall`() {
            val fagsak = lagFagsak(saksnummer = "MEL-1", status = Saksstatuser.AVSLUTTET)
            val mappinger = (1..1500).map { lagMapping(fagsak) }
            every { skjemaSakMappingRepository.findAllMedFagsak() } returns mappinger

            val requests = mutableListOf<BulkOppdaterSaksstatusRequest>()
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(capture(requests)) } answers {
                BulkOppdaterSaksstatusResultat(firstArg<BulkOppdaterSaksstatusRequest>().oppdateringer.size, emptyList())
            }

            val rapport = service.massesynk(dryRun = false)

            requests.map { it.oppdateringer.size } shouldContainExactly listOf(1000, 500)
            rapport.antallTotalt shouldBe 1500
            rapport.antallOppdatert shouldBe 1500
            rapport.ukjenteSkjemaIder shouldBe emptyList()
        }

        @Test
        fun `ingen mapping-rader gir tom rapport`() {
            every { skjemaSakMappingRepository.findAllMedFagsak() } returns emptyList()

            val rapport = service.massesynk(dryRun = false)

            rapport.antallTotalt shouldBe 0
            rapport.antallOppdatert shouldBe 0
            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }
    }
}
