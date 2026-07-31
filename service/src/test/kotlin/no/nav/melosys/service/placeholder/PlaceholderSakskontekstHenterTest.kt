package no.nav.melosys.service.placeholder

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.arbeidsforholdDokument
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.saksopplysning
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
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
            // Ekte tjeneste: mottakeren utledes rent av trygdeavgiftsperiodene, uten databaseoppslag
            TrygdeavgiftMottakerService(behandlingsresultatService),
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
    fun `arbeidsgiverne faller tilbake til soknadens oppgitte naar ingenting er avklart enda`() {
        medBehandling(
            behandling(
                arbeidsforholdOrgnumre = listOf("999888777"),
                ekstraArbeidsgivere = listOf("111222333"),
                utenlandskeForetaksnavn = listOf("Nordwerk GmbH"),
            )
        )
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any()) } returns emptySet()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.norskeArbeidsgivereOrgnumre shouldContainExactlyInAnyOrder setOf("999888777", "111222333")
        sakskontekst.utenlandskeArbeidsgivere shouldContainExactly listOf("Nordwerk GmbH")
    }

    @Test
    fun `avklarte arbeidsgivere gjelder foran soknadens oppgitte`() {
        medBehandling(
            behandling(
                arbeidsforholdOrgnumre = listOf("999888777"),
                ekstraArbeidsgivere = listOf("111222333"),
                utenlandskeForetaksnavn = listOf("Nordwerk GmbH", "Sydwerk GmbH"),
            )
        )

        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.norskeArbeidsgivereOrgnumre shouldContainExactly setOf("123456789")
        sakskontekst.utenlandskeArbeidsgivere shouldContainExactly listOf("Nordwerk GmbH")
    }

    @Test
    fun `uten avklarte og uten oppgitte arbeidsgivere blir feltene tomme`() {
        every { avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any()) } returns emptySet()
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        val sakskontekst = henter.hent(BEHANDLING_ID)

        sakskontekst.norskeArbeidsgivereOrgnumre.shouldBeEmpty()
        sakskontekst.utenlandskeArbeidsgivere.shouldBeEmpty()
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

    @Test
    fun `materialiserer betingelsesfakta fra behandlingen og behandlingsresultatet`() {
        val fakta = henter.hent(BEHANDLING_ID).fakta

        fakta.erInnvilgelse shouldBe false
        fakta.erAvslag shouldBe false
        fakta.erOpphørt shouldBe false
        fakta.erDelvisInnvilgelse shouldBe false
        fakta.harÅpenSluttdato shouldBe false
        fakta.erUtsending shouldBe false
        fakta.erPensjonist shouldBe false
        fakta.erFørstegangsvurdering shouldBe true
        fakta.erNyVurdering shouldBe false
        // Uten trygdeavgiftsperioder er verken skatteplikt, inntektskilde eller mottaker kjent
        fakta.erSkattepliktig.shouldBeNull()
        fakta.harLønnFraNorge.shouldBeNull()
        fakta.harInntektFraUtlandet.shouldBeNull()
        fakta.trygdeavgiftTilSkatt.shouldBeNull()
    }

    // Åpen sluttdato er en påstand om de innvilgede periodene – uten dem finnes ingen påstand
    @Test
    fun `avslag uten innvilgede perioder utelater apen sluttdato`() {
        medBehandlingsresultat(
            Behandlingsresultat.forTest {
                behandling { fagsak { tema = Sakstemaer.TRYGDEAVGIFT } }
                medlemskapsperiode {
                    fom = LocalDate.of(2024, 3, 1)
                    tom = null
                    innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
                }
            }
        )

        henter.hent(BEHANDLING_ID).fakta.harÅpenSluttdato.shouldBeNull()
    }

    @Test
    fun `behandlingens tema og type gir utsending, pensjonist og ny vurdering`() {
        medBehandling(behandling(behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER, behandlingstype = Behandlingstyper.NY_VURDERING))

        val fakta = henter.hent(BEHANDLING_ID).fakta

        fakta.erUtsending shouldBe true
        fakta.erPensjonist shouldBe false
        fakta.erFørstegangsvurdering shouldBe false
        fakta.erNyVurdering shouldBe true
    }

    @Test
    fun `innvilget periode uten sluttdato gir apen sluttdato`() {
        medBehandlingsresultat(
            Behandlingsresultat.forTest {
                behandling { fagsak { tema = Sakstemaer.TRYGDEAVGIFT } }
                medlemskapsperiode {
                    fom = LocalDate.of(2024, 3, 1)
                    tom = null
                }
            }
        )

        henter.hent(BEHANDLING_ID).fakta.harÅpenSluttdato shouldBe true
    }

    @Test
    fun `trygdeavgiftsperiodene gir skatteplikt, inntektskilde og trygdeavgift til skatt`() {
        medBehandlingsresultat(medTrygdeavgift(Skatteplikttype.SKATTEPLIKTIG, Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, tilSkatt = true))

        val fakta = henter.hent(BEHANDLING_ID).fakta

        fakta.erSkattepliktig shouldBe true
        fakta.harLønnFraNorge shouldBe true
        fakta.harInntektFraUtlandet shouldBe false
        fakta.trygdeavgiftTilSkatt shouldBe true
    }

    @Test
    fun `ikke skattepliktig inntekt fra utlandet gir trygdeavgift til Nav`() {
        medBehandlingsresultat(medTrygdeavgift(Skatteplikttype.IKKE_SKATTEPLIKTIG, Inntektskildetype.INNTEKT_FRA_UTLANDET, tilSkatt = false))

        val fakta = henter.hent(BEHANDLING_ID).fakta

        fakta.erSkattepliktig shouldBe false
        fakta.harLønnFraNorge shouldBe false
        fakta.harInntektFraUtlandet shouldBe true
        fakta.trygdeavgiftTilSkatt shouldBe false
    }

    @Test
    fun `delvis innvilgelse leses av svaret pa anmodningsperioden`() {
        medBehandlingsresultat(medAnmodningsperioder(1))

        henter.hent(BEHANDLING_ID).fakta.erDelvisInnvilgelse shouldBe true
    }

    @Test
    fun `flere anmodningsperioder utelater delvis innvilgelse uten a velte de ovrige faktaene`() {
        medBehandlingsresultat(medAnmodningsperioder(2))

        val fakta = henter.hent(BEHANDLING_ID).fakta

        fakta.erDelvisInnvilgelse.shouldBeNull()
        fakta.erOpphørt shouldBe false
    }

    @Test
    fun `uten behandlingsresultat star bare behandlingens egne fakta igjen`() {
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(BEHANDLING_ID) } throws
            IkkeFunnetException("Fant ikke behandlingsresultat")

        val fakta = henter.hent(BEHANDLING_ID).fakta

        fakta.erInnvilgelse.shouldBeNull()
        fakta.erAvslag.shouldBeNull()
        fakta.erDelvisInnvilgelse.shouldBeNull()
        fakta.harÅpenSluttdato.shouldBeNull()
        fakta.erFørstegangsvurdering shouldBe true
    }

    private fun medBehandling(behandling: Behandling) {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
    }

    private fun medBehandlingsresultat(behandlingsresultat: Behandlingsresultat) {
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(BEHANDLING_ID) } returns behandlingsresultat
    }

    private fun medTrygdeavgift(
        skatteplikt: Skatteplikttype,
        inntektskilde: Inntektskildetype,
        tilSkatt: Boolean,
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        behandling { fagsak { tema = Sakstemaer.TRYGDEAVGIFT } }
        medlemskapsperiode {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2027, 2, 28)
            trygdeavgiftsperiode {
                grunnlagSkatteforholdTilNorge { skatteplikttype = skatteplikt }
                grunnlagInntekstperiode {
                    type = inntektskilde
                    arbeidsgiversavgiftBetalesTilSkatt = tilSkatt
                }
            }
        }
    }

    private fun medAnmodningsperioder(antall: Int): Behandlingsresultat = Behandlingsresultat.forTest {
        behandling { fagsak { tema = Sakstemaer.MEDLEMSKAP_LOVVALG } }
        repeat(antall) { nummer ->
            anmodningsperiode {
                fom = LocalDate.of(2024, 3, 1).plusYears(nummer.toLong())
                anmodningsperiodeSvar {
                    anmodningsperiodeSvarType = Anmodningsperiodesvartyper.DELVIS_INNVILGELSE
                    innvilgetFom = LocalDate.of(2024, 3, 1)
                    innvilgetTom = LocalDate.of(2025, 2, 28)
                }
            }
        }
    }

    private fun behandling(
        utsendingsperiode: Periode = Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2027, 3, 31)),
        sakstema: Sakstemaer = Sakstemaer.TRYGDEAVGIFT,
        behandlingstema: Behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
        behandlingstype: Behandlingstyper = Behandlingstyper.FØRSTEGANG,
        arbeidsforholdOrgnumre: List<String> = emptyList(),
        ekstraArbeidsgivere: List<String> = emptyList(),
        utenlandskeForetaksnavn: List<String> = emptyList(),
    ): Behandling = Behandling.forTest {
        id = BEHANDLING_ID
        tema = behandlingstema
        type = behandlingstype
        fagsak {
            saksnummer = "MEL-12345"
            tema = sakstema
            medBruker()
        }
        mottatteOpplysninger {
            soeknad {
                periode(LocalDate.of(2024, 3, 1), LocalDate.of(2027, 2, 28))
                ekstraArbeidsgivere.forEach { ekstraArbeidsgiver(it) }
                utenlandskeForetaksnavn.forEachIndexed { indeks, foretaksnavn ->
                    foretakUtlandMedDetaljer {
                        navn = foretaksnavn
                        uuid = "uuid-$indeks"
                    }
                }
            }
        }
        if (arbeidsforholdOrgnumre.isNotEmpty()) {
            saksopplysning {
                type = SaksopplysningType.ARBFORH
                arbeidsforholdDokument {
                    arbeidsforholdOrgnumre.forEach { orgnr ->
                        arbeidsforhold {
                            arbeidsgivertype = Aktoertype.ORGANISASJON
                            arbeidsgiverID = orgnr
                        }
                    }
                }
            }
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
