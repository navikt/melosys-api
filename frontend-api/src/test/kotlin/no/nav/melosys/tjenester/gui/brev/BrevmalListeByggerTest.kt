package no.nav.melosys.tjenester.gui.brev

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.buildWithDefaults
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Distribusjonstype
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.brev.BrevAdresse
import no.nav.melosys.service.brev.BrevmalListeService
import no.nav.melosys.service.brev.bestilling.HentBrevAdresseTilMottakereService
import no.nav.melosys.service.brev.bestilling.HentMuligeProduserbaredokumenterService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.tjenester.gui.dto.brev.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(MockKExtension::class)
internal class BrevmalListeByggerTest {
    @MockK
    private lateinit var hentBrevAdresseTilMottakereService: HentBrevAdresseTilMottakereService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    private lateinit var brevmalListeBygger: BrevmalListeBygger

    @BeforeEach
    fun init() {
        val hentMuligeProduserbaredokumenterService = HentMuligeProduserbaredokumenterService(behandlingService)
        val brevmalListeService = BrevmalListeService(hentMuligeProduserbaredokumenterService, hentBrevAdresseTilMottakereService)
        brevmalListeBygger = BrevmalListeBygger(
            brevmalListeService,
            behandlingService,
            saksbehandlingRegler,
            utenlandskMyndighetService
        )
        every { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(any<Long>(), any()) } returns emptyList()
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
    }

    @Test
    fun byggBrevmalDtoListe_brukerErHovedpart_returnererTilgjengeligeMaler() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(5).run {
            map { it.mottaker.type }
                .shouldContainExactly(
                    MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse,
                    MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.beskrivelse,
                    MottakerType.UTENLANDSK_TRYGDEMYNDIGHET.beskrivelse,
                    MottakerType.ANNEN_ORGANISASJON.beskrivelse,
                    MottakerType.NORSK_MYNDIGHET.beskrivelse
                )
            first().brevTyper
                .shouldHaveSize(3)
                .map(BrevmalTypeDto::getType)
                .shouldContainExactly(
                    Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                    Produserbaredokumenter.MANGELBREV_BRUKER,
                    Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
                )
            elementAt(1).brevTyper
                .shouldHaveSize(2)
                .map(BrevmalTypeDto::getType)
                .shouldContainExactly(
                    Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
                    Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER
                )
            elementAt(2).brevTyper
                .shouldHaveSize(1)
                .single().type
                .shouldBe(Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV)
            elementAt(3).brevTyper
                .shouldHaveSize(2)
                .map(BrevmalTypeDto::getType)
                .shouldContainExactly(
                    Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
                    Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER
                )
            elementAt(4).brevTyper
                .shouldHaveSize(1)
                .single().type
                .shouldBe(Produserbaredokumenter.FRITEKSTBREV)
        }
    }

    @Test
    fun byggBrevmalDtoListe_virksomhetErHovedpart_returnererTilgjengeligeMaler() {
        val behandling = lagBehandling(aktoer = Aktoer().apply { rolle = Aktoersroller.VIRKSOMHET })
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(2).run {
            map { it.mottaker.type }.shouldContainExactly(
                MottakerType.VIRKSOMHET.beskrivelse,
                MottakerType.ANNEN_ORGANISASJON.beskrivelse
            )
            first().brevTyper
                .shouldHaveSize(1)
                .single().type
                .shouldBe(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
            elementAt(1).brevTyper
                .shouldHaveSize(1)
                .single().type
                .shouldBe(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
        }
    }

    @Test
    fun byggBrevmalDtoListe_behandlingHarIngenFlyt_returnererIkkeArbeidsgiverArbeidsgiversFullmektig() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling(Behandlingstyper.HENVENDELSE)
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling(Behandlingstyper.HENVENDELSE)
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(4)
            .map { it.mottaker.type }
            .shouldNotContain(MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.beskrivelse)
            .shouldContainExactly(
                listOf(
                    MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse,
                    MottakerType.UTENLANDSK_TRYGDEMYNDIGHET.beskrivelse,
                    MottakerType.ANNEN_ORGANISASJON.beskrivelse,
                    MottakerType.NORSK_MYNDIGHET.beskrivelse
                )
            )
    }

    companion object {

        var mottakereUtenArbeidsgiver = listOf(
            MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse,
            MottakerType.UTENLANDSK_TRYGDEMYNDIGHET.beskrivelse,
            MottakerType.ANNEN_ORGANISASJON.beskrivelse,
            MottakerType.NORSK_MYNDIGHET.beskrivelse
        )

        var mottakereAlle = listOf(
            MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse,
            MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.beskrivelse,
            MottakerType.UTENLANDSK_TRYGDEMYNDIGHET.beskrivelse,
            MottakerType.ANNEN_ORGANISASJON.beskrivelse,
            MottakerType.NORSK_MYNDIGHET.beskrivelse
        )

        @JvmStatic
        fun byggBrevmalDtoListe_behandlingsTemaIkkeStøttet_returnererIkkeArbeidsgiverArbeidsgiversFullmektigParametere() = listOf(
            Arguments.of(Behandlingstema.UTSENDT_ARBEIDSTAKER, mottakereAlle),
            Arguments.of(Behandlingstema.UTSENDT_SELVSTENDIG, mottakereUtenArbeidsgiver),
            Arguments.of(Behandlingstema.ARBEID_FLERE_LAND, mottakereAlle),
            Arguments.of(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY, mottakereAlle),
            Arguments.of(Behandlingstema.ARBEID_KUN_NORGE, mottakereAlle),
            Arguments.of(Behandlingstema.IKKE_YRKESAKTIV, mottakereUtenArbeidsgiver),
            Arguments.of(Behandlingstema.PENSJONIST, mottakereUtenArbeidsgiver),
            Arguments.of(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, mottakereAlle),
            Arguments.of(Behandlingstema.TRYGDETID, mottakereAlle),
        )
    }

    @ParameterizedTest
    @MethodSource("byggBrevmalDtoListe_behandlingsTemaIkkeStøttet_returnererIkkeArbeidsgiverArbeidsgiversFullmektigParametere")
    fun byggBrevmalDtoListe_behandlingsTemaIkkeStøttet_returnererIkkeArbeidsgiverArbeidsgiversFullmektig(
        behandlingstema: Behandlingstema,
        list: List<String>
    ) {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val behandling = lagBehandling(
            Behandlingstyper.FØRSTEGANG,
            Sakstyper.EU_EOS,
            Aktoer().apply { rolle = Aktoersroller.BRUKER },
            behandlingstema
        )

        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        withClue("Behandlingstema $behandlingstema") {
            tilgjengeligeMaler.map { it.mottaker.type }.shouldContainExactly(list)
        }

    }

    @Test
    fun byggBrevmalDtoListe_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererSoeknadMal() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(5)
            .first().brevTyper.run {
                this.shouldHaveSize(3)
                    .map(BrevmalTypeDto::getType)
                    .shouldContainExactly(
                        listOf(
                            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                            Produserbaredokumenter.MANGELBREV_BRUKER,
                            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
                        )
                    )
                this.first().apply {
                    type.shouldBe(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
                    felter.shouldBeNull()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_brukerAdresseNull_returnererMalMedFeilmelding() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(5)
            .first().mottaker.run {
                type.shouldBe(MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse)
                feilmelding.tittel.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.beskrivelse)
            }
    }

    @Test
    fun byggBrevmalDtoListe_registerOpplysningerIkkeHentet_returnererMalMedFeilmelding() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        every {
            hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(
                any<Long>(),
                any()
            )
        } throws TekniskException("Finner ikke arbeidsforholddokument")
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(5)
            .elementAt(1).mottaker.run {
                type.shouldBe(MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.beskrivelse)
                feilmelding.tittel.shouldBe(Kontroll_begrunnelser.INGEN_ARBEIDSGIVERE.beskrivelse)
            }
    }

    @Test
    fun byggBrevmalDtoListe_brevAdresseLagingKasterFeil_returnererMalMedFeilmeldingMenArbeidsgiverHarFastFeilmelding() {
        val FEILMELDING = "En annen feil"
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        every {
            hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(
                any<Long>(),
                any()
            )
        } throws TekniskException(FEILMELDING)
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(5).run {
            first().mottaker.run {
                type.shouldBe(MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse)
                feilmelding.tittel.shouldBe(FEILMELDING)
            }
            elementAt(1).mottaker.run {
                type.shouldBe(MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG.beskrivelse)
                feilmelding.tittel.shouldBe(
                    "Finner ikke gyldig adresse til arbeidsgiver(e). Kontroller at arbeidsgiver(e) er lagt inn korrekt i sidemenyen"
                )
            }
        }
    }

    @Test
    fun byggBrevmalDtoListe_mangelbrev_lagerRiktigeValg() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.first().brevTyper.elementAt(1).run {
            type.shouldBe(Produserbaredokumenter.MANGELBREV_BRUKER)
            felter.shouldHaveSize(2).run {
                first().run {
                    kode.shouldBe(BrevmalFeltKode.INNLEDNING_FRITEKST.kode)
                    feltType.shouldBe(FeltType.FRITEKST)
                    isPaakrevd.shouldBeTrue()
                    hjelpetekst.shouldBeNull()
                    tegnBegrensning.shouldBeNull()
                    valg.run {
                        valgType.shouldBe(FeltValgType.RADIO)
                        valgAlternativer.shouldHaveSize(2).run {
                            first().run {
                                kode.shouldBe(FeltvalgAlternativKode.STANDARD.kode)
                                isVisFelt.shouldBeFalse()
                            }
                            elementAt(1).run {
                                kode.shouldBe(FeltvalgAlternativKode.FRITEKST.kode)
                                isVisFelt.shouldBeTrue()
                            }
                        }
                    }
                }
                elementAt(1).run {
                    kode.shouldBe(BrevmalFeltKode.MANGLER_FRITEKST.kode)
                    feltType.shouldBe(FeltType.FRITEKST)
                    isPaakrevd.shouldBeTrue()
                    valg.shouldBeNull()
                    hjelpetekst.shouldBeNull()
                    tegnBegrensning.shouldBeNull()
                }
            }
        }
    }

    @Test
    fun byggBrevmalDtoListe_mangelbrevKlage_lagerRiktigeValg() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling(Behandlingstyper.KLAGE)
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling(Behandlingstyper.KLAGE)
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.first().brevTyper.first().run {
            type.shouldBe(Produserbaredokumenter.MANGELBREV_BRUKER)
            felter.shouldHaveSize(2).run {
                first().run {
                    kode.shouldBe(BrevmalFeltKode.INNLEDNING_FRITEKST.kode)
                    feltType.shouldBe(FeltType.FRITEKST)
                    isPaakrevd.shouldBeTrue()
                    hjelpetekst.shouldBeNull()
                    tegnBegrensning.shouldBeNull()
                    valg.shouldBeNull()
                }
                elementAt(1).run {
                    kode.shouldBe(BrevmalFeltKode.MANGLER_FRITEKST.kode)
                    feltType.shouldBe(FeltType.FRITEKST)
                    isPaakrevd.shouldBeTrue()
                    valg.shouldBeNull()
                    hjelpetekst.shouldBeNull()
                    tegnBegrensning.shouldBeNull()
                }
            }
        }
    }

    @Test
    fun byggBrevmalDtoListe_EUEØS_lagerRiktigeTittelValgForFritekstbrev() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(5)
            .first().brevTyper[2].felter[0].valg.valgAlternativer
            .shouldHaveSize(2).run {
                first().run {
                    kode.shouldBe(FeltvalgAlternativKode.HENVENDELSE_OM_TRYGDETILHØRLIGHET.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(1).run {
                    kode.shouldBe(FeltvalgAlternativKode.FRITEKST.kode)
                    isVisFelt.shouldBeTrue()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_EUEØS_lagerRiktigeDistribusjonstyperForFritekstbrev() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(5)
            .first().brevTyper[2].felter[3].valg.valgAlternativer
            .shouldHaveSize(3).run {
                first().run {
                    kode.shouldBe(Distribusjonstype.VEDTAK.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(1).run {
                    kode.shouldBe(Distribusjonstype.VIKTIG.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(2).run {
                    kode.shouldBe(Distribusjonstype.ANNET.kode)
                    isVisFelt.shouldBeFalse()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_FTRL_lagerRiktigeTittelValgForFritekstbrev() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling(sakstype = Sakstyper.FTRL)
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling(sakstype = Sakstyper.FTRL)

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(4)
            .first().brevTyper[2].felter.first().valg.valgAlternativer
            .shouldHaveSize(4).run {
                first().run {
                    kode.shouldBe(FeltvalgAlternativKode.CONFIRMATION_OF_MEMBERSHIP.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(1).run {
                    kode.shouldBe(FeltvalgAlternativKode.BEKREFTELSE_PÅ_MEDLEMSKAP.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(2).run {
                    kode.shouldBe(FeltvalgAlternativKode.HENVENDELSE_OM_MEDLEMSKAP.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(3).run {
                    kode.shouldBe(FeltvalgAlternativKode.FRITEKST.kode)
                    isVisFelt.shouldBeTrue()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_FTRL_lagerRiktigeDistribusjonstyperForFritekstbrev() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling(sakstype = Sakstyper.FTRL)
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling(sakstype = Sakstyper.FTRL)

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(4)
            .first().brevTyper[2].felter[3].valg.valgAlternativer
            .shouldHaveSize(3).run {
                first().run {
                    kode.shouldBe(Distribusjonstype.VEDTAK.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(1).run {
                    kode.shouldBe(Distribusjonstype.VIKTIG.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(2).run {
                    kode.shouldBe(Distribusjonstype.ANNET.kode)
                    isVisFelt.shouldBeFalse()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_trygdeavtale_lagerRiktigeTittelValgForFritekstbrev() {
        val behandlingTrygdeavtale = lagBehandling(sakstype = Sakstyper.TRYGDEAVTALE).apply {
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = MottatteOpplysningerData().apply {
                    soeknadsland = Soeknadsland(listOf(Land_iso2.GB.kode), false)
                }
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandlingTrygdeavtale
        every { behandlingService.hentBehandling(any<Long>()) } returns behandlingTrygdeavtale

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.shouldHaveSize(5)
            .first().brevTyper[2].felter[0].valg.valgAlternativer
            .shouldHaveSize(2).run {
                first().run {
                    kode.shouldBe(FeltvalgAlternativKode.ENGELSK_FRITEKSTBREV.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(1).run {
                    kode.shouldBe(FeltvalgAlternativKode.FRITEKST.kode)
                    isVisFelt.shouldBeTrue()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_trygdeavtale_lagerRiktigeDistribusjonstyperForFritekstbrev() {
        val behandlingTrygdeavtale = lagBehandling(sakstype = Sakstyper.TRYGDEAVTALE).apply {
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = AnmodningEllerAttest().apply {
                    lovvalgsland = Land_iso2.GB
                }
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandlingTrygdeavtale
        every { behandlingService.hentBehandling(any<Long>()) } returns behandlingTrygdeavtale

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(5)
            .first().brevTyper[2].felter[3].valg.valgAlternativer
            .shouldHaveSize(3).run {
                first().run {
                    kode.shouldBe(Distribusjonstype.VEDTAK.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(1).run {
                    kode.shouldBe(Distribusjonstype.VIKTIG.kode)
                    isVisFelt.shouldBeFalse()
                }
                elementAt(2).run {
                    kode.shouldBe(Distribusjonstype.ANNET.kode)
                    isVisFelt.shouldBeFalse()
                }
            }
    }

    @Test
    fun byggBrevmalDtoListe_trygdeavtale_behandlingUtenFlyt_lagerRiktigeFeltForUtenlandskTrygdeMyndighetFritekstbrev() {
        val behandlingTrygdeavtale = lagBehandling(Behandlingstyper.HENVENDELSE, Sakstyper.TRYGDEAVTALE).apply {
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = AnmodningEllerAttest()
            }
        }
        val utenlandskMyndighet = UtenlandskMyndighet().apply { landkode = Land_iso2.AU }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandlingTrygdeavtale
        every { behandlingService.hentBehandling(any<Long>()) } returns behandlingTrygdeavtale
        every { saksbehandlingRegler.harIngenFlyt(behandlingTrygdeavtale) } returns true
        every { utenlandskMyndighetService.hentAlleUtenlandskeMyndigheter() } returns listOf(utenlandskMyndighet)

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.filter { it.mottaker.rolle == Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET }
            .shouldHaveSize(1)
            .single().run {
                brevTyper
                    .shouldHaveSize(1)
                    .single().felter.first().run {
                        kode.shouldBe(BrevmalFeltKode.UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER.kode)
                        valg.valgAlternativer.shouldHaveSize(1)
                            .single().run {
                                kode.shouldBe(Land_iso2.AU.kode)
                                beskrivelse.shouldBe("Trygdemyndighetene i ${Land_iso2.AU.beskrivelse}")
                            }
                    }
            }
    }

    @Test
    fun byggBrevmalDtoListe_eu_eos_lagerRiktigeFeltOgValgForUtenlandskTrygdeMyndighetFritekstbrev() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.filter { it.mottaker.rolle == Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET }
            .shouldHaveSize(1)
            .single().run {
                brevTyper
                    .shouldHaveSize(1)
                    .single().felter.first().run {
                        kode.shouldBe(BrevmalFeltKode.UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER.kode)
                        valg.valgAlternativer.shouldHaveSize(2).run {
                            first().run {
                                kode.shouldBe(Land_iso2.FO.kode)
                                beskrivelse.shouldBe("Trygdemyndighetene i ${Land_iso2.FO.beskrivelse}")
                            }
                            elementAt(1).run {
                                kode.shouldBe(Land_iso2.GL.kode)
                                beskrivelse.shouldBe("Trygdemyndighetene i ${Land_iso2.GL.beskrivelse}")
                            }
                        }
                    }
            }
        verify(exactly = 0) { utenlandskMyndighetService.hentAlleUtenlandskeMyndigheter() }
    }

    @Test
    fun byggBrevmalDtoListe_trygdeavtale_behandlingMedFlyt_lagerRiktigeFeltForUtenlandskTrygdeMyndighetFritekstbrev() {
        val behandlingTrygdeavtale = lagBehandling(sakstype = Sakstyper.TRYGDEAVTALE).apply {
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = AnmodningEllerAttest().apply {
                    lovvalgsland = Land_iso2.AU
                }
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandlingTrygdeavtale
        every { behandlingService.hentBehandling(any<Long>()) } returns behandlingTrygdeavtale
        every { saksbehandlingRegler.harIngenFlyt(behandlingTrygdeavtale) } returns false

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.filter { it.mottaker.rolle == Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET }
            .shouldHaveSize(1)
            .single().run {
                brevTyper
                    .shouldHaveSize(1)
                    .single().felter
                    .filter { it.kode == BrevmalFeltKode.UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER.kode }
                    .shouldBeEmpty()
            }
    }

    @Test
    fun byggBrevmalDtoListe_trygdeavtale_behandlingUtenLand_lagerFeilmeldingForUtenlandskTrygdeMyndighetMottaker() {
        val behandlingTrygdeavtale = lagBehandling(sakstype = Sakstyper.TRYGDEAVTALE).apply {
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = AnmodningEllerAttest()
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandlingTrygdeavtale
        every { behandlingService.hentBehandling(any<Long>()) } returns behandlingTrygdeavtale

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler.filter { it.mottaker.rolle == Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET }
            .shouldHaveSize(1)
            .single().mottaker.feilmelding.tittel
            .shouldBe("Du må velge land på inngangssteget for å kunne sende brev til utenlandsk trygdemyndighet.")
    }

    @Test
    fun byggBrevmalDtoListe_registeOpplysningerNorskAdresseUtenAdresselinjer_returnererMalOK() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()
        every {
            hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(
                any<Long>(),
                any()
            )
        } returns lagBrevAdresse(Land_iso2.NO)

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(5)
            .first().mottaker.feilmelding
            .shouldBeNull()
    }

    @Test
    fun byggBrevmalDtoListe_registeOpplysningerUtenlandskadresseAdresseUtenAdresselinjer_returnererFeilkode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        mockUtenlandskTrygdemyndighetServiceMottakerValgKall()
        every {
            hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(
                any<Long>(),
                any()
            )
        } returns lagBrevAdresse(Land_iso2.SE)

        val tilgjengeligeMaler = brevmalListeBygger.byggBrevmalDtoListe(123L)

        tilgjengeligeMaler
            .shouldHaveSize(5)
            .first().mottaker.run {
                type.shouldBe(MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG.beskrivelse)
                feilmelding.tittel.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.beskrivelse)
            }
    }

    fun lagBehandling(
        behandlingstype: Behandlingstyper = Behandlingstyper.FØRSTEGANG,
        sakstype: Sakstyper = Sakstyper.EU_EOS,
        aktoer: Aktoer = Aktoer().apply { rolle = Aktoersroller.BRUKER },
        behandlingstema: Behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
    ): Behandling = Behandling.buildWithDefaults {
        id = 1L
        fagsak = FagsakTestFactory.builder().apply {
            type = sakstype
            leggTilAktør(aktoer)
        }.build()
        tema = behandlingstema
        type = behandlingstype

    }

    fun lagBrevAdresse(land: Land_iso2) = listOf(
        BrevAdresse("Mottaker", null, null, "0010", null, null, land.name)
    )

    fun mockUtenlandskTrygdemyndighetServiceMottakerValgKall() {
        val utenlandskMyndighetGrønland = UtenlandskMyndighet()
        utenlandskMyndighetGrønland.landkode = Land_iso2.GL
        val utenlandskMyndighetFærøyene = UtenlandskMyndighet()
        utenlandskMyndighetFærøyene.landkode = Land_iso2.FO
        every { utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.GL) } returns utenlandskMyndighetGrønland
        every { utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.FO) } returns utenlandskMyndighetFærøyene
        every { utenlandskMyndighetService.hentAlleUtenlandskeMyndigheter() } returns emptyList()
    }
}
