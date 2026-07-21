package no.nav.melosys.service.sak

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository.SaksstatusSynkRad
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusRequest
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusResultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class SkjemaSaksstatusSyncServiceTest {

    @MockK
    lateinit var skjemaSakMappingRepository: SkjemaSakMappingRepository

    @MockK
    lateinit var fagsakRepository: FagsakRepository

    @MockK
    lateinit var melosysSkjemaApiClient: MelosysSkjemaApiClient

    private lateinit var service: SkjemaSaksstatusSyncService

    @BeforeEach
    fun setup() {
        service = SkjemaSaksstatusSyncService(skjemaSakMappingRepository, fagsakRepository, melosysSkjemaApiClient)
    }

    private fun lagSynkRad(
        saksnummer: String,
        status: Saksstatuser,
        skjemaId: UUID = UUID.randomUUID()
    ): SaksstatusSynkRad = object : SaksstatusSynkRad {
        override fun getSkjemaId(): UUID = skjemaId
        override fun getSaksnummer(): String = saksnummer
        override fun getSaksstatus(): Saksstatuser = status
    }

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
    inner class SynkroniserSaksstatusForSaksnummer {

        private val saksnummer = "MEL-100"

        @Test
        fun `sak uten mapping-rad gir ingen kall til skjema-api`() {
            every { skjemaSakMappingRepository.existsByFagsak_Saksnummer(saksnummer) } returns false

            service.synkroniserSaksstatusForSaksnummer(saksnummer)

            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
            verify(exactly = 0) { fagsakRepository.finnStatusForSaksnummer(any()) }
        }

        @Test
        fun `henlagt sak med mapping gir bulk-kall med AVSLUTTET og gjeldende status fra slankt oppslag`() {
            val skjemaId = UUID.randomUUID()
            every { skjemaSakMappingRepository.existsByFagsak_Saksnummer(saksnummer) } returns true
            every { fagsakRepository.finnStatusForSaksnummer(saksnummer) } returns
                Optional.of(Saksstatuser.HENLAGT_BORTFALT)
            every { skjemaSakMappingRepository.finnSkjemaIderForSaksnummer(saksnummer) } returns listOf(skjemaId)

            val requests = mutableListOf<BulkOppdaterSaksstatusRequest>()
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(capture(requests)) } returns
                BulkOppdaterSaksstatusResultat(1, emptyList())

            service.synkroniserSaksstatusForSaksnummer(saksnummer)

            requests.size shouldBe 1
            requests.first().oppdateringer.single().let {
                it.skjemaId shouldBe skjemaId
                it.saksnummer shouldBe saksnummer
                it.saksstatus shouldBe Saksstatus.AVSLUTTET
            }
        }

        @Test
        fun `flere mapping-rader gir ett bulk-kall med alle skjemaId-ene og mappet status`() {
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()
            every { skjemaSakMappingRepository.existsByFagsak_Saksnummer(saksnummer) } returns true
            every { fagsakRepository.finnStatusForSaksnummer(saksnummer) } returns Optional.of(Saksstatuser.OPPRETTET)
            every { skjemaSakMappingRepository.finnSkjemaIderForSaksnummer(saksnummer) } returns
                listOf(skjemaId1, skjemaId2)

            val requests = mutableListOf<BulkOppdaterSaksstatusRequest>()
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(capture(requests)) } returns
                BulkOppdaterSaksstatusResultat(2, emptyList())

            service.synkroniserSaksstatusForSaksnummer(saksnummer)

            requests.size shouldBe 1
            requests.first().oppdateringer.map { it.skjemaId } shouldContainExactly listOf(skjemaId1, skjemaId2)
            requests.first().oppdateringer.forEach {
                it.saksnummer shouldBe saksnummer
                it.saksstatus shouldBe Saksstatus.MOTTATT
            }
        }

        @Test
        fun `over 1000 mapping-rader batches i flere bulk-kall`() {
            every { skjemaSakMappingRepository.existsByFagsak_Saksnummer(saksnummer) } returns true
            every { fagsakRepository.finnStatusForSaksnummer(saksnummer) } returns Optional.of(Saksstatuser.AVSLUTTET)
            every { skjemaSakMappingRepository.finnSkjemaIderForSaksnummer(saksnummer) } returns
                (1..1500).map { UUID.randomUUID() }

            val requests = mutableListOf<BulkOppdaterSaksstatusRequest>()
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(capture(requests)) } answers {
                BulkOppdaterSaksstatusResultat(firstArg<BulkOppdaterSaksstatusRequest>().oppdateringer.size, emptyList())
            }

            service.synkroniserSaksstatusForSaksnummer(saksnummer)

            requests.map { it.oppdateringer.size } shouldContainExactly listOf(1000, 500)
        }
    }

    @Nested
    inner class Massesynk {

        @Test
        fun `dry-run bygger rapport uten kall til skjema-api`() {
            every { skjemaSakMappingRepository.finnAlleSaksstatusSynkRader() } returns listOf(
                lagSynkRad("MEL-1", Saksstatuser.OPPRETTET),
                lagSynkRad("MEL-2", Saksstatuser.AVSLUTTET),
                lagSynkRad("MEL-2", Saksstatuser.AVSLUTTET),
                lagSynkRad("MEL-3", Saksstatuser.HENLAGT)
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
            rapport.feiletBatch shouldBe null
            rapport.feilmelding shouldBe null

            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }

        @Test
        fun `reell synk sender bulk-kall og aggregerer resultat i rapporten`() {
            val ukjentSkjemaId = UUID.randomUUID()
            every { skjemaSakMappingRepository.finnAlleSaksstatusSynkRader() } returns listOf(
                lagSynkRad("MEL-1", Saksstatuser.OPPRETTET),
                lagSynkRad("MEL-1", Saksstatuser.OPPRETTET, ukjentSkjemaId)
            )
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) } returns
                BulkOppdaterSaksstatusResultat(3, listOf(ukjentSkjemaId))

            val rapport = service.massesynk(dryRun = false)

            rapport.dryRun shouldBe false
            rapport.antallTotalt shouldBe 2
            rapport.antallOppdatert shouldBe 3
            rapport.ukjenteSkjemaIder shouldContainExactly listOf(ukjentSkjemaId)
            rapport.feiletBatch shouldBe null

            verify(exactly = 1) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }

        @Test
        fun `over 1000 rader batches i flere bulk-kall`() {
            every { skjemaSakMappingRepository.finnAlleSaksstatusSynkRader() } returns
                (1..1500).map { lagSynkRad("MEL-1", Saksstatuser.AVSLUTTET) }

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
        fun `feilet batch stopper videre sending og gir delrapport med tallene saa langt`() {
            every { skjemaSakMappingRepository.finnAlleSaksstatusSynkRader() } returns
                (1..2500).map { lagSynkRad("MEL-1", Saksstatuser.OPPRETTET) }

            var kall = 0
            every { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) } answers {
                kall++
                if (kall == 2) {
                    throw RuntimeException("skjema-api er nede")
                }
                BulkOppdaterSaksstatusResultat(1000, emptyList())
            }

            val rapport = service.massesynk(dryRun = false)

            // Batch 1 gikk gjennom, batch 2 feilet, batch 3 skal ikke sendes
            verify(exactly = 2) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
            rapport.antallTotalt shouldBe 2500
            rapport.antallOppdatert shouldBe 1000
            rapport.feiletBatch shouldBe 2
            rapport.feilmelding!!.contains("Batch 2 av 3") shouldBe true
            rapport.feilmelding!!.contains("skjema-api er nede") shouldBe false
        }

        @Test
        fun `ingen mapping-rader gir tom rapport`() {
            every { skjemaSakMappingRepository.finnAlleSaksstatusSynkRader() } returns emptyList()

            val rapport = service.massesynk(dryRun = false)

            rapport.antallTotalt shouldBe 0
            rapport.antallOppdatert shouldBe 0
            verify(exactly = 0) { melosysSkjemaApiClient.bulkOppdaterSaksstatus(any()) }
        }
    }
}
