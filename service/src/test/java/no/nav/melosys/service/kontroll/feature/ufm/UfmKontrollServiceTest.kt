package no.nav.melosys.service.kontroll.feature.ufm

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.repository.KontrollresultatRepository
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
class UfmKontrollServiceTest {
    @RelaxedMockK
    lateinit var kontrollresultatRepository: KontrollresultatRepository

    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    lateinit var behandlingsgrunnlagService: BehandlingsgrunnlagService

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    lateinit var ufmKontrollService: UfmKontrollService

    private val unleash: FakeUnleash = FakeUnleash()

    private val BEHANDLING_ID = 1L
    private val BEHANDLINGSRESULTAT_ID = 2L
    private val behandling: Behandling = SaksbehandlingDataFactory.lagBehandling()
    private val kontrollresultatSlot = slot<List<Kontrollresultat>>()
    private var sedDokument: SedDokument = SedDokument()
    private var medlemskapDokument = MedlemskapDokument()
    private var personopplysninger: Personopplysninger = lagPersonopplysninger()
    private var behandlingsgrunnlagData: BehandlingsgrunnlagData = BehandlingsgrunnlagData()

    @BeforeEach
    fun setup() {
        unleash.enableAll()
        ufmKontrollService = UfmKontrollService(
            kontrollresultatRepository,
            behandlingsresultatService,
            behandlingsgrunnlagService,
            behandlingService,
            persondataFasade,
            unleash
        )
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_forventKontroll_ingenFeil() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        }
        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldBeEmpty().toList()
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test // Ta vekk med a003-inn toggle
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_forventKontroll_overlappendePerioder_gammel() {
        unleash.disableAll();
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
            setErEndring(false);
        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                }
            )
        }

        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(1)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                    }
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_erIkkeEndring_ingenFeil() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
            setErEndring(false);
        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                }
            )
        }

        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(0).toList()
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_erEndring_forventKontroll() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
            setErEndring(true)
        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                }
            )
        }

        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(1)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                    }
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_harOpplysning_forventKontroll() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))

        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                }
            )
            personopplysninger
        }
        behandlingsgrunnlagData.apply {
            ytterligereInformasjon = "Vi har ekstra informasjon som en saksbehandler må manuelt behandle!"
        }
        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(1)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                    }
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_harIkkeOpplysning_forventIkkeKontroll() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))

        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                }
            )
            personopplysninger
        }
        behandlingsgrunnlagData.apply {
            ytterligereInformasjon = null
        }
        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(0).toList()
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_harForskjelligMedlLovvalgsland_forventKontroll() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(2))

        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                },
            )
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2))
                    land = "DK"
                },
            )
            personopplysninger
        }
        behandlingsgrunnlagData.apply {
            ytterligereInformasjon = "Vi har ekstra informasjon som en saksbehandler må manuelt behandle!"
        }
        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(1)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                    }
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_harLikMedlLovvalgsland_forventIkkeKontroll() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))

        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                },
            )
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2))
                    land = "SE"
                },
            )
            personopplysninger
        }
        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(0).toList()
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_harLikMedlLovvalgslandIPeriodeUtenomSedEndring_forventIkkeKontroll() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))

        }
        medlemskapDokument.apply {
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
                    land = "SE"
                },
            )
            medlemsperiode.add(
                Medlemsperiode().apply {
                    periode = Periode(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2))
                    land = "DK"
                },
            )
            personopplysninger
        }
        personopplysninger.apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(0).toList()
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }


    @Test
    fun utførKontrollerOgRegistrerFeil_A003_forventKontroll_bosattINorge() {
        sedDokument.apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.NO
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(2)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                        last().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.BOSATT_I_NORGE)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                    }
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)


        verify { behandlingService.hentBehandlingMedSaksopplysninger(any()) }
        verify { kontrollresultatRepository.deleteByBehandlingsresultat(ofType(Behandlingsresultat::class)) }
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A009_forventKontroll_lovvalgslandNorge() {
        sedDokument.apply {
            sedType = SedType.A009
            lovvalgslandKode = Landkoder.NO
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        }
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldHaveSize(2)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                        last().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.LOVVALGSLAND_NORGE)
                            behandlingsresultat.id.shouldBe(BEHANDLINGSRESULTAT_ID)
                        }
                    }
            }
        setupMockedTestData()


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontroller_periodeIkkeGyldig_forventFeil_feilIPerioden() {
        sedDokument.apply {
            lovvalgsperiode = Periode(
                LocalDate.now(),
                LocalDate.now().minusYears(1)
            )
        }
        behandling.saksopplysninger.add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL))


        val kontrollBegrunnelser = ufmKontrollService.utførKontroller(behandling)


        kontrollBegrunnelser
            .shouldHaveSize(1)
            .first().shouldBeEqualToComparingFields(Kontroll_begrunnelser.FEIL_I_PERIODEN)
    }

    private fun setupMockedTestData() {
        behandling.saksopplysninger.add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL))
        behandling.saksopplysninger.add(lagSaksopplysning(medlemskapDokument, SaksopplysningType.MEDL))
        behandling.saksopplysninger.add(lagSaksopplysning(InntektDokument(), SaksopplysningType.INNTK))
        behandling.saksopplysninger.add(lagSaksopplysning(UtbetalingDokument(), SaksopplysningType.UTBETAL))

        every { persondataFasade.hentPerson(any()) } returns personopplysninger
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns Behandlingsresultat()
            .apply {
                id = BEHANDLINGSRESULTAT_ID
            }
        every { behandlingsgrunnlagService.hentBehandlingsgrunnlagdata(BEHANDLING_ID) } returns behandlingsgrunnlagData
    }

    private fun lagSaksopplysning(
        saksopplysningDokument: SaksopplysningDokument,
        type: SaksopplysningType
    ): Saksopplysning {
        val saksopplysning = Saksopplysning()
        saksopplysning.dokument = saksopplysningDokument
        saksopplysning.type = type
        return saksopplysning
    }
}
