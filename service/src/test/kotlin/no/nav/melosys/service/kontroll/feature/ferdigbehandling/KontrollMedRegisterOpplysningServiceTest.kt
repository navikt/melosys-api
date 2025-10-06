package no.nav.melosys.service.kontroll.feature.ferdigbehandling

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.brev.UtkastBrevService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
internal class KontrollMedRegisterOpplysningServiceTest {

    @MockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @RelaxedMockK
    lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @RelaxedMockK
    lateinit var registeropplysningerService: RegisteropplysningerService

    @RelaxedMockK
    lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @RelaxedMockK
    lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @RelaxedMockK
    lateinit var utkastBrevService: UtkastBrevService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var trygdeavgiftService: TrygdeavgiftService

    @RelaxedMockK
    lateinit var trygdeagiftMottakerService: TrygdeavgiftMottakerService

    @RelaxedMockK
    lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    lateinit var mockedKontrollMedRegisterOpplysning: KontrollMedRegisteropplysning

    private val BEHANDLING_ID = 1L
    private val behandling = SaksbehandlingDataFactory.lagBehandling(MottatteOpplysningerData())
    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        every { persondataFasade.hentFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns "fnr"
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling

        val mockedKontroll = Kontroll(
            behandlingService,
            lovvalgsperiodeService,
            avklarteVirksomheterService,
            persondataFasade,
            organisasjonOppslagService,
            saksbehandlingRegler,
            medlemskapsperiodeService,
            utkastBrevService,
            behandlingsresultatService,
            trygdeavgiftService,
            trygdeagiftMottakerService,
            helseutgiftDekkesPeriodeService,
            unleash
        )
        mockedKontrollMedRegisterOpplysning =
            KontrollMedRegisteropplysning(behandlingService, persondataFasade, registeropplysningerService, mockedKontroll)
    }

    @Test
    fun kontrollerVedtak_oppdatererRegisteropplysningerOgFårKontrollFeilFraKontroller() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        every { lovvalgsperiodeService.hentLovvalgsperiode(BEHANDLING_ID) } returns Lovvalgsperiode().apply {
            tom = null
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        }
        every { lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(BEHANDLING_ID) } returns null
        behandling.saksopplysninger.add(Saksopplysning().apply {
            type = SaksopplysningType.MEDL
            dokument = MedlemskapDokument()
        })


        val alleKontrollfeil = mockedKontrollMedRegisterOpplysning.kontrollerVedtak(
            behandling, Sakstyper.EU_EOS, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet()
        )


        alleKontrollfeil
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO, Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET_BREV)
        verify { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }

    @Test
    fun kontrollerVedtak_medOgUtenFeilSomSkalIgnoreres_returnererKorrekt() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()
        val feilSomSkalIgnoreres = Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER


        val kontrollFeilMedFeilSomSkalIgnoreres = mockedKontrollMedRegisterOpplysning.kontrollerVedtak(
            behandling, Sakstyper.EU_EOS, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, setOf(feilSomSkalIgnoreres)
        )

        kontrollFeilMedFeilSomSkalIgnoreres.shouldBeEmpty()


        val kontrollFeilUtenFeilSomSkalIgnoreres = mockedKontrollMedRegisterOpplysning.kontrollerVedtak(
            behandling, Sakstyper.EU_EOS, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet()
        )

        kontrollFeilUtenFeilSomSkalIgnoreres
            .shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(feilSomSkalIgnoreres)
        verify(exactly = 2) { registeropplysningerService.hentOgLagreOpplysninger(any()) }
    }
}
