package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException
import java.util.*

@ExtendWith(MockKExtension::class)
internal class SkjemaSakMappingServiceTest {

    @MockK lateinit var skjemaSakMappingRepository: SkjemaSakMappingRepository
    @MockK lateinit var fagsakRepository: FagsakRepository

    private lateinit var service: SkjemaSakMappingService

    @BeforeEach
    fun setup() {
        service = SkjemaSakMappingService(skjemaSakMappingRepository, fagsakRepository)
    }

    @Nested
    inner class FinnSaksnummerForGyldigSak {

        @Test
        fun `tom skjemaIder returnerer null`() {
            service.finnGyldigSaksnummerForSkjemaIder(emptyList()).shouldBeNull()
        }

        @Test
        fun `ingen mappinger funnet returnerer null`() {
            val skjemaIder = listOf(UUID.randomUUID())
            every { skjemaSakMappingRepository.findBySkjemaIdIn(skjemaIder) } returns emptyList()

            service.finnGyldigSaksnummerForSkjemaIder(skjemaIder).shouldBeNull()
        }

        @Test
        fun `en mapping med sak OPPRETTET returnerer saksnummer`() {
            val skjemaId = UUID.randomUUID()
            val saksnummer = "MEL-100"
            val mapping = SkjemaSakMapping(skjemaId = skjemaId, fagsak = Fagsak.forTest { this.saksnummer = saksnummer })
            val fagsak = Fagsak.forTest { this.saksnummer = saksnummer; status = Saksstatuser.OPPRETTET }

            every { skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId)) } returns listOf(mapping)
            every { fagsakRepository.findAllBySaksnummerIn(listOf(saksnummer)) } returns listOf(fagsak)

            service.finnGyldigSaksnummerForSkjemaIder(listOf(skjemaId)) shouldBe saksnummer
        }

        @Test
        fun `en mapping med sak LOVVALG_AVKLART returnerer saksnummer`() {
            val skjemaId = UUID.randomUUID()
            val saksnummer = "MEL-200"
            val mapping = SkjemaSakMapping(skjemaId = skjemaId, fagsak = Fagsak.forTest { this.saksnummer = saksnummer })
            val fagsak = Fagsak.forTest { this.saksnummer = saksnummer; status = Saksstatuser.LOVVALG_AVKLART }

            every { skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId)) } returns listOf(mapping)
            every { fagsakRepository.findAllBySaksnummerIn(listOf(saksnummer)) } returns listOf(fagsak)

            service.finnGyldigSaksnummerForSkjemaIder(listOf(skjemaId)) shouldBe saksnummer
        }

        @Test
        fun `en mapping med sak AVSLUTTET returnerer null`() {
            val skjemaId = UUID.randomUUID()
            val saksnummer = "MEL-300"
            val mapping = SkjemaSakMapping(skjemaId = skjemaId, fagsak = Fagsak.forTest { this.saksnummer = saksnummer })
            val fagsak = Fagsak.forTest { this.saksnummer = saksnummer; status = Saksstatuser.AVSLUTTET }

            every { skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId)) } returns listOf(mapping)
            every { fagsakRepository.findAllBySaksnummerIn(listOf(saksnummer)) } returns listOf(fagsak)

            service.finnGyldigSaksnummerForSkjemaIder(listOf(skjemaId)).shouldBeNull()
        }

        @Test
        fun `flere mappinger men bare en gyldig sak returnerer den gyldige`() {
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()
            val saksnummer1 = "MEL-400"
            val saksnummer2 = "MEL-500"
            val mapping1 = SkjemaSakMapping(skjemaId = skjemaId1, fagsak = Fagsak.forTest { this.saksnummer = saksnummer1 })
            val mapping2 = SkjemaSakMapping(skjemaId = skjemaId2, fagsak = Fagsak.forTest { this.saksnummer = saksnummer2 })

            val fagsakOpprettet = Fagsak.forTest { this.saksnummer = saksnummer1; status = Saksstatuser.OPPRETTET }
            val fagsakAvsluttet = Fagsak.forTest { this.saksnummer = saksnummer2; status = Saksstatuser.AVSLUTTET }

            every { skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId1, skjemaId2)) } returns listOf(mapping1, mapping2)
            every { fagsakRepository.findAllBySaksnummerIn(listOf(saksnummer1, saksnummer2)) } returns listOf(fagsakOpprettet, fagsakAvsluttet)

            service.finnGyldigSaksnummerForSkjemaIder(listOf(skjemaId1, skjemaId2)) shouldBe saksnummer1
        }

        @Test
        fun `flere mappinger med to gyldige saker kaster IllegalStateException`() {
            val skjemaId1 = UUID.randomUUID()
            val skjemaId2 = UUID.randomUUID()
            val saksnummer1 = "MEL-600"
            val saksnummer2 = "MEL-700"
            val mapping1 = SkjemaSakMapping(skjemaId = skjemaId1, fagsak = Fagsak.forTest { this.saksnummer = saksnummer1 })
            val mapping2 = SkjemaSakMapping(skjemaId = skjemaId2, fagsak = Fagsak.forTest { this.saksnummer = saksnummer2 })

            val fagsak1 = Fagsak.forTest { this.saksnummer = saksnummer1; status = Saksstatuser.OPPRETTET }
            val fagsak2 = Fagsak.forTest { this.saksnummer = saksnummer2; status = Saksstatuser.LOVVALG_AVKLART }

            every { skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId1, skjemaId2)) } returns listOf(mapping1, mapping2)
            every { fagsakRepository.findAllBySaksnummerIn(listOf(saksnummer1, saksnummer2)) } returns listOf(fagsak1, fagsak2)

            shouldThrow<IllegalStateException> {
                service.finnGyldigSaksnummerForSkjemaIder(listOf(skjemaId1, skjemaId2))
            }
        }
    }

    @Nested
    inner class LagreMapping {

        @Test
        fun `lagrer mapping med alle felter`() {
            val skjemaId = UUID.randomUUID()
            val fagsak = Fagsak.forTest { saksnummer = "MEL-100" }
            val mottatteOpplysninger = mockk<MottatteOpplysninger>()
            val mappingSlot = slot<SkjemaSakMapping>()

            every { skjemaSakMappingRepository.save(capture(mappingSlot)) } answers { mappingSlot.captured }

            service.lagreMapping(skjemaId, fagsak, mottatteOpplysninger = mottatteOpplysninger, originalData = "{}", innsendtDato = null)

            val saved = mappingSlot.captured
            saved.skjemaId shouldBe skjemaId
            saved.saksnummer shouldBe "MEL-100"
            saved.mottatteOpplysninger shouldBe mottatteOpplysninger
            saved.originalData shouldBe "{}"
        }

        @Test
        fun `duplikat PK fanges stille`() {
            val skjemaId = UUID.randomUUID()
            val fagsak = Fagsak.forTest { saksnummer = "MEL-100" }
            every { skjemaSakMappingRepository.save(any()) } throws DataIntegrityViolationException("PK constraint")

            // Skal ikke kaste exception
            service.lagreMapping(skjemaId, fagsak)
        }
    }

    @Nested
    inner class OppdaterJournalpostId {

        @Test
        fun `mapping finnes - oppdaterer journalpostId`() {
            val skjemaId = UUID.randomUUID()
            val mapping = SkjemaSakMapping(skjemaId = skjemaId, fagsak = Fagsak.forTest { saksnummer = "MEL-100" })

            every { skjemaSakMappingRepository.findBySkjemaId(skjemaId) } returns Optional.of(mapping)
            every { skjemaSakMappingRepository.save(any()) } answers { firstArg() }

            service.oppdaterJournalpostId(skjemaId, "JOARK-999")

            mapping.journalpostId shouldBe "JOARK-999"
            verify { skjemaSakMappingRepository.save(mapping) }
        }

        @Test
        fun `mapping finnes ikke - logger advarsel uten exception`() {
            val skjemaId = UUID.randomUUID()
            every { skjemaSakMappingRepository.findBySkjemaId(skjemaId) } returns Optional.empty()

            // Skal ikke kaste exception
            service.oppdaterJournalpostId(skjemaId, "JOARK-999")

            verify(exactly = 0) { skjemaSakMappingRepository.save(any()) }
        }
    }
}
