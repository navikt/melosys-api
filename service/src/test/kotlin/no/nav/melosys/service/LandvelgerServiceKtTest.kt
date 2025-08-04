package no.nav.melosys.service

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class LandvelgerServiceKtTest {

    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private val behandlingID = 1L

    private lateinit var søknad: Soeknad
    private lateinit var lovvalgsperiode: Lovvalgsperiode
    private lateinit var anmodningsperiode: Anmodningsperiode
    private lateinit var landvelgerService: LandvelgerService
    private lateinit var behandling: Behandling

    private val søknadsland = Land_iso2.DE
    private val avklartArbeidsland = Land_iso2.DK
    private val oppgittbostedsland = Land_iso2.SE
    private val avklartBostedsland = Bostedsland(Landkoder.FI)
    private val territorialfarvannLand = Land_iso2.GB

    @BeforeEach
    fun setUp() {
        søknad = Soeknad()
        søknad.oppholdUtland.oppholdslandkoder = søknad.oppholdUtland.oppholdslandkoder + "NO"
        søknad.bosted.oppgittAdresse.landkode = oppgittbostedsland.kode
        // Don't set søknadsland by default - let individual tests set it as needed
        val maritimtArbeid = MaritimtArbeid()
        maritimtArbeid.territorialfarvannLandkode = territorialfarvannLand.kode
        søknad.maritimtArbeid = søknad.maritimtArbeid + maritimtArbeid

        lovvalgsperiode = Lovvalgsperiode()

        anmodningsperiode = Anmodningsperiode()
        anmodningsperiode.unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1

        behandling = lagBehandlingMedSedDokument()

        lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        landvelgerService = LandvelgerService(avklartefaktaService, behandlingsresultatService, mottatteOpplysningerService)

        // Default mocks for avklartefaktaService
        every { avklartefaktaService.hentAlleAvklarteArbeidsland(any()) } returns mutableSetOf()
        every { avklartefaktaService.hentLandkoderMedMarginaltArbeid(any()) } returns mutableSetOf()
    }

    private fun lagBehandlingsresultat(periode: PeriodeOmLovvalg): Behandlingsresultat {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(behandlingID)
            .medFagsak(fagsak)
            .build()
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.behandling = behandling
        behandlingsresultat.id = behandlingID
        when (periode) {
            is Lovvalgsperiode -> behandlingsresultat.lovvalgsperioder = setOf(periode)
            is Anmodningsperiode -> behandlingsresultat.anmodningsperioder = setOf(periode)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        return behandlingsresultat
    }

    private fun leggTilAlleAvklartArbeidsland(landkoder: Collection<Land_iso2>) {
        val newLandkoder = søknad.soeknadsland.landkoder.toMutableList()
        for (landkode in landkoder) {
            newLandkoder.add(landkode.kode)
        }
        søknad.soeknadsland.landkoder = newLandkoder
        every { avklartefaktaService.hentAlleAvklarteArbeidsland(any()) } returns landkoder.toSet()
    }

    @Test
    fun `hentArbeidsland utenAvklartArbeidsland girSøknadsland`() {
        mockMottatteOpplysninger()
        søknad.soeknadsland.landkoder = søknad.soeknadsland.landkoder + søknadsland.kode

        val land = landvelgerService.hentArbeidsland(behandlingID).beskrivelse

        land shouldBe søknadsland.beskrivelse
    }

    @Test
    fun `hentAlleArbeidsland medAvklartArbeidsland girAvklartArbeidsland`() {
        lagBehandlingsresultat(lovvalgsperiode)
        leggTilAlleAvklartArbeidsland(setOf(avklartArbeidsland))

        val land = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID)

        land shouldContainExactly listOf(avklartArbeidsland)
    }

    @Test
    fun `hentAlleArbeidsland medAvklartArbeidslandOgSøknadsland girAlleUnikeArbeidsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        every { avklartefaktaService.hentAlleAvklarteArbeidsland(any()) } returns setOf(Land_iso2.DK, Land_iso2.NO)
        søknad.soeknadsland.landkoder = listOf(Landkoder.DK.kode, Landkoder.SE.kode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A

        val arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID)

        arbeidsland shouldContainExactlyInAnyOrder listOf(Land_iso2.NO, Land_iso2.DK, Land_iso2.SE)
        arbeidsland.count { it == Land_iso2.DK } shouldBe 1
    }

    @Test
    fun `hentAlleArbeidsland noenMedMarginaltArbeid girKunArbeidslandMedVesentligVirksomhet`() {
        lagBehandlingsresultat(lovvalgsperiode)
        leggTilAlleAvklartArbeidsland(listOf(Land_iso2.DK, Land_iso2.SE))
        every { avklartefaktaService.hentLandkoderMedMarginaltArbeid(any()) } returns setOf(Land_iso2.SE)

        val arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID)

        arbeidsland shouldContainExactlyInAnyOrder listOf(Land_iso2.DK)
    }

    @Test
    fun `hentAlleArbeidsland medArtikkel11_4_2AvklartArbeidslandOgSøknadsland girKunArbeidsland`() {
        lagBehandlingsresultat(lovvalgsperiode)
        every { avklartefaktaService.hentAlleAvklarteArbeidsland(any()) } returns setOf(Land_iso2.DK, Land_iso2.NO)
        søknad.soeknadsland.landkoder = listOf(Landkoder.DK.kode, Landkoder.SE.kode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2

        val arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID)

        arbeidsland shouldContainExactlyInAnyOrder listOf(Land_iso2.NO, Land_iso2.DK)
    }

    @Test
    fun `hentAlleArbeidsland returnererLovvalgslandKode nårBehandlingErAnmodningOmUnntak`() {
        mockMottatteOpplysninger()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        // Mock the specific behavior for this test
        every { avklartefaktaService.hentAlleAvklarteArbeidsland(any()) } returns setOf(Land_iso2.BE)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns lagBehandlingsresultat(lovvalgsperiode)

        val arbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID)

        arbeidsland shouldContainExactly listOf(Land_iso2.BE)
    }

    @Test
    fun `hentAlleArbeidsland returnererSøknadslandskoder dersomSøknadslandHarLandkoder`() {
        lagBehandlingsresultat(lovvalgsperiode)
        every { avklartefaktaService.hentAlleAvklarteArbeidsland(any()) } returns setOf(Land_iso2.DK, Land_iso2.NO)
        søknad.soeknadsland.landkoder = listOf(Landkoder.DK.kode, Landkoder.SE.kode)
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL

        val arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID)

        arbeidsland shouldContainExactlyInAnyOrder listOf(Land_iso2.NO, Land_iso2.DK)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt121 girSøknadsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        søknad.soeknadsland.landkoder = søknad.soeknadsland.landkoder + søknadsland.kode
        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)
        land shouldContainExactly listOf(søknadsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt121AvklartArbeidsland girAvklartArbeidsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        leggTilAlleAvklartArbeidsland(listOf(avklartArbeidsland))

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(avklartArbeidsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt122 girSøknadsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
        søknad.soeknadsland.landkoder = søknad.soeknadsland.landkoder + søknadsland.kode

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(søknadsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt161 girSøknadsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        søknad.soeknadsland.landkoder = søknad.soeknadsland.landkoder + søknadsland.kode

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(søknadsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt1142 girSøknadsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2
        søknad.soeknadsland.landkoder = søknad.soeknadsland.landkoder + søknadsland.kode

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(søknadsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt113A girOppgittBostedsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        // Set up søknadsland for this test
        søknad.soeknadsland = Soeknadsland(listOf(oppgittbostedsland.kode), false)

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(oppgittbostedsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt113AOgAvklartBosted overstyrerOppgittBosted`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        lovvalgsperiode.tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        every { avklartefaktaService.hentBostedland(any()) } returns Optional.of(avklartBostedsland)

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(Land_iso2.valueOf(avklartBostedsland.landkode))
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt113AUtenOppgittEllerAvkartBostedsland girTomListe`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        lovvalgsperiode.tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
        søknad.bosted.oppgittAdresse.landkode = null
        every { avklartefaktaService.hentBostedland(any()) } returns Optional.empty()

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)
        land shouldBe emptyList()
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt13BostedsadresseIkkeNorge`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
        søknad.bosted.oppgittAdresse.landkode = Landkoder.SE.kode
        // Set up søknadsland for this test
        søknad.soeknadsland = Soeknadsland(listOf(Landkoder.SE.kode), false)

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(Land_iso2.SE)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt13BostedsadresseNorge girSøknadsland`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
        søknad.bosted.oppgittAdresse.landkode = Landkoder.NO.kode
        søknad.soeknadsland.landkoder = søknad.soeknadsland.landkoder + søknadsland.kode

        val land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        land shouldContainExactly listOf(søknadsland)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland medArt13Videresending`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A

        søknad.arbeidPaaLand.fysiskeArbeidssteder = listOf(lagFysiskArbeidssted())
        søknad.foretakUtland = listOf(lagForetakUtland(Landkoder.ES))
        søknad.soeknadsland.landkoder = listOf(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString())

        val utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        utenlandskeTrygdemyndighetsland shouldHaveSize 2
        utenlandskeTrygdemyndighetsland shouldContainExactlyInAnyOrder listOf(Land_iso2.SE, Land_iso2.DK)

        verify { behandlingsresultatService.hentBehandlingsresultat(behandlingID) }
        verify(exactly = 3) { mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID) }
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland artikkel13IngenArbeidssted forventLand`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A

        søknad.soeknadsland.landkoder = listOf(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString())

        val utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        utenlandskeTrygdemyndighetsland shouldHaveSize 2
        utenlandskeTrygdemyndighetsland shouldContainExactlyInAnyOrder listOf(Land_iso2.SE, Land_iso2.DK)

        verify { behandlingsresultatService.hentBehandlingsresultat(behandlingID) }
        verify(exactly = 3) { mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID) }
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland artikkel13MedArbeidssted forventLand`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A

        søknad.arbeidPaaLand.fysiskeArbeidssteder = listOf(lagFysiskArbeidssted())
        søknad.foretakUtland = listOf(lagForetakUtland(Landkoder.ES))
        søknad.soeknadsland.landkoder = listOf(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString())

        val utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        utenlandskeTrygdemyndighetsland shouldHaveSize 2
        utenlandskeTrygdemyndighetsland shouldContainExactlyInAnyOrder listOf(Land_iso2.SE, Land_iso2.DK)

        verify { behandlingsresultatService.hentBehandlingsresultat(behandlingID) }
        verify(exactly = 3) { mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID) }
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland artikkel13MedArbeidsstedOgMarginaltArbeid forventLand`() {
        mockMottatteOpplysninger()
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A

        søknad.arbeidPaaLand.fysiskeArbeidssteder = listOf(lagFysiskArbeidssted())
        søknad.foretakUtland = listOf(lagForetakUtland(Landkoder.ES))
        søknad.soeknadsland.landkoder = listOf(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString())

        every { avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID) } returns setOf(Land_iso2.DK, Land_iso2.ES)

        val utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        utenlandskeTrygdemyndighetsland shouldHaveSize 2
        utenlandskeTrygdemyndighetsland shouldContainExactlyInAnyOrder listOf(Land_iso2.SE, Land_iso2.ES)

        verify { behandlingsresultatService.hentBehandlingsresultat(behandlingID) }
        verify(exactly = 3) { mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID) }
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland artikkel11_5DanmarkValgtAvSaksbehandler forventEttLandDanmark`() {
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        lovvalgsperiode.tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5

        every { avklartefaktaService.hentInformertMyndighet(behandlingID) } returns Optional.of(Land_iso2.DK)

        val utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        utenlandskeTrygdemyndighetsland shouldContainExactly listOf(Land_iso2.DK)
    }

    @Test
    fun `hentUtenlandskTrygdemyndighetsland artikkel11_5SaksbehandlerIkkeValgLand forventTomListe`() {
        lagBehandlingsresultat(lovvalgsperiode)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        lovvalgsperiode.tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5

        every { avklartefaktaService.hentInformertMyndighet(behandlingID) } returns Optional.empty()

        val utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)

        utenlandskeTrygdemyndighetsland shouldHaveSize 0
    }

    private fun lagUtenlandskAdresse(landkode: Landkoder): StrukturertAdresse {
        val utenlandskAdresse = StrukturertAdresse()
        utenlandskAdresse.landkode = landkode.toString()
        return utenlandskAdresse
    }

    private fun lagFysiskArbeidssted(): FysiskArbeidssted {
        return FysiskArbeidssted(null, lagUtenlandskAdresse(Landkoder.DE))
    }

    private fun lagForetakUtland(landkode: Landkoder): ForetakUtland {
        val foretakUtland = ForetakUtland()
        foretakUtland.adresse = lagUtenlandskAdresse(landkode)
        return foretakUtland
    }

    private fun mockMottatteOpplysninger() {
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.mottatteOpplysningerData = søknad
        mottatteOpplysninger.behandling = behandling
        every { mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID) } returns mottatteOpplysninger
    }

    private fun lagBehandlingMedSedDokument(): Behandling {
        val sedDokument = SedDokument()
        sedDokument.sedType = SedType.A001
        sedDokument.unntakFraLovvalgslandKode = Landkoder.BE
        sedDokument.lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))

        val saksopplysning = Saksopplysning()
        saksopplysning.dokument = sedDokument
        saksopplysning.type = SaksopplysningType.SEDOPPL

        val behandling = SaksbehandlingDataFactory.lagBehandling()
        behandling.tema = Behandlingstema.ARBEID_FLERE_LAND
        behandling.saksopplysninger.add(saksopplysning)
        return behandling
    }
}
