package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenTestData.FNR_BRUKER
import no.nav.melosys.service.dokument.DokgenTestData.FNR_FULLMEKTIG
import no.nav.melosys.service.dokument.DokgenTestData.ORGNR_FULLMEKTIG
import no.nav.melosys.service.dokument.DokgenTestData.lagBehandling
import no.nav.melosys.service.dokument.DokgenTestData.lagMottaker
import no.nav.melosys.service.dokument.DokgenTestData.lagMottakerFullmektig
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DokgenMapperDatahenterKtTest {

    @MockK
    private lateinit var eregFasade: EregFasade
    
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    
    @MockK
    private lateinit var kodeverkService: KodeverkService
    
    @MockK
    private lateinit var persondataFasade: PersondataFasade
    
    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    private lateinit var dokgenMapperDatahenter: DokgenMapperDatahenter

    @BeforeEach
    fun init() {
        dokgenMapperDatahenter = DokgenMapperDatahenter(
            behandlingsresultatService, 
            eregFasade, 
            persondataFasade, 
            kodeverkService, 
            avklarteVirksomheterService
        )
    }

    @Test
    fun `hentFullmektigNavn fullmektig person henter sammensatt navn person`() {
        val fullmektig = Aktoer().apply {
            personIdent = FNR_FULLMEKTIG
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
        }
        val fagsak = FagsakTestFactory.builder().aktører(setOf(fullmektig, Aktoer())).build()
        val brevbestilling = DokgenBrevbestilling().apply {
            behandling = BehandlingTestFactory.builderWithDefaults()
                .medFagsak(fagsak)
                .build()
        }

        every { persondataFasade.hentSammensattNavn(FNR_FULLMEKTIG) } returns "Etternavn, Fornavn"

        dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD)

        verify { persondataFasade.hentSammensattNavn(FNR_FULLMEKTIG) }
    }

    @Test
    fun `hentFullmektigNavn fullmektig org henter navn organisasjon`() {
        val fullmektig = Aktoer().apply {
            orgnr = ORGNR_FULLMEKTIG
            rolle = Aktoersroller.FULLMEKTIG
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
        }
        val fagsak = FagsakTestFactory.builder().aktører(setOf(fullmektig, Aktoer())).build()
        val brevbestilling = DokgenBrevbestilling().apply {
            behandling = BehandlingTestFactory.builderWithDefaults()
                .medFagsak(fagsak)
                .build()
        }

        every { eregFasade.hentOrganisasjonNavn(ORGNR_FULLMEKTIG) } returns "Orgnavn"

        dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD)

        verify { eregFasade.hentOrganisasjonNavn(ORGNR_FULLMEKTIG) }
    }

    @Test
    fun `hentPersondata mottaker er ikke virksomhet kaller persondataFasade`() {
        every { persondataFasade.hentPerson(any()) } returns mockk()
        
        dokgenMapperDatahenter.hentPersondata(lagBehandling())

        verify { persondataFasade.hentPerson(any()) }
    }

    @Test
    fun `hentPersondata mottaker er virksomhet returnerer null`() {
        val behandling = lagBehandling()
        behandling.fagsak.aktører.forEach { it.rolle = Aktoersroller.VIRKSOMHET }
        
        val response = dokgenMapperDatahenter.hentPersondata(behandling)

        response.shouldBeNull()
        verify(exactly = 0) { persondataFasade.hentPerson(any()) }
    }

    @Test
    fun `hentPersonMottaker mottaker aktørID bruker aktørID`() {
        every { persondataFasade.hentPerson(FNR_BRUKER) } returns mockk()
        
        dokgenMapperDatahenter.hentPersonMottaker(lagMottaker(Mottakerroller.BRUKER))

        verify { persondataFasade.hentPerson(FNR_BRUKER) }
    }

    @Test
    fun `hentPersonMottaker mottaker personIdent bruker personIdent`() {
        every { persondataFasade.hentPerson(FNR_FULLMEKTIG) } returns mockk()
        
        dokgenMapperDatahenter.hentPersonMottaker(lagMottakerFullmektig(Aktoertype.PERSON))

        verify { persondataFasade.hentPerson(FNR_FULLMEKTIG) }
    }

    @Test
    fun `hentAvklartVirksomhet benytter norsk register ved treff`() {
        val behandling = lagBehandling()
        val avklartVirksomhetMock = mockk<AvklartVirksomhet>()

        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling) } returns listOf(avklartVirksomhetMock)

        val response = dokgenMapperDatahenter.hentAvklartVirksomhet(behandling)

        response shouldBe avklartVirksomhetMock
        verify(exactly = 0) { avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling) }
    }

    @Test
    fun `hentAvklartVirksomhet benytter det utenlandsk registeret dersom det ikke eksisterer i norsk register`() {
        val avklartVirksomhetMock = mockk<AvklartVirksomhet>()
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any<Behandling>()) } returns emptyList()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any<Behandling>()) } returns listOf(avklartVirksomhetMock)

        val behandling = lagBehandling()
        val response = dokgenMapperDatahenter.hentAvklartVirksomhet(behandling)

        response shouldBe avklartVirksomhetMock
        verify { avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling) }
    }

    @Test
    fun `hentAvklartVirksomhet kaster funksjonelt exception dersom org ikke eksisterer`() {
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any<Behandling>()) } returns emptyList()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any<Behandling>()) } returns emptyList()

        shouldThrow<FunksjonellException> {
            dokgenMapperDatahenter.hentAvklartVirksomhet(lagBehandling())
        }
    }
}