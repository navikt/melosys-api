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
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
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
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
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
    private val lovvalgsperiode = Lovvalgsperiode().apply {
        tom = LocalDate.now()
        bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
    }
    private val mottatteOpplysningerData = MottatteOpplysningerData()
    private val behandling = SaksbehandlingDataFactory.lagBehandling(mottatteOpplysningerData)

    lateinit var mockedKontroll: Kontroll
    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling) } returns 1
        behandling.saksopplysninger.add(Saksopplysning().apply {
            type = SaksopplysningType.MEDL
            dokument = MedlemskapDokument()
        })

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
        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.HENLEGGELSE, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_AvslagPersonMedRegistrertAdresse_returnererTomCollection() {
        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_avslagIngenFlyt__returnererTomCollection() {
        behandling.type = Behandlingstyper.HENVENDELSE
        behandling.mottatteOpplysninger = null
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() {
        mockLovvalgsperiodeService()
        lovvalgsperiode.apply {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(1)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        }


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_FTRL_returnerer_adresse_mangler() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()
        behandling.fagsak.type = Sakstyper.FTRL


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun kontroller_fullmektigFinnes_adresse_mangler() {
        val fullmektig = Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            personIdent = "12345678910"
            setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD)
        }
        behandling.fagsak.apply {
            aktører.add(fullmektig)
            type = Sakstyper.FTRL
        }
        every { persondataFasade.hentPerson(fullmektig.personIdent) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT)
    }

    @Test
    fun kontroller_AvslagPersonUtenRegistrertAdresse_returnererKode() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun kontroller_HenleggelsePersonUtenRegistrertAdresse_returnererKontrollfeil() {
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
        mockLovvalgsperiodeService()
        lovvalgsperiode.apply {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
        }


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK, emptySet())


        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() {
        mockLovvalgsperiodeService()
        lovvalgsperiode.apply {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        behandling.hentMedlemskapDokument().medlemsperiode = listOf(
            Medlemsperiode(
                null, Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2)), null,
                PeriodestatusMedl.GYLD.kode, null, null, null, null, null, null
            )
        )


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
        behandling.fagsak.type = Sakstyper.FTRL

        val behandlingFraAndreFagsak = Behandling.forTest {
            fagsak = Fagsak(saksnummer = "test-321", status = Saksstatuser.OPPRETTET, tema = Sakstemaer.TRYGDEAVGIFT, type = Sakstyper.FTRL)
        }
        val behandlingsresultatFraAndreFagsak = Behandlingsresultat().apply {
            behandling = behandlingFraAndreFagsak
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 1),
                        periodeTil = LocalDate.of(2012, 12, 20),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraAndreFagsak)

        val nyBehandling1 = Behandling.forTest {
            fagsak = behandling.fagsak
        }
        val behandlingsresultatMedNyePerioder = Behandlingsresultat().apply {
            behandling = nyBehandling1
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 21)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 21),
                        periodeTil = LocalDate.of(2012, 12, 24),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
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
        behandling.fagsak.type = Sakstyper.FTRL

        val behandlingFraAndreFagsak = Behandling.forTest {
            fagsak = Fagsak(saksnummer = "test-321", status = Saksstatuser.OPPRETTET, tema = Sakstemaer.TRYGDEAVGIFT, type = Sakstyper.FTRL)
        }
        val behandlingsresultatFraAndreFagsak = Behandlingsresultat().apply {
            behandling = behandlingFraAndreFagsak
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 11)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 11),
                        periodeTil = LocalDate.of(2012, 12, 24),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraAndreFagsak)

        val nyBehandling = Behandling.forTest {
            fagsak = behandling.fagsak
        }
        val behandlingsresultatMedNyePerioder = Behandlingsresultat().apply {
            behandling = nyBehandling
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 1),
                        periodeTil = LocalDate.of(2012, 12, 20),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
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
        behandling.fagsak.type = Sakstyper.FTRL

        val sammeFagsak = Fagsak(saksnummer = "MEL-test", status = Saksstatuser.OPPRETTET, tema = Sakstemaer.TRYGDEAVGIFT, type = Sakstyper.FTRL)

        val behandlingSammeFagsak1 = Behandling.forTest {
            fagsak = sammeFagsak
        }
        val behandlingsresultatFraFagsak = Behandlingsresultat().apply {
            behandling = behandlingSammeFagsak1
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 1),
                        periodeTil = LocalDate.of(2012, 12, 20),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraFagsak)

        val behandlingSammeFagsak2 = Behandling.forTest {
            fagsak = sammeFagsak
        }
        val behandlingsresultatMedNyePerioder = Behandlingsresultat().apply {
            behandling = behandlingSammeFagsak2
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 21)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 21),
                        periodeTil = LocalDate.of(2012, 12, 24),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun `trygdeavgiftsperioder med overlappende periode, skal ikke gi kontrollfeil, dersom periode er i samme fagsak og har trygdeavgift`() {
        behandling.fagsak.type = Sakstyper.FTRL

        val sammeFagsak = Fagsak(saksnummer = "MEL-test", status = Saksstatuser.OPPRETTET, tema = Sakstemaer.TRYGDEAVGIFT, type = Sakstyper.FTRL)

        val behandlingSammeFagsak1 = Behandling.forTest {
            fagsak = sammeFagsak
        }
        val behandlingsresultatFraFagsak = Behandlingsresultat().apply {
            behandling = behandlingSammeFagsak1
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 11)
                tom = LocalDate.of(2012, 12, 24)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 11),
                        periodeTil = LocalDate.of(2012, 12, 24),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
        }

        every {
            behandlingsresultatService.finnAlleBehandlingsresultatForAktør(any())
        } returns listOf(behandlingsresultatFraFagsak)

        val behandlingSammeFagsak2 = Behandling.forTest {
            fagsak = sammeFagsak
        }
        val behandlingsresultatMedNyePerioder = Behandlingsresultat().apply {
            behandling = behandlingSammeFagsak2
            val medlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2012, 12, 1)
                tom = LocalDate.of(2012, 12, 20)
                trygdeavgiftsperioder.add(
                    Trygdeavgiftsperiode(
                        periodeFra = LocalDate.of(2012, 12, 1),
                        periodeTil = LocalDate.of(2012, 12, 20),
                        trygdeavgiftsbeløpMd = Penger(BigDecimal.TEN, NOK.kode),
                        trygdesats = BigDecimal.TEN
                    )
                )
            }
            addMedlemskapsperiode(medlemskapsperiode)
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedNyePerioder

        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true

        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())

        resultat.shouldBeEmpty()
    }

    @Test
    fun kontroller_periodeOver3År_returnererKode() {
        mockLovvalgsperiodeService()
        lovvalgsperiode.apply {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3).plusDays(1)
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        }
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        behandling.fagsak.type = Sakstyper.TRYGDEAVTALE


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .first().kode.shouldBe(Kontroll_begrunnelser.MER_ENN_TRE_ÅR)
    }

    @Test
    fun kontroller_manglerAdresse_returnererKode() {
        mockLovvalgsperiodeService()
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.IKKE_FASTSATT, emptySet())


        resultat.shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER)
    }

    @Test
    fun kontroller_periodeManglerSluttdato_returnererKode() {
        mockLovvalgsperiodeService()
        lovvalgsperiode.tom = null


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
        mockLovvalgsperiodeService()
        lovvalgsperiode.tom = null
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling


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
        mockLovvalgsperiodeService()
        mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder = listOf(FysiskArbeidssted())


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND)
    }

    @Test
    fun kontroller_foretakUtlandManglerFelter_returnererKode() {
        mockLovvalgsperiodeService()
        mottatteOpplysningerData.foretakUtland = listOf(ForetakUtland().apply { selvstendigNæringsvirksomhet = false })


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL)
    }

    @Test
    fun kontroller_avklartVirksomhetErOpphørt_returnererKode() {
        mockLovvalgsperiodeService()
        every { avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling) } returns true


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.OPPHØRT_ARBEIDSGIVER)
    }

    @Test
    fun kontroller_representantIUtlandetMangler_returnererKode() {
        mockLovvalgsperiodeService()
        lovvalgsperiode.fom = LocalDate.now()
        lovvalgsperiode.bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS()
        behandling.fagsak.type = Sakstyper.TRYGDEAVTALE


        val resultat = mockedKontroll.kontroller(behandlingID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, emptySet())


        resultat.shouldNotBeEmpty()
            .shouldHaveSize(1)
            .single().kode.shouldBe(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED)
    }


    private fun mockLovvalgsperiodeService() {
        every { lovvalgsperiodeService.hentLovvalgsperiode(behandlingID) } returns lovvalgsperiode
        every { lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandlingID) } returns null
    }
}

