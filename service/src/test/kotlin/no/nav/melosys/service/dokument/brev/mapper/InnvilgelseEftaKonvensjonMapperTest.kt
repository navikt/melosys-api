package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.FakeUnleash
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Bostedsland
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.InnvilgelseEftaStorbritanniaBrevbestilling
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class InnvilgelseEftaKonvensjonMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockVilkaarsresultatService: VilkaarsresultatService

    @MockK
    private lateinit var mockVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var mockAvklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var mockLandvelgerService: LandvelgerService

    private lateinit var innvilgelseEftaStorbritanniaMapper: InnvilgelseEftaStorbritanniaMapper

    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        unleash.enableAll()
        innvilgelseEftaStorbritanniaMapper = InnvilgelseEftaStorbritanniaMapper(
            mockVilkaarsresultatService,
            mockDokgenMapperDatahenter,
            mockVirksomheterService,
            mockAvklartefaktaService,
            mockLandvelgerService,
            unleash
        )
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling, arbeid kun norge`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A)
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("NO")


        val brevbestilling =
            InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"

                })
                .medPersonMottaker(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land.av("NOR")
                    }
                })
                .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            navnVirksomhet.shouldBe("Bedrift AS")
            behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
            erArtikkel11_3_a_eller_13_3_a_arbeid_norge?.shouldBeTrue()
            erArtikkel13_3_a_eller_13_4?.shouldBeFalse()
            erArtikkel14_1_eller_14_2?.shouldBeFalse()
            erArtikkel16_1_eller_16_3?.shouldBeFalse()
            erArtikkel18_1?.shouldBeFalse()
            bosted.shouldBe("Norge")
            lovvalgsbestemmelse.shouldBe(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A.name)
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1)
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("GB")


        val brevbestilling =
            InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
                .medBehandling(lagBehandling())
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"

                })
                .medPersonMottaker(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land.av("GBR")
                    }
                })
                .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            navnVirksomhet.shouldBe("Bedrift AS")
            behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
            erArtikkel13_3_a_eller_13_4?.shouldBeFalse()
            erArtikkel14_1_eller_14_2?.shouldBeFalse()
            erArtikkel16_1_eller_16_3?.shouldBeFalse()
            erArtikkel18_1?.shouldBeTrue()
            bosted.shouldBe("Storbritannia")
            lovvalgsbestemmelse.shouldBe(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1.name)
        }
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}): Behandling = Behandling().apply behandling@{
        id = 1L
        fagsak = FagsakTestFactory.builder().apply {
            type = Sakstyper.FTRL
            leggTilBehandling(this@behandling)
        }.build()
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.YRKESAKTIV
        block()
    }

    private fun lagBehandlingsResultat(lovvalgsbestemmelse: LovvalgBestemmelse): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = 1L
            behandling = lagBehandling()
            avklartefakta = setOf(Avklartefakta().apply {
                fakta = AvklartYrkesgruppeType.ORDINAER.name
                type = Avklartefaktatyper.YRKESGRUPPE
            })
            lovvalgsperioder = setOf(no.nav.melosys.domain.Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = lovvalgsbestemmelse
            })
        }
    }
}
