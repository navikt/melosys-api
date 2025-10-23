package no.nav.melosys.service.aktoer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Fullmakt
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AktoerRepository
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
internal class AktoerServiceTest {
    @MockK(relaxed = true)
    private lateinit var aktoerRepository: AktoerRepository
    
    @MockK(relaxed = true)
    private lateinit var aksesskontroll: Aksesskontroll

    private lateinit var aktoerService: AktoerService

    private val aktoerSlot = slot<Aktoer>()

    @BeforeEach
    fun setUp() {
        aktoerService = AktoerService(aktoerRepository, aksesskontroll)
    }

    @Test
    fun lagEllerOppdater_nyAktoer() {
        val aktoerDto = lagAktoerDto()
        val fagsak = Fagsak.forTest()
        val aktoer = lagAktoer()
        every { aktoerRepository.save(any()) } returns aktoer


        val databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto)


        verify { aktoerRepository.save(capture(aktoerSlot)) }
        val aktoerCaptured = aktoerSlot.captured
        assertAktoerData(aktoerDto, fagsak, aktoerCaptured)
        aktoerCaptured.id.shouldBeNull()
        databaseId shouldBe aktoer.id
    }

    @Test
    fun lagEllerOppdater_oppdaterAktoer() {
        val aktoerFromDatabase = lagAktoer()
        val aktoerDto = lagAktoerDto()
        aktoerDto.databaseID = aktoerFromDatabase.id
        val fagsak = Fagsak.forTest()

        every { aktoerRepository.save(any()) } returns aktoerFromDatabase
        every { aktoerRepository.findById(aktoerDto.databaseID) } returns Optional.of(aktoerFromDatabase)


        val databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto)


        verify { aktoerRepository.save(capture(aktoerSlot)) }
        val aktoerCaptured = aktoerSlot.captured
        assertAktoerData(aktoerDto, fagsak, aktoerCaptured)
        aktoerCaptured.id shouldBe aktoerDto.databaseID
        databaseId shouldBe aktoerDto.databaseID
    }

    @Test
    fun `bare en fullmektig per fullmakstype`() {
        val aktoerDto = lagAktoerDto().apply {
            fullmakter = setOf(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        }
        val fagsak = Fagsak.forTest()
        val aktoer = lagAktoer().apply {
            fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
        }

        every { aktoerRepository.findByFagsakAndFullmakterIsNotEmpty(fagsak) } returns listOf(aktoer)


        val exception = shouldThrow<FunksjonellException> { aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto) }
        exception.message shouldContain "en fullmektig per fullmakttype"
    }

    @Test
    fun hentfagsakAktoerer() {
        val fagsak = Fagsak.forTest()


        aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG)


        val fagsakSlot = slot<Fagsak>()
        val aktoersrollerSlot = slot<Aktoersroller>()

        verify { aktoerRepository.findByFagsakAndRolle(capture(fagsakSlot), capture(aktoersrollerSlot) ) }
        val fagsakCaptured = fagsakSlot.captured
        val aktoersrollerCaptured = aktoersrollerSlot.captured
        fagsakCaptured shouldBe Fagsak.forTest()
        aktoersrollerCaptured shouldBe Aktoersroller.FULLMEKTIG
    }

    @Test
    fun slettAktør_sletteBruker_kasterException() {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.BRUKER
        val optionalAktoer = Optional.of(aktoer)
        every { aktoerRepository.findById(10L) } returns optionalAktoer


        val exception = shouldThrow<FunksjonellException> { aktoerService.slettAktoer(10L) }
        exception.message shouldContain "er en bruker"

        verify(exactly = 0) { aktoerRepository.deleteByAktørId(optionalAktoer.get().aktørId) }
    }

    @Test
    fun slettAktør_sletteFullmektig_fungerer() {
        val aktoer = Aktoer()
        aktoer.id = 10L
        aktoer.rolle = Aktoersroller.FULLMEKTIG
        aktoer.fagsak = Fagsak.forTest()
        val optionalAktoer = Optional.of(aktoer)
        every { aktoerRepository.findById(10L) } returns optionalAktoer


        aktoerService.slettAktoer(10L)


        verify {aktoerRepository.deleteById(optionalAktoer.get().id) }
    }


    @Test
    fun erstattEksisterendeArbeidsgiveraktører_medNyttOrgnr() {
        val fagsak = Fagsak.forTest()
        val orgnumre = listOf("123456789")
        every { aktoerRepository.save(any()) } returns mockk()


        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, orgnumre)


        verify { aktoerRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER) }
        val aktoer = Aktoer()
        aktoer.fagsak = Fagsak.forTest()
        aktoer.rolle = Aktoersroller.ARBEIDSGIVER
        aktoer.orgnr = "123456789"
        verify { aktoerRepository.save(aktoer) }
    }

    @Test
    fun erstattEksisterendeArbeidsgiveraktører_utenNyeOrgnr() {
        val fagsak = Fagsak.forTest()


        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, emptyList())


        verify { aktoerRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER) }
        verify(exactly = 0) { aktoerRepository.save(any()) }
    }

    private fun lagAktoer(): Aktoer = Aktoer().apply {
        id = 234L
    }

    private fun assertAktoerData(aktoerDto: AktoerDto, fagsak: Fagsak, aktoer: Aktoer) {
        aktoer.fagsak shouldBe fagsak
        aktoer.institusjonID shouldBe aktoerDto.institusjonsID
        aktoer.utenlandskPersonId shouldBe aktoerDto.utenlandskPersonID
        aktoer.orgnr shouldBe aktoerDto.orgnr
        aktoer.rolle.toString() shouldBe aktoerDto.rolleKode
        aktoer.fullmaktstyper shouldBe aktoerDto.fullmakter
        aktoer.personIdent shouldBe aktoerDto.personIdent
    }


    private fun lagAktoerDto(): AktoerDto {
        val aktoerDto = AktoerDto()
        aktoerDto.rolleKode = "BRUKER"
        aktoerDto.institusjonsID = "institusjonsID"
        aktoerDto.utenlandskPersonID = "utenlandskPersonID"
        aktoerDto.orgnr = "orgnr"
        aktoerDto.personIdent = "21075114491"
        aktoerDto.fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
        return aktoerDto
    }
}
