package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.service.aktoer.AktoerDto
import no.nav.melosys.service.aktoer.AktoerService
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class DigitalSøknadAktørSynkroniseringTest {

    @MockK(relaxed = true) lateinit var aktoerService: AktoerService
    @MockK(relaxed = true) lateinit var kontaktopplysningService: KontaktopplysningService
    @MockK lateinit var persondataFasade: PersondataFasade

    private lateinit var synkronisering: DigitalSøknadAktørSynkronisering

    private val saksnummer = "MEL-1234"
    private val arbeidsgiverOrgnr = "111111111"
    private val rådgiverOrgnr = "333333333"
    private val fullmektigFnr = "10987654321"
    private val fullmektigAktørId = "1234567890123"
    private val fullmektigNavn = "Ola Nordmann"

    @BeforeEach
    fun setup() {
        synkronisering = DigitalSøknadAktørSynkronisering(aktoerService, kontaktopplysningService, persondataFasade)
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns Optional.empty()
    }

    @Test
    fun `RADGIVER_UTEN_FULLMAKT lagrer FULLMEKTIG-aktør med orgnr og kontaktopplysning`() {
        val fagsak = Fagsak.forTest { this.saksnummer = this@DigitalSøknadAktørSynkroniseringTest.saksnummer }
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER) } returns emptyList()
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG) } returnsMany listOf(emptyList(), emptyList())
        every { persondataFasade.hentSammensattNavn(fullmektigFnr) } returns fullmektigNavn

        val aktørSlot = slot<AktoerDto>()
        every { aktoerService.lagEllerOppdaterAktoer(eq(fagsak), capture(aktørSlot)) } returns 1L

        synkronisering.synkroniser(fagsak, AktørerFraSøknad(
            arbeidsgiverOrgnumre = listOf(arbeidsgiverOrgnr),
            fullmektige = listOf(FullmektigSpec(
                orgnr = rådgiverOrgnr,
                personIdent = null,
                kontaktpersonFnr = fullmektigFnr,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            )),
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
        ))

        aktørSlot.captured.orgnr shouldBe rådgiverOrgnr
        aktørSlot.captured.personIdent shouldBe null
        aktørSlot.captured.fullmakter shouldBe setOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
        verify { kontaktopplysningService.lagEllerOppdaterKontaktopplysning(saksnummer, rådgiverOrgnr, null, fullmektigNavn, null) }
    }

    @Test
    fun `RADGIVER_MED_FULLMAKT lagrer én aktør med både orgnr og personIdent`() {
        val fagsak = Fagsak.forTest { this.saksnummer = this@DigitalSøknadAktørSynkroniseringTest.saksnummer }
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER) } returns emptyList()
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG) } returnsMany listOf(emptyList(), emptyList())
        every { persondataFasade.hentAktørIdForIdent(fullmektigFnr) } returns fullmektigAktørId
        every { persondataFasade.hentSammensattNavn(fullmektigFnr) } returns fullmektigNavn

        val aktørSlot = slot<AktoerDto>()
        every { aktoerService.lagEllerOppdaterAktoer(eq(fagsak), capture(aktørSlot)) } returns 1L

        synkronisering.synkroniser(fagsak, AktørerFraSøknad(
            arbeidsgiverOrgnumre = listOf(arbeidsgiverOrgnr),
            fullmektige = listOf(FullmektigSpec(
                orgnr = rådgiverOrgnr,
                personIdent = fullmektigFnr,
                kontaktpersonFnr = fullmektigFnr,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            )),
            skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
        ))

        aktørSlot.captured.orgnr shouldBe rådgiverOrgnr
        aktørSlot.captured.personIdent shouldBe fullmektigFnr
        aktørSlot.captured.aktoerID shouldBe fullmektigAktørId
        aktørSlot.captured.fullmakter shouldBe setOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
    }

    @Test
    fun `ANNEN_PERSON lagrer FULLMEKTIG-aktør med kun personIdent (ingen kontaktopplysning)`() {
        val fagsak = Fagsak.forTest { this.saksnummer = this@DigitalSøknadAktørSynkroniseringTest.saksnummer }
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER) } returns emptyList()
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG) } returnsMany listOf(emptyList(), emptyList())
        every { persondataFasade.hentAktørIdForIdent(fullmektigFnr) } returns fullmektigAktørId

        val aktørSlot = slot<AktoerDto>()
        every { aktoerService.lagEllerOppdaterAktoer(eq(fagsak), capture(aktørSlot)) } returns 1L

        synkronisering.synkroniser(fagsak, AktørerFraSøknad(
            arbeidsgiverOrgnumre = listOf(arbeidsgiverOrgnr),
            fullmektige = listOf(FullmektigSpec(
                orgnr = null,
                personIdent = fullmektigFnr,
                kontaktpersonFnr = null,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
            )),
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
        ))

        aktørSlot.captured.orgnr shouldBe null
        aktørSlot.captured.personIdent shouldBe fullmektigFnr
        aktørSlot.captured.aktoerID shouldBe fullmektigAktørId
        verify(exactly = 0) { kontaktopplysningService.lagEllerOppdaterKontaktopplysning(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `DEG_SELV uten endring i arbeidsgivere kaller ikke erstatt`() {
        val fagsak = Fagsak.forTest { this.saksnummer = this@DigitalSøknadAktørSynkroniseringTest.saksnummer }
        val eksisterendeArbeidsgiver = Aktoer().apply {
            this.orgnr = arbeidsgiverOrgnr
            rolle = Aktoersroller.ARBEIDSGIVER
        }
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER) } returns listOf(eksisterendeArbeidsgiver)
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG) } returnsMany listOf(emptyList(), emptyList())

        synkronisering.synkroniser(fagsak, AktørerFraSøknad(
            arbeidsgiverOrgnumre = listOf(arbeidsgiverOrgnr),
            fullmektige = emptyList(),
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
        ))

        verify(exactly = 0) { aktoerService.erstattEksisterendeArbeidsgiveraktører(any(), any()) }
    }

    @Test
    fun `AT-del DEG_SELV på sak med tidligere RADGIVER_MED_FULLMAKT fjerner FULLMEKTIG_SØKNAD-fullmaktstype og personIdent`() {
        val fagsak = Fagsak.forTest { this.saksnummer = this@DigitalSøknadAktørSynkroniseringTest.saksnummer }
        val eksisterende = mockk<Aktoer>(relaxed = true).apply {
            every { id } returns 1L
            every { orgnr } returns rådgiverOrgnr
            every { personIdent } returns fullmektigFnr
            every { aktørId } returns fullmektigAktørId
            every { fullmaktstyper } returns setOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            every { rolle } returns Aktoersroller.FULLMEKTIG
            every { institusjonID } returns null
            every { utenlandskPersonId } returns null
        }
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER) } returns emptyList()
        every { aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG) } returnsMany listOf(listOf(eksisterende), listOf(eksisterende))

        val dtoSlot = slot<AktoerDto>()
        every { aktoerService.lagEllerOppdaterAktoer(eq(fagsak), capture(dtoSlot)) } returns 1L

        synkronisering.synkroniser(fagsak, AktørerFraSøknad(
            arbeidsgiverOrgnumre = listOf(arbeidsgiverOrgnr),
            fullmektige = emptyList(), // DEG_SELV produserer ingen fullmektige
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
        ))

        // Skal ha oppdatert eksisterende: fjernet FULLMEKTIG_SØKNAD og personIdent
        dtoSlot.captured.fullmakter shouldBe setOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
        dtoSlot.captured.personIdent shouldBe null
        dtoSlot.captured.aktoerID shouldBe null
        dtoSlot.captured.orgnr shouldBe rådgiverOrgnr
    }
}
