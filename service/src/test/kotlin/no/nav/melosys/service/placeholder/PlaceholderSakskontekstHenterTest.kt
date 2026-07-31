package no.nav.melosys.service.placeholder

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class PlaceholderSakskontekstHenterTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var landnavnOppslag: PlaceholderLandnavnOppslag

    private lateinit var henter: PlaceholderSakskontekstHenter

    @BeforeEach
    fun setup() {
        henter = PlaceholderSakskontekstHenter(
            behandlingService,
            behandlingsresultatService,
            landvelgerService,
            avklarteVirksomheterService,
            landnavnOppslag,
        )
        medBehandling(behandling())
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(BEHANDLING_ID) } returns behandlingsresultat()
        every { landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(BEHANDLING_ID) } returns listOf(Land_iso2.DE)
        every { landnavnOppslag.landnavn(Land_iso2.DE.kode) } returns "Tyskland"
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns listOf(utenlandskVirksomhet("Nordwerk GmbH"))
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any()) } returns setOf("123456789")
    }

    @Test
    fun `materialiserer saken, periodene, arbeidslandet og arbeidsgiverne`() {
        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.saksnummer shouldBe "MEL-12345"
        sakskontekst.brukersAktørID shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
        sakskontekst.erLovvalg shouldBe false
        sakskontekst.medlemskapsperiodeFom shouldBe LocalDate.of(2023, 1, 1)
        sakskontekst.medlemskapsperiodeTom shouldBe LocalDate.of(2027, 2, 28)
        sakskontekst.avgiftspliktigPerioder shouldContainExactly listOf(
            PeriodeData(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28), erInnvilget = true),
            PeriodeData(LocalDate.of(2023, 1, 1), LocalDate.of(2024, 2, 29), erInnvilget = true),
            PeriodeData(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), erInnvilget = false),
        )
        sakskontekst.arbeidsland shouldContainExactly listOf("Tyskland")
        sakskontekst.utenlandskeArbeidsgivere shouldContainExactly listOf("Nordwerk GmbH")
        sakskontekst.norskeArbeidsgivereOrgnumre shouldContainExactly setOf("123456789")
    }

    @Test
    fun `soknadsperioden tar med den samlede utsendingsperioden naar den er ulik`() {
        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.soknadsperioder shouldContainExactly listOf(
            PeriodeData(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28)),
            PeriodeData(LocalDate.of(2024, 4, 1), LocalDate.of(2027, 3, 31)),
        )
    }

    @Test
    fun `lik utsendingsperiode gir bare en soknadsperiode`() {
        medBehandling(behandling(utsendingsperiode = Periode(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28))))

        henter.hent(BEHANDLING_ID).soknadsperioder shouldContainExactly listOf(
            PeriodeData(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28)),
        )
    }

    @Test
    fun `lovvalgssaker henter lovvalgsperioden som eneste avgiftspliktige periode`() {
        medBehandling(behandling(sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG))
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(BEHANDLING_ID) } returns
            Behandlingsresultat.forTest {
                behandling { fagsak { tema = Sakstemaer.MEDLEMSKAP_LOVVALG } }
                lovvalgsperiode {
                    fom = LocalDate.of(2024, 3, 1)
                    tom = LocalDate.of(2027, 2, 28)
                }
            }

        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.erLovvalg shouldBe true
        sakskontekst.lovvalgsperiode shouldBe PeriodeData(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28), erInnvilget = true)
        sakskontekst.avgiftspliktigPerioder shouldContainExactly listOf(sakskontekst.lovvalgsperiode!!)
        // Periodene er lovvalgsperioder, så medlemskapsnøklene skal ikke få dem under sitt navn
        sakskontekst.medlemskapsperioder().shouldBeNull()
    }

    @Test
    fun `arbeidslandene sorteres paa landkode slik at forhaandsvalget er det samme hver gang`() {
        every { landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(BEHANDLING_ID) } returns
            setOf(Land_iso2.SE, Land_iso2.DE)
        every { landnavnOppslag.landnavn(Land_iso2.SE.kode) } returns "Sverige"

        henter.hent(BEHANDLING_ID).arbeidsland shouldContainExactly listOf("Tyskland", "Sverige")
    }

    @Test
    fun `et deloppslag som feiler utelater bare sitt eget felt`() {
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(BEHANDLING_ID) } throws
            IkkeFunnetException("Fant ikke behandlingsresultat")
        every { landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(BEHANDLING_ID) } throws
            IllegalStateException("Landvelgeren feilet")

        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.saksnummer shouldBe "MEL-12345"
        sakskontekst.lovvalgsperiode.shouldBeNull()
        sakskontekst.medlemskapsperiodeFom.shouldBeNull()
        sakskontekst.avgiftspliktigPerioder.shouldBeEmpty()
        sakskontekst.arbeidsland.shouldBeEmpty()
        sakskontekst.soknadsperioder.shouldContainExactly(
            listOf(
                PeriodeData(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28)),
                PeriodeData(LocalDate.of(2024, 4, 1), LocalDate.of(2027, 3, 31)),
            )
        )
        sakskontekst.utenlandskeArbeidsgivere shouldContainExactly listOf("Nordwerk GmbH")
    }

    private fun medBehandling(behandling: Behandling) {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
    }

    private fun behandling(
        utsendingsperiode: Periode = Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2027, 3, 31)),
        sakstema: Sakstemaer = Sakstemaer.TRYGDEAVGIFT,
    ): Behandling = Behandling.forTest {
        id = BEHANDLING_ID
        fagsak {
            saksnummer = "MEL-12345"
            tema = sakstema
            medBruker()
        }
        mottatteOpplysninger {
            soeknad { periode(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28)) }
        }
    }.apply {
        (hentMottatteOpplysninger().mottatteOpplysningerData as Soeknad).utenlandsoppdraget.samletUtsendingsperiode = utsendingsperiode
    }

    // Trygdeavgiftssak: finnAvgiftspliktigPerioder() gir medlemskapsperiodene
    private fun behandlingsresultat(): Behandlingsresultat = Behandlingsresultat.forTest {
        behandling { fagsak { tema = Sakstemaer.TRYGDEAVGIFT } }
        medlemskapsperiode {
            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2024, 2, 29)
        }
        medlemskapsperiode {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2027, 2, 28)
        }
        medlemskapsperiode {
            fom = LocalDate.of(2020, 1, 1)
            tom = LocalDate.of(2020, 12, 31)
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }
    }

    private fun utenlandskVirksomhet(navn: String) =
        AvklartVirksomhet(navn, null, null, Yrkesaktivitetstyper.LOENNET_ARBEID)

    private companion object {
        const val BEHANDLING_ID = 1234L
    }
}
