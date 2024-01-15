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
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AktoerRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Example
import java.util.*

@ExtendWith(MockKExtension::class)
internal class AktoerServiceTest {
    @MockK(relaxed = true)
    private lateinit var aktoerRepository: AktoerRepository

    private lateinit var aktoerService: AktoerService


    private val exampleSlot = slot<Example<Aktoer>>()
    private val aktoerSlot = slot<Aktoer>()

    @BeforeEach
    fun setUp() {
        aktoerService = AktoerService(aktoerRepository)
    }

    @Test
    fun lagEllerOppdater_nyAktoer() {
        val aktoerDto = lagAktoerDto()
        val fagsak = lagFagsak()
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
        val fagsak = lagFagsak()

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
    fun hentfagsakAktoerer() {
        val fagsak = lagFagsak()


        aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG)


        verify { aktoerRepository.findAll(capture((exampleSlot))) }
        val aktoerExample = exampleSlot.captured

        val aktoerProbe = aktoerExample.probe
        aktoerProbe.fagsak shouldBe lagFagsak()
        aktoerProbe.rolle shouldBe Aktoersroller.FULLMEKTIG
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
        aktoer.fagsak = Fagsak()
        val optionalAktoer = Optional.of(aktoer)
        every { aktoerRepository.findById(10L) } returns optionalAktoer


        aktoerService.slettAktoer(10L)


        verify {aktoerRepository.deleteById(optionalAktoer.get().id) }
    }


    @Test
    fun erstattEksisterendeArbeidsgiveraktører_medNyttOrgnr() {
        val fagsak = lagFagsak()
        val orgnumre = listOf("123456789")
        every { aktoerRepository.save(any()) } returns mockk()


        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, orgnumre)


        verify { aktoerRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER) }
        val aktoer = Aktoer()
        aktoer.fagsak = fagsak
        aktoer.rolle = Aktoersroller.ARBEIDSGIVER
        aktoer.orgnr = "123456789"
        verify { aktoerRepository.save(aktoer) }
    }

    @Test
    fun erstattEksisterendeArbeidsgiveraktører_utenNyeOrgnr() {
        val fagsak = lagFagsak()


        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, emptyList())


        verify { aktoerRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER) }
        verify(exactly = 0) { aktoerRepository.save(any()) }
    }

    private fun lagFagsak(): Fagsak {
        val fagsak = Fagsak()
        fagsak.saksnummer = "MELTEST-1"
        return fagsak
    }

    private fun lagAktoer(): Aktoer = Aktoer().apply {
        id = 234L
    }

    private fun assertAktoerData(aktoerDto: AktoerDto, fagsak: Fagsak, aktoer: Aktoer) {
        aktoer.fagsak shouldBe fagsak
        aktoer.institusjonId shouldBe aktoerDto.institusjonsID
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
