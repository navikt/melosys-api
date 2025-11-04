package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.getunleash.FakeUnleash
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory.SKATTEPLIKTTYPE
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringIntervall
import no.nav.melosys.saksflyt.steg.fakturering.OpprettFakturaserie
import no.nav.melosys.saksflyt.steg.fakturering.OpprettFakturaserie.Companion.DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockKExtension::class)
class OpprettFakturaserieTest {

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    @RelaxedMockK
    lateinit var pdlService: PersondataService

    @RelaxedMockK
    lateinit var trygdeavgiftService: TrygdeavgiftService

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService
    private lateinit var opprettFakturaserie: OpprettFakturaserie

    private val slotFakturaserieDto = slot<FakturaserieDto>()
    private val unleash = FakeUnleash()
    private val inneværendeÅr = LocalDate.now().year
    private val foregåendeÅr = inneværendeÅr - 1

    @BeforeEach
    internal fun setUp() {
        unleash.enableAll()
        slotFakturaserieDto.clear()
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(behandlingsresultatService)

        opprettFakturaserie = OpprettFakturaserie(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenConsumer,
            pdlService,
            trygdeavgiftService,
            unleash
        )
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBeNull()
        }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier for eøs pensjonister når inntektskilde type er PENSJON`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2023, 1, 1)
                tomDato = LocalDate.of(2023, 5, 1)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.PENSJONIST
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }


        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns Optional.of(FagsakTestFactory.BRUKER_AKTØR_ID)

        opprettFakturaserie.utfør(prosessinstans)

        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Informasjon om trygdeavgift datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldNotContain("Dekning")
            perioder.single().beskrivelse.shouldContain("Inntekt: 5000.0")
            perioder.single().beskrivelse.shouldContain("Sats: 3.5 %")
        }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier for eøs pensjonister - en periode skal ikke forskuddsfaktureres`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2023, 1, 1)
                tomDato = LocalDate.of(2023, 5, 1)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON
                    }
                }
                trygdeavgiftsperiode {
                    forskuddsvisFaktura = false
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.PENSJONIST
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Informasjon om trygdeavgift datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldNotContain("Dekning")
            perioder.single().beskrivelse.shouldContain("Inntekt: 5000.0")
            perioder.single().beskrivelse.shouldContain("Sats: 3.5 %")
        }
    }

    @Test
    fun `Ikke opprett faktura da ingen trygdeavgiftsperioder skal forskuddsfaktureres`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                    }
                    forskuddsvisFaktura = false
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }


        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify { faktureringskomponentenConsumer wasNot Called }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier når Inntektskildetype er PENSJON_UFØRETRYGD`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldContain("Dekning: $DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL")
        }
    }

    @Test
    fun `Opprett betalingsplan med riktige verdier når Inntektskildetype er PENSJON_UFØRETRYGD_KILDESKATT`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBeNull()
            perioder.single().beskrivelse.shouldContain("Dekning: $DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL")
        }
    }

    @Test
    fun `Kanseller betaling når resultat er opphørt`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.OPPHØRT
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)
        every {
            faktureringskomponentenConsumer.kansellerFakturaserie(
                FAKTURASERIE_REFERANSE,
                SAKSBEHANDLER_IDENT
            )
        } returns NyFakturaserieResponseDto(
            NY_FAKTURASERIE_REFERANSE
        )


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
        verify {
            behandlingsresultatService.lagre(
                match {
                    it.fakturaserieReferanse == NY_FAKTURASERIE_REFERANSE
                }
            )
        }
    }

    @Test
    fun `Kanseller betaling når manglende innbetaling resulterer i fjerning av trygdeavgift`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(inneværendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(inneværendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                status = Behandlingsstatus.AVSLUTTET
                fagsak = opprinneligBehandlingsresultat.behandling?.fagsak
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.kansellerFakturaserie(FAKTURASERIE_REFERANSE, SAKSBEHANDLER_IDENT) } returns
            NyFakturaserieResponseDto(NY_FAKTURASERIE_REFERANSE)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
        verify {
            behandlingsresultatService.lagre(
                match {
                    it.fakturaserieReferanse == NY_FAKTURASERIE_REFERANSE
                }
            )
        }
    }

    @Test
    fun `Kanseller betaling når ny vurdering resulterer i fjerning av trygdeavgift`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
            }
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(inneværendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.kansellerFakturaserie(FAKTURASERIE_REFERANSE, SAKSBEHANDLER_IDENT) } returns
            NyFakturaserieResponseDto(NY_FAKTURASERIE_REFERANSE)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
        verify {
            behandlingsresultatService.lagre(
                match {
                    it.fakturaserieReferanse == NY_FAKTURASERIE_REFERANSE
                }
            )
        }
    }

    @Test
    fun `Skal ikke kansellere fakturaserie hvis det finnes en ÅRSAVREGNING`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(inneværendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        Behandling.forTest {
            id = 3L
            type = Behandlingstyper.ÅRSAVREGNING
            fagsak = opprinneligBehandlingsresultat.behandling?.fagsak
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = opprinneligBehandlingsresultat.behandling?.fagsak
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.kansellerFakturaserie(FAKTURASERIE_REFERANSE, SAKSBEHANDLER_IDENT) } returns
            NyFakturaserieResponseDto(NY_FAKTURASERIE_REFERANSE)


        opprettFakturaserie.utfør(prosessinstans)


        verify { faktureringskomponentenConsumer wasNot Called }
    }

    @Test
    fun `Ikke kanseller betaling når resultat er ny vurdering og trygdeavgift betales til NAV`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(inneværendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(2022, 1, 1)
                    periodeTil = LocalDate.of(2023, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = opprinneligBehandlingsresultat.behandling?.fagsak
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.kansellerFakturaserie(eq(FAKTURASERIE_REFERANSE), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Opprett betalingsplan for ny vurdering`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(inneværendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(inneværendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        Behandling.forTest {
            id = 0L
            registrertDato = Instant.EPOCH
            fagsak = opprinneligBehandlingsresultat.behandling!!.fagsak
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(2022, 1, 1)
                    periodeTil = LocalDate.of(2023, 5, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = opprinneligBehandlingsresultat.behandling!!.fagsak
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fakturaserieReferanse.shouldBe(FAKTURASERIE_REFERANSE)
        slotFakturaserieDto.captured.perioder.apply {
            shouldHaveSize(1)
            forAll { periode ->
                periode.startDato shouldBe LocalDate.of(2022, 1, 1)
                periode.sluttDato shouldBe LocalDate.of(2023, 5, 31)
                periode.enhetsprisPerManed shouldBe BigDecimal.valueOf(5000)
                periode.beskrivelse shouldBe "Inntekt: 5000.0, Dekning: Helsedel, Sats: 3.5 %"
            }
        }
    }

    @Test
    fun `Opprett betalingsplan kun for trygdeavgiftsperioder med avgift større enn 0`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(2023, 1, 1)
                    periodeTil = LocalDate.of(2023, 5, 31)
                }
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(0.0)
                    trygdesats = BigDecimal(0)
                    periodeFra = LocalDate.of(2023, 6, 1)
                    periodeTil = LocalDate.of(2023, 12, 31)
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()

                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        behandlingsresultat.trygdeavgiftsperioder.size.shouldBe(2)

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.perioder.size.shouldBe(1)
    }

    @Test
    fun `Ikke opprett betalingsplan når behandling ikke har trygdeavgiftsperioder med avgift`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(0.0)
                    trygdesats = BigDecimal(0)
                    periodeFra = LocalDate.of(2023, 6, 1)
                    periodeTil = LocalDate.of(2023, 12, 31)
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()

                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        behandlingsresultat.trygdeavgiftsperioder.size.shouldBe(1)

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.lagFakturaserie(any(), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Ikke opprett betalingsplan når trygdeavgift betales til skatt`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(2023, 6, 1)
                    periodeTil = LocalDate.of(2023, 12, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = SKATTEPLIKTTYPE
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()

                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 0) { faktureringskomponentenConsumer.lagFakturaserie(any(), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `Opprett betalingsplan med organisasjon-fullmektig sender fullmektig`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(2023, 6, 1)
                    periodeTil = LocalDate.of(2023, 12, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = SKATTEPLIKTTYPE
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    aktører(
                        setOf(
                            Aktoer().apply {
                                orgnr = FULLMEKTIG_IDENT
                                rolle = Aktoersroller.FULLMEKTIG
                                setFullmaktstype(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
                            },
                            Aktoer().apply {
                                aktørId = BRUKER_AKTØR_ID
                                rolle = Aktoersroller.BRUKER
                            })
                    )
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fullmektig?.organisasjonsnummer.shouldBe(FULLMEKTIG_IDENT)
        slotFakturaserieDto.captured.fullmektig?.fodselsnummer.shouldBeNull()
    }

    @Test
    fun `Opprett betalingsplan med person-fullmektig sender fullmektig`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2023, 5, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(2023, 6, 1)
                    periodeTil = LocalDate.of(2023, 12, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = SKATTEPLIKTTYPE
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    aktører(
                        setOf(
                            Aktoer().apply {
                                personIdent = FULLMEKTIG_IDENT
                                rolle = Aktoersroller.FULLMEKTIG
                                setFullmaktstype(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
                            },
                            Aktoer().apply {
                                aktørId = BRUKER_AKTØR_ID
                                rolle = Aktoersroller.BRUKER
                            })
                    )
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.fullmektig?.fodselsnummer.shouldBe(FULLMEKTIG_IDENT)
        slotFakturaserieDto.captured.fullmektig?.organisasjonsnummer.shouldBeNull()
    }

    @Test
    fun `Opprett betalingsplan for pensjonister som ønsker faktura - BETALINGSVALG er FAKTURA`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2023, 1, 1)
                tomDato = LocalDate.of(2023, 5, 1)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.PENSJONIST
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)

        opprettFakturaserie.utfør(prosessinstans)

        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        slotFakturaserieDto.captured.shouldNotBeNull()
        slotFakturaserieDto.captured.perioder.apply {
            shouldHaveSize(1)
            forAll { periode ->
                periode.startDato shouldBe LocalDate.of(2023, 1, 1)
                periode.sluttDato shouldBe LocalDate.of(2023, 12, 31)
                periode.enhetsprisPerManed shouldBe BigDecimal.valueOf(5000)
                periode.beskrivelse shouldBe "Inntekt: 5000.0, Sats: 3.5 %"
            }
        }
    }

    @Test
    fun `Ikke opprett betalingsplan for pensjonister - BETALINGSVALG er TREKK`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2023, 1, 1)
                tomDato = LocalDate.of(2023, 5, 1)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    grunnlagInntekstperiode {
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                tema = Behandlingstema.PENSJONIST
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                    betalingsvalg = Betalingstype.TREKK
                    medBruker()
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify { faktureringskomponentenConsumer wasNot Called }
    }

    @Test
    fun `Ny vurdering som setter medlemskapsperioder til kun foregående år skal sende tomme perioder til faktureringskomponent ved tidligere fakturering`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()

                }
            }
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(foregåendeÅr, 1, 1)
                tom = LocalDate.of(inneværendeÅr, 12, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(foregåendeÅr, 1, 1)
                    periodeTil = LocalDate.of(inneværendeÅr, 12, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(foregåendeÅr, 1, 1)
                tom = LocalDate.of(foregåendeÅr, 12, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = opprinneligBehandlingsresultat.behandling!!.fagsak
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)
        every { faktureringskomponentenConsumer.lagFakturaserie(any(), SAKSBEHANDLER_IDENT) } returns
            NyFakturaserieResponseDto(FAKTURASERIE_REFERANSE)


        opprettFakturaserie.utfør(prosessinstans)


        verify(exactly = 1) { faktureringskomponentenConsumer.lagFakturaserie(capture(slotFakturaserieDto), eq(SAKSBEHANDLER_IDENT)) }
        verify {
            behandlingsresultatService.lagre(
                match {
                    it.fakturaserieReferanse == FAKTURASERIE_REFERANSE
                }
            )
        }

        slotFakturaserieDto.captured.shouldNotBeNull().run {
            referanseBruker.shouldContain("Vedtak om medlemskap datert ")
            fakturaserieReferanse.shouldBe(FAKTURASERIE_REFERANSE)
            perioder.shouldBeEmpty()
        }
    }

    @Test
    fun `Ny vurdering og førstegangsbehandling på kun foregående år skal ikke opprette fakturering `() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = OPPRINNELIG_BEHANDLING_ID
            fakturaserieReferanse = FAKTURASERIE_REFERANSE
            behandling = Behandling.forTest {
                id = OPPRINNELIG_BEHANDLING_ID
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    betalingsvalg = Betalingstype.FAKTURA
                    medBruker()

                }
            }
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(foregåendeÅr, 1, 1)
                tom = LocalDate.of(foregåendeÅr, 12, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                trygdeavgiftsperiode {
                    trygdeavgiftsbeløpMd = BigDecimal(5000.0)
                    trygdesats = BigDecimal(3.5)
                    periodeFra = LocalDate.of(foregåendeÅr, 1, 1)
                    periodeTil = LocalDate.of(foregåendeÅr, 12, 31)
                    grunnlagInntekstperiode {
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperiode {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                fom = LocalDate.of(foregåendeÅr, 1, 1)
                tom = LocalDate.of(foregåendeÅr, 10, 31)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            }
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                vedtaksdato = Instant.now().minus(3, ChronoUnit.DAYS)
            }
            behandling {
                id = BEHANDLING_ID
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                fagsak = opprinneligBehandlingsresultat.behandling!!.fagsak
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SAKSBEHANDLER, "S123456")
            medData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)
            medBehandling(behandlingsresultat.behandling)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) } returns opprinneligBehandlingsresultat
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(opprinneligBehandlingsresultat) } returns true
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandlingsresultat.behandling
        every { pdlService.finnFolkeregisterident(BRUKER_AKTØR_ID) } returns Optional.of(BRUKER_AKTØRID)


        opprettFakturaserie.utfør(prosessinstans)


        verify { faktureringskomponentenConsumer wasNot Called }
    }

    companion object {
        const val SAKSBEHANDLER_IDENT = "S123456"
        const val BEHANDLING_ID = 1L
        const val OPPRINNELIG_BEHANDLING_ID = 2L
        const val BRUKER_AKTØRID = "12345678911"
        const val FULLMEKTIG_IDENT = "123456789"
        const val FAKTURASERIE_REFERANSE = "1234"
        const val NY_FAKTURASERIE_REFERANSE = "56789"
    }
}
