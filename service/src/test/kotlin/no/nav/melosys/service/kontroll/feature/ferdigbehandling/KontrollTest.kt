package no.nav.melosys.service.kontroll.feature.ferdigbehandling

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.exception.KontrolldataFeilType
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl
import no.nav.melosys.service.LovvalgsperiodeService
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
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class KontrollTest {
    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @RelaxedMockK
    lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
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
    lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    @RelaxedMockK
    lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    private val behandlingID = 1L

    private lateinit var mockedKontroll: Kontroll
    private val unleash = FakeUnleash()

    private fun lagBehandling(
        init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
    ): Behandling = Behandling.forTest {
        id = behandlingID
        status = Behandlingsstatus.UNDER_BEHANDLING
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
            medGsakSaksnummer()
        }
        mottatteOpplysninger {
            mottatteOpplysningerData = MottatteOpplysningerData()
        }
        saksopplysning {
            type = SaksopplysningType.MEDL
            dokument = MedlemskapDokument()
        }
        init()
    }

    private fun lagLovvalgsperiode(
        init: LovvalgsperiodeTestFactory.Builder.() -> Unit = {}
    ): Lovvalgsperiode = lovvalgsperiodeForTest {
        tom = LocalDate.now()
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        init()
    }

    @BeforeEach
    fun setup() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()

        mockedKontroll = Kontroll(
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
            trygdeavgiftMottakerService,
            helseutgiftDekkesPeriodeService,
            unleash
        )
    }

    @Test
    fun kontroller_medOgUtenFeilSomSkalIgnoreres_returnererKorrekt() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()
        val feilSomSkalIgnoreres = Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER


        val kontrollFeilMedFeilSomSkalIgnoreres =
            mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, setOf(feilSomSkalIgnoreres))
        kontrollFeilMedFeilSomSkalIgnoreres.shouldBeEmpty()


        val kontrollFeilUtenFeilSomSkalIgnoreres =
            mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())
        kontrollFeilUtenFeilSomSkalIgnoreres
            .shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(feilSomSkalIgnoreres)
    }

    @Test
    fun kontroller_HenleggelsePersonMedRegistrertAdresse_returnererTomCollection() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.HENLEGGELSE, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_AvslagPersonMedRegistrertAdresse_returnererTomCollection() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_avslagIngenFlyt__returnererTomCollection() {
        val behandling = lagBehandling {
            type = Behandlingstyper.HENVENDELSE
            mottatteOpplysninger = null
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(1)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_FTRL_returnerer_adresse_mangler() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun kontroller_fullmektigFinnes_adresse_mangler() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.FTRL
                medFullmektig {
                    personIdent = "12345678910"
                    setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
                }
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { persondataFasade.hentPerson("12345678910") } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    @Test
    fun kontroller_AvslagPersonUtenRegistrertAdresse_returnererKode() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun kontroller_HenleggelsePersonUtenRegistrertAdresse_returnererKontrollfeil() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.HENLEGGELSE, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().run {
                kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
                type.shouldBe(KontrolldataFeilType.FEIL)
            }
    }

    @Test
    fun kontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomCollection() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() {
        val behandling = Behandling.forTest {
            id = behandlingID
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak {
                medBruker()
                medGsakSaksnummer()
            }
            mottatteOpplysninger {
                mottatteOpplysningerData = MottatteOpplysningerData()
            }
            saksopplysning {
                type = SaksopplysningType.MEDL
                // MedlemskapDokument er et dokument/DTO-objekt uten forTest DSL - bruker .apply
                dokument = MedlemskapDokument().apply {
                    medlemsperiode = listOf(
                        Medlemsperiode(
                            null, Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2)), null,
                            PeriodestatusMedl.GYLD.kode, null, null, null, null, null, null
                        )
                    )
                }
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(2)
            .run {
                first().kode.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
                last().kode.shouldBe(Kontroll_begrunnelser.PERIODEN_OVER_24_MD)
            }
    }

    @Test
    fun `tidligere trygdeavgiftsperioder som avsluttes dagen før en ny trygdeavgiftsperiode, skal gi kontrollfeil, dersom periode er i annen fagsak og har trygdeavgift`() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val behandlingsresultatFraAndreFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = "test-321"
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 1)
                    periodeTil = LocalDate.of(2012, 12, 20)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraAndreFagsak)

        val behandlingsresultatMedNyePerioder = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = behandling.fagsak.saksnummer
                    type = behandling.fagsak.type
                    tema = behandling.fagsak.tema
                    status = behandling.fagsak.status
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 21)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 21)
                    periodeTil = LocalDate.of(2012, 12, 24)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldNotBeEmpty()
            .single()
            .kode shouldBe Kontroll_begrunnelser.DIREKTE_FORUTGÅENDE_PERIODE
    }

    @Test
    fun `trygdeavgiftsperioder med overlappende periode, skal gi kontrollfeil, dersom periode er i annen fagsak og har trygdeavgift`() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val behandlingsresultatFraAndreFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = "test-321"
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 11)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 11)
                    periodeTil = LocalDate.of(2012, 12, 24)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraAndreFagsak)

        val behandlingsresultatMedNyePerioder = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = behandling.fagsak.saksnummer
                    type = behandling.fagsak.type
                    tema = behandling.fagsak.tema
                    status = behandling.fagsak.status
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 1)
                    periodeTil = LocalDate.of(2012, 12, 20)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldNotBeEmpty()
            .single()
            .kode shouldBe Kontroll_begrunnelser.OVERLAPPENDE_PERIODE_MED_FORSKUDDSVIS_FAKTURERUNG
    }

    @Test
    fun `tidligere trygdeavgiftsperioder som avsluttes dagen før en ny trygdeavgiftsperiode, skal ikke gi kontrollfeil, dersom periode er i samme fagsak`() {
        val sammeFagsakSaksnummer = "MEL-test"

        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                saksnummer = sammeFagsakSaksnummer
                status = Saksstatuser.OPPRETTET
                tema = Sakstemaer.TRYGDEAVGIFT
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val behandlingsresultatFraFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = sammeFagsakSaksnummer
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 1)
                    periodeTil = LocalDate.of(2012, 12, 20)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraFagsak)

        val behandlingsresultatMedNyePerioder = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = sammeFagsakSaksnummer
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 21)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 21)
                    periodeTil = LocalDate.of(2012, 12, 24)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun `trygdeavgiftsperioder med overlappende periode, skal ikke gi kontrollfeil, dersom periode er i samme fagsak og har trygdeavgift`() {
        val sammeFagsakSaksnummer = "MEL-test"

        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                saksnummer = sammeFagsakSaksnummer
                status = Saksstatuser.OPPRETTET
                tema = Sakstemaer.TRYGDEAVGIFT
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val behandlingsresultatFraFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = sammeFagsakSaksnummer
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 11)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 11)
                    periodeTil = LocalDate.of(2012, 12, 24)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraFagsak)

        val behandlingsresultatMedNyePerioder = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = sammeFagsakSaksnummer
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 1)
                    periodeTil = LocalDate.of(2012, 12, 20)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun `trygdeavgiftsperioder i bortfalt fagsak skal ikke gi overlappende periode kontrollfeil`() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val behandlingsresultatFraBortfaltFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = "test-bortfalt"
                    status = Saksstatuser.HENLAGT_BORTFALT
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 11)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 11)
                    periodeTil = LocalDate.of(2012, 12, 24)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraBortfaltFagsak)

        val behandlingsresultatMedNyePerioder = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = behandling.fagsak.saksnummer
                    type = behandling.fagsak.type
                    tema = behandling.fagsak.tema
                    status = behandling.fagsak.status
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 1)
                    periodeTil = LocalDate.of(2012, 12, 20)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun `trygdeavgiftsperioder i bortfalt fagsak skal ikke gi direkte forutgående periode kontrollfeil`() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.FTRL
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val behandlingsresultatFraBortfaltFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = "test-bortfalt"
                    status = Saksstatuser.HENLAGT_BORTFALT
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 1)
                    periodeTil = LocalDate.of(2012, 12, 20)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraBortfaltFagsak)

        val behandlingsresultatMedNyePerioder = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = behandling.fagsak.saksnummer
                    type = behandling.fagsak.type
                    tema = behandling.fagsak.tema
                    status = behandling.fagsak.status
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2012, 12, 21)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2012, 12, 21)
                    periodeTil = LocalDate.of(2012, 12, 24)
                    trygdeavgiftsbeløpMd = BigDecimal.TEN
                    trygdesats = BigDecimal.TEN
                }
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun `helseutgift dekkes-perioder i gyldig fagsak skal gi overlappende periode kontrollfeil`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.PENSJONIST
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling

        val nyHelseutgiftDekkesPeriode = HelseutgiftDekkesPeriode.forTest {
            fomDato = LocalDate.of(2012, 12, 1)
            tomDato = LocalDate.of(2012, 12, 20)
        }
        every { helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(any()) } returns nyHelseutgiftDekkesPeriode

        val behandlingsresultatFraGyldigFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = "test-gyldig"
                    status = Saksstatuser.OPPRETTET
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2012, 12, 10)
                tomDato = LocalDate.of(2012, 12, 25)
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraGyldigFagsak)

        val resultat = mockedKontroll.kontroller(behandlingID, null, emptySet())

        resultat.shouldNotBeEmpty()
            .single().kode shouldBe Kontroll_begrunnelser.OVERLAPPENDE_HELSEUTGIFT_DEKKES_PERIODE
    }

    @Test
    fun `helseutgift dekkes-perioder i bortfalt fagsak skal ikke gi overlappende periode kontrollfeil`() {
        val behandling = lagBehandling {
            tema = Behandlingstema.PENSJONIST
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.TRYGDEAVGIFT
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling

        val nyHelseutgiftDekkesPeriode = HelseutgiftDekkesPeriode.forTest {
            fomDato = LocalDate.of(2012, 12, 1)
            tomDato = LocalDate.of(2012, 12, 20)
        }
        every { helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(any()) } returns nyHelseutgiftDekkesPeriode

        val behandlingsresultatFraBortfaltFagsak = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    saksnummer = "test-bortfalt"
                    status = Saksstatuser.HENLAGT_BORTFALT
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.of(2012, 12, 10)
                tomDato = LocalDate.of(2012, 12, 25)
            }
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraBortfaltFagsak)

        val resultat = mockedKontroll.kontroller(behandlingID, null, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_periodeOver3År_returnererKode() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.TRYGDEAVTALE
            }
            mottatteOpplysninger {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3).plusDays(1)
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .first().kode.shouldBe(Kontroll_begrunnelser.MER_ENN_TRE_ÅR)
    }

    @Test
    fun kontroller_manglerAdresse_returnererKode() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode()
        mockLovvalgsperiodeService(lovvalgsperiode)
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.IKKE_FASTSATT, emptySet())


        resultat.shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun kontroller_periodeManglerSluttdato_returnererKode() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            tom = null
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().run {
                kode.shouldBe(Kontroll_begrunnelser.INGEN_SLUTTDATO)
                type.shouldBe(KontrolldataFeilType.FEIL)
            }
    }

    @Test
    fun kontroller_periodeManglerSluttdatoOgErUnntak_returnererKode() {
        val behandling = lagBehandling {
            tema = Behandlingstema.REGISTRERING_UNNTAK
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            tom = null
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND, emptySet())

        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().run {
                kode.shouldBe(Kontroll_begrunnelser.INGEN_SLUTTDATO)
                type.shouldBe(KontrolldataFeilType.FEIL)
            }
    }

    @Test
    fun kontroller_arbeidsstedManglerFelter_returnererKode() {
        // MottatteOpplysningerData er et Java DTO-objekt uten forTest DSL - bruker .apply
        val mottatteOpplysningerTestData = MottatteOpplysningerData().apply {
            arbeidPaaLand.fysiskeArbeidssteder = listOf(FysiskArbeidssted())
        }
        val behandling = lagBehandling {
            mottatteOpplysninger {
                mottatteOpplysningerData = mottatteOpplysningerTestData
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode()
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND)
    }

    @Test
    fun kontroller_foretakUtlandManglerFelter_returnererKode() {
        // MottatteOpplysningerData og ForetakUtland er Java DTO-objekter uten forTest DSL - bruker .apply
        val mottatteOpplysningerTestData = MottatteOpplysningerData().apply {
            foretakUtland = listOf(ForetakUtland().apply { selvstendigNæringsvirksomhet = false })
        }
        val behandling = lagBehandling {
            mottatteOpplysninger {
                mottatteOpplysningerData = mottatteOpplysningerTestData
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode()
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL)
    }

    @Test
    fun kontroller_avklartVirksomhetErOpphørt_returnererKode() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        every { avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling) } returns true

        val lovvalgsperiode = lagLovvalgsperiode()
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.OPPHØRT_ARBEIDSGIVER)
    }

    @Test
    fun kontroller_representantIUtlandetMangler_returnererKode() {
        val behandling = lagBehandling {
            fagsak {
                medBruker()
                medGsakSaksnummer()
                type = Sakstyper.TRYGDEAVTALE
            }
            mottatteOpplysninger {
                mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1

        val lovvalgsperiode = lagLovvalgsperiode {
            fom = LocalDate.now()
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        }
        mockLovvalgsperiodeService(lovvalgsperiode)


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED)
    }


    private fun mockLovvalgsperiodeService(lovvalgsperiode: Lovvalgsperiode) {
        every { lovvalgsperiodeService.hentLovvalgsperiode(behandlingID) } returns lovvalgsperiode
        every { lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandlingID) } returns null
    }
}
