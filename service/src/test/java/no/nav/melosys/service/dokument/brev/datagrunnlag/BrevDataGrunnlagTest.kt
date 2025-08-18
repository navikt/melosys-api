package no.nav.melosys.service.dokument.brev.datagrunnlag

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAvklartMaritimtArbeid
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagMaritimtArbeid
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrevDataGrunnlagTest {
    @MockK
    private lateinit var kodeverkService: KodeverkService

    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var brevbestilling: DoksysBrevbestilling
    private lateinit var søknad: Soeknad
    private lateinit var dataGrunnlag: BrevDataGrunnlag

    @BeforeEach
    fun setUp() {
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap<String, AvklartMaritimtArbeid>()

        søknad = Soeknad()
        val behandling = lagBehandling(søknad)

        brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val avklarteVirksomheterService = mockk<AvklarteVirksomheterService>()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()
        dataGrunnlag = BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
    }

    private fun lagBehandling(søknad: Soeknad) = Behandling.forTest {
        id = 1L
        fagsak {
            medBruker()
        }
        this.mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = søknad
        }
    }

    @Test
    fun `skal konvertere avklart maritim arbeid til maritime arbeidssteder`() {
        val maritimtArbeid = lagAvklartMaritimtArbeid()
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns mapOf("Dunfjæder" to maritimtArbeid)
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val avklarteVirksomheterService = mockk<AvklarteVirksomheterService>()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()
        dataGrunnlag = BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
        val maritimtArbeidISøknad = lagMaritimtArbeid()
        søknad.maritimtArbeid.add(maritimtArbeidISøknad)


        val arbeidssteder = dataGrunnlag.arbeidsstedGrunnlag.hentArbeidssteder()


        arbeidssteder.shouldHaveSize(1)
        val arbeidssted = arbeidssteder[0] as MaritimtArbeidssted
        arbeidssted.run {
            enhetNavn shouldBe maritimtArbeidISøknad.enhetNavn
            foretakNavn.shouldBeNull()
            idnummer.shouldBeNull()
            yrkesgruppe.kode shouldBe Yrkesgrupper.SOKKEL_ELLER_SKIP.kode
        }
    }

    @Test
    fun `skal returnere tom liste når maritim arbeid mangler avklarte fakta`() {
        val maritimtArbeidISøknad = lagMaritimtArbeid()
        søknad.maritimtArbeid.add(maritimtArbeidISøknad)
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap<String, AvklartMaritimtArbeid>()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val avklarteVirksomheterService = mockk<AvklarteVirksomheterService>()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()
        val dataGrunnlagUtenAvklartMaritimtArbeid = BrevDataGrunnlag(
            brevbestilling,
            kodeverkService,
            avklarteVirksomheterService,
            avklartefaktaService,
            persondata
        )


        val arbeidssteder = dataGrunnlagUtenAvklartMaritimtArbeid.arbeidsstedGrunnlag.hentArbeidssteder()


        arbeidssteder.shouldBeEmpty()
    }
}
