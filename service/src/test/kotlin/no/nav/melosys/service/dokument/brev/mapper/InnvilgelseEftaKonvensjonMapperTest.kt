package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.FakeUnleash
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.InnvilgelseEftaStorbritanniaBrevbestilling
import no.nav.melosys.domain.brev.OrienteringAnmodningUnntakBrevbestilling
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser.UTSENDELSE_OVER_24_MN
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_naeringsdrivende_begrunnelser.IKKE_LIGNENDE_VIRKSOMHET
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

    private val orgnr1 = "111111111"
    private val orgnr2 = "222222222"
    private val orgnr3 = "123456789"
    private val orgnr4 = "444444444"
    private val uuid1 = "a2k2jf-a3khs"
    private val uuid2 = "0dkf93-kj701"

    @BeforeEach
    fun setup() {
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
    fun `Innvilgelse efta Storbritannia brevbestilling`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat()
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
        every { mockAvklartefaktaService.hentAvklarteOrgnrOgUuid(ofType()) } returns setOf(orgnr1, orgnr2, orgnr3, orgnr4, uuid1, uuid2)
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

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = 1L
            behandling = lagBehandling()
            avklartefakta = setOf(Avklartefakta().apply {
                fakta = AvklartYrkesgruppeType.ORDINAER_UTEN_ART12.name
            })
            lovvalgsperioder = setOf(no.nav.melosys.domain.Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            })
        }
    }
}
