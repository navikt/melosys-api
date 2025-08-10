package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.OrienteringTilArbeidsgiverOmVedtakBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
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
internal class OrienteringTilArbeidsgiverOmVedtakMapperTest {

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

    private lateinit var orienteringTilArbeidsgiverOmVedtakMapper: OrienteringTilArbeidsgiverOmVedtakMapper

    private val orgnr1 = "111111111"
    private val orgnr2 = "222222222"
    private val orgnr3 = "123456789"
    private val orgnr4 = "444444444"
    private val uuid1 = "a2k2jf-a3khs"
    private val uuid2 = "0dkf93-kj701"

    @BeforeEach
    fun setup() {
        orienteringTilArbeidsgiverOmVedtakMapper = OrienteringTilArbeidsgiverOmVedtakMapper(
            mockDokgenMapperDatahenter,
            mockVilkaarsresultatService,
            mockAvklartefaktaService,
            mockVirksomheterService,
            mockLandvelgerService,
        )
    }

    @Test
    fun `Orienteringsbrev til arbeidsgiver om vedtak, innvilgelse`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A)
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.VESENTLIG_VIRKSOMHET) } returns Vilkaarsresultat().apply {
            isOppfylt = true
        }

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockAvklartefaktaService.hentAvklarteOrgnrOgUuid(ofType()) } returns setOf(orgnr1, orgnr2, orgnr3, orgnr4, uuid1, uuid2)
        every { mockLandvelgerService.hentArbeidsland(ofType()) } returns Land_iso2.DE


        val brevbestilling = OrienteringTilArbeidsgiverOmVedtakBrevbestilling.Builder()
            .medErInnvilgelse(true)
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Hei Test"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Hei Test"
            })
            .medBehandling(lagBehandling())
            .build()

        orienteringTilArbeidsgiverOmVedtakMapper.map(brevbestilling).run {
            navnVirksomhet.shouldBe("Bedrift AS")
            erInnvilgelse.shouldBeTrue()
            arbeidsland.shouldBe("Tyskland")
            periodeFom.shouldBe(LocalDate.of(2020, 1, 1))
            periodeTom.shouldBe(LocalDate.of(2021, 2, 1))
            erVesentligVirksomhetOppfyllt.shouldBeTrue()
            vesentligVirksomhetBegrunnelser.shouldBe(listOf())
        }
    }

    @Test
    fun `Orienteringsbrev til arbeidsgiver om vedtak, avslag`() {
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A)
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.VESENTLIG_VIRKSOMHET) } returns Vilkaarsresultat().apply {
            isOppfylt = true
            begrunnelser = setOf(
                VilkaarBegrunnelse().apply { kode = "KUN_ADMIN_ANSATTE" },
                VilkaarBegrunnelse().apply { kode = "FOR_LITE_OMSETNING_NORGE" },
                VilkaarBegrunnelse().apply { kode = "REKRUTTERER_ANSATTE_UTL" })
        }

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockAvklartefaktaService.hentAvklarteOrgnrOgUuid(ofType()) } returns setOf(orgnr1, orgnr2, orgnr3, orgnr4, uuid1, uuid2)
        every { mockLandvelgerService.hentArbeidsland(ofType()) } returns Land_iso2.DE


        val brevbestilling = OrienteringTilArbeidsgiverOmVedtakBrevbestilling.Builder()
            .medErInnvilgelse(false)
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Hei Test"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Hei Test"
            })
            .medBehandling(lagBehandling())
            .build()

        orienteringTilArbeidsgiverOmVedtakMapper.map(brevbestilling).run {
            navnVirksomhet.shouldBe("Bedrift AS")
            erInnvilgelse.shouldBeFalse()
            arbeidsland.shouldBe("Tyskland")
            periodeFom.shouldBe(LocalDate.of(2020, 1, 1))
            periodeTom.shouldBe(LocalDate.of(2021, 2, 1))
            erVesentligVirksomhetOppfyllt.shouldBeTrue()
            vesentligVirksomhetBegrunnelser.shouldBe(listOf("KUN_ADMIN_ANSATTE", "FOR_LITE_OMSETNING_NORGE", "REKRUTTERER_ANSATTE_UTL"))
        }
    }

    private fun lagBehandling(): Behandling = Behandling.forTest {
        id = 1L
        fagsak {
            type = Sakstyper.FTRL
        }
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.YRKESAKTIV
    }

    private fun lagBehandlingsResultat(lovvalgsbestemmelse: LovvalgBestemmelse): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = 1L
            behandling = lagBehandling()
            avklartefakta = setOf(Avklartefakta().apply {
                fakta = AvklartYrkesgruppeType.ORDINAER_UTEN_ART12.name
            })
            lovvalgsperioder = setOf(Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = lovvalgsbestemmelse
            })
        }
    }
}
