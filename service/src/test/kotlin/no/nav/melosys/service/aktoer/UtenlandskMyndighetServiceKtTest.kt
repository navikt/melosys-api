package no.nav.melosys.service.aktoer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.UtenlandskMyndighetRepository
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class UtenlandskMyndighetServiceKtTest {

    @RelaxedMockK
    lateinit var utenlandskMyndighetRepositoryMock: UtenlandskMyndighetRepository

    @RelaxedMockK
    lateinit var fagsakServiceMock: FagsakService

    @RelaxedMockK
    lateinit var landvelgerServiceMock: LandvelgerService

    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        utenlandskMyndighetService = UtenlandskMyndighetService(utenlandskMyndighetRepositoryMock, landvelgerServiceMock, fagsakServiceMock)
        behandling = lagBehandling()
    }

    @Test
    fun avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndighetForTrygdeavtale() {
        behandling.fagsak.type = Sakstyper.TRYGDEAVTALE
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.NO)


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling)


        verify { fagsakServiceMock.oppdaterMyndighetForTrygdeavtale(FagsakTestFactory.SAKSNUMMER, Land_iso2.NO) }
        verify(exactly = 1) { fagsakServiceMock.oppdaterMyndighetForTrygdeavtale(any(), any()) }
    }

    @Test
    fun avklarUtenlandskMyndighetSomAktørOgLagre_kasterFunksjonellException_nårDetErFlereLandkoder() {
        behandling.fagsak.type = Sakstyper.TRYGDEAVTALE
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.NO, Land_iso2.BE)


        val exception = shouldThrow<FunksjonellException> {
            utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling)
        }


        exception.message shouldContain "Fant ingen eller flere enn ett trygdemyndighetsland for bilaterale trygdeavtaler."
    }

    @Test
    fun avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndigheterForEuEos() {
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE)
        val utenlandskMyndighet = UtenlandskMyndighet().apply {
            landkode = Land_iso2.SE
        }
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE) } returns Optional.of(utenlandskMyndighet)


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling)


        verify { fagsakServiceMock.oppdaterMyndigheterForEuEos(eq(FagsakTestFactory.SAKSNUMMER), any()) }
        verify(exactly = 1) { fagsakServiceMock.oppdaterMyndigheterForEuEos(any(), any()) }
    }

    @Test
    fun avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndigheterMedRiktigId() {
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE, Land_iso2.DK)
        val svenskUtenlandskMyndighet = UtenlandskMyndighet().apply {
            landkode = Land_iso2.SE
            institusjonskode = "INSTITUSJONSKODE"
        }
        val danskUtenlandskMyndighet = UtenlandskMyndighet().apply {
            landkode = Land_iso2.DK
            institusjonskode = null
        }
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE) } returns Optional.of(svenskUtenlandskMyndighet)
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.DK) } returns Optional.of(danskUtenlandskMyndighet)


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling)


        verify { fagsakServiceMock.oppdaterMyndigheterForEuEos(FagsakTestFactory.SAKSNUMMER, listOf("SE:INSTITUSJONSKODE", "DK")) }
        verify(exactly = 1) { fagsakServiceMock.oppdaterMyndigheterForEuEos(any(), any()) }
    }

    @Test
    fun avklarUtenlandskMyndighetSomAktørOgLagre_kasterIkkeFunnetException_nårUtenlandskmyndighetIkkeErFunnet() {
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE)
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE) } returns Optional.empty()


        val exception = shouldThrow<FunksjonellException> {
            utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling)
        }


        exception.message shouldContain "Finner ikke utenlandskMyndighet for SE."
    }

    @Test
    fun hentUtenlandskMyndighet_kasterIkkeFunnetException_nårUtenlandskmyndighetIkkeErFunnet() {
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE) } returns Optional.empty()


        val exception = shouldThrow<FunksjonellException> {
            utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.SE, null)
        }


        exception.message shouldContain "Finner ikke utenlandskMyndighet for SE."
    }

    @Test
    fun lagUtenlandskeMyndigheterFraBehandling_svelgerIkkeFunnetException_nårLandvelgerIkkeFinnerUtenlandskMyndighet() {
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } throws IkkeFunnetException("asd")


        utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)


        verify { utenlandskMyndighetRepositoryMock.findByLandkodeIsIn(emptyList()) }
    }

    @Test
    fun avklarUtenlandskMyndighetSomAktørOgLagre_forventkorrektInstitusjonsId() {
        val utenlandskMyndighet = lagUtenlandskMyndighet(Land_iso2.IT, "IT123", null)
        val utenlandskMyndighetReservert = lagUtenlandskMyndighet(Land_iso2.CZ, "CZ123", Preferanse.PreferanseEnum.RESERVERT_FRA_A1)
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.IT) } returns Optional.of(utenlandskMyndighet)
        every { utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.CZ) } returns Optional.of(utenlandskMyndighetReservert)
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(any()) } returns listOf(Land_iso2.IT, Land_iso2.CZ)


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling)


        verify { fagsakServiceMock.oppdaterMyndigheterForEuEos(eq(behandling.fagsak.saksnummer), any()) }
    }

    @Test
    fun lagUtenlandskeMyndigheterFraBehandling_mapperUtenlandskmyndighetTilAktør() {
        val utenlandskeMyndigheterLandkoder = listOf(Land_iso2.SE, Land_iso2.DK)
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns utenlandskeMyndigheterLandkoder
        val svenskUtenlandskMyndighet = UtenlandskMyndighet().apply {
            landkode = Land_iso2.SE
            institusjonskode = "INSTSE"
            postnummer = "123"
        }
        val danskUtenlandskMyndighet = UtenlandskMyndighet().apply {
            landkode = Land_iso2.DK
            institusjonskode = "INSTDK"
            postnummer = "123"
        }
        val utenlandskMyndighetList = listOf(svenskUtenlandskMyndighet, danskUtenlandskMyndighet)
        every { utenlandskMyndighetRepositoryMock.findByLandkodeIsIn(utenlandskeMyndigheterLandkoder) } returns utenlandskMyndighetList


        val resultat = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)


        resultat[svenskUtenlandskMyndighet]?.rolle shouldBe Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        resultat[svenskUtenlandskMyndighet]?.institusjonID shouldBe "SE:INSTSE"
        resultat[danskUtenlandskMyndighet]?.rolle shouldBe Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        resultat[danskUtenlandskMyndighet]?.institusjonID shouldBe "DK:INSTDK"
    }

    @Test
    fun lagUtenlandskeMyndigheterFraBehandling_forventAktoerMedGyldigInstitusjonsId() {
        val utenlandskMyndighet = lagUtenlandskMyndighet(Land_iso2.IT, "IT123", null)
        val utenlandskMyndighetReservert = lagUtenlandskMyndighet(Land_iso2.CZ, "CZ123", Preferanse.PreferanseEnum.RESERVERT_FRA_A1)
        every { landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.IT, Land_iso2.CZ)
        every { utenlandskMyndighetRepositoryMock.findByLandkodeIsIn(any()) } returns listOf(utenlandskMyndighet, utenlandskMyndighetReservert)


        val mottakere = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling)


        mottakere shouldBe mapOf(
            utenlandskMyndighet to Mottaker(
                rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET,
                institusjonID = "IT:IT123"
            ),
            utenlandskMyndighetReservert to Mottaker(
                rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET,
                institusjonID = "CZ:CZ123"
            )
        )
    }

    private fun lagBehandling(): Behandling = Behandling.forTest {
        id = BEHANDLING_ID
        fagsak {
            saksnummer = FagsakTestFactory.SAKSNUMMER
        }
    }

    private fun lagUtenlandskMyndighet(landkode: Land_iso2, institusjonID: String, preferanse: Preferanse.PreferanseEnum?): UtenlandskMyndighet =
        UtenlandskMyndighet().apply {
            institusjonskode = institusjonID
            this.landkode = landkode
            postnummer = "123"
            if (preferanse != null) {
                val preferanser = HashSet<Preferanse>()
                preferanser.add(Preferanse(1L, preferanse))
                this.preferanser = preferanser
            }
        }

    companion object {
        private const val BEHANDLING_ID = 1L
    }
}
