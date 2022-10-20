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
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
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
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    lateinit var ufmKontrollService: UfmKontrollService

    private val unleash: FakeUnleash = FakeUnleash()

    private val behandling: Behandling = SaksbehandlingDataFactory.lagBehandling()

    private val BEHANDLING_ID = 1L

    @BeforeEach
    fun setup() {
        unleash.enableAll()
        ufmKontrollService = UfmKontrollService(
            kontrollresultatRepository,
            behandlingsresultatService,
            behandlingService,
            persondataFasade,
            unleash
        )
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_forventKontroll_ingenFeil() {
        val sedDokument = SedDokument().apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        }

        val personopplysninger = lagPersonopplysninger().apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        mockTestData(sedDokument, personopplysninger)

        val kontrollresultatSlot = slot<List<Kontrollresultat>>()
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldBeEmpty().toList()
            }


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_medOverlappendePeriode_erOpprinnelig_ingenFeil() {
        val sedDokument = SedDokument().apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.SE
            avsenderLandkode = Landkoder.SE
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
            erEndring = true;
        }

        val personopplysninger = lagPersonopplysninger().apply {
            bostedsadresse = Bostedsadresse(
                StrukturertAdresse().apply { landkode = "SE" }, null, null, null, null,
                null,
                false
            );
        }
        mockTestData(sedDokument, personopplysninger)

        val kontrollresultatSlot = slot<List<Kontrollresultat>>()
        every { kontrollresultatRepository.saveAll(capture(kontrollresultatSlot)) }
            .answers {
                kontrollresultatSlot.captured.shouldBeEmpty().toList()
            }


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A003_forventKontroll_bosattINorge() {
        val sedDokument = SedDokument().apply {
            sedType = SedType.A003
            lovvalgslandKode = Landkoder.NO
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        }

        mockTestData(sedDokument, lagPersonopplysninger())

        val kontrollresultaterSlot = slot<List<Kontrollresultat>>()
        every { kontrollresultatRepository.saveAll(capture(kontrollresultaterSlot)) }
            .answers {
                kontrollresultaterSlot.captured.shouldHaveSize(2)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND)
                            behandlingsresultat.id.shouldBe(2L)
                        }
                        last().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.BOSATT_I_NORGE)
                            behandlingsresultat.id.shouldBe(2L)
                        }
                    }
            }


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)


        verify { behandlingService.hentBehandlingMedSaksopplysninger(any()) }
        verify { kontrollresultatRepository.deleteByBehandlingsresultat(ofType(Behandlingsresultat::class)) }
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A009_forventKontroll_lovvalgslandNorge() {
        val sedDokument = SedDokument().apply {
            sedType = SedType.A009
            lovvalgslandKode = Landkoder.NO
            lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        }

        mockTestData(sedDokument, lagPersonopplysninger())

        val kontrollresultaterSlot = slot<List<Kontrollresultat>>()
        every { kontrollresultatRepository.saveAll(capture(kontrollresultaterSlot)) }
            .answers {
                kontrollresultaterSlot.captured.shouldHaveSize(2)
                    .sortedBy { it.begrunnelse }
                    .apply {
                        first().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND)
                            behandlingsresultat.id.shouldBe(2L)
                        }
                        last().apply {
                            begrunnelse.shouldBe(Kontroll_begrunnelser.LOVVALGSLAND_NORGE)
                            behandlingsresultat.id.shouldBe(2L)
                        }
                    }
            }


        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
    }

    @Test
    fun utførKontroller_periodeIkkeGyldig_forventFeil_feilIPerioden() {
        val sedDokument = SedDokument().apply {
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

    private fun mockTestData(
        sedDokument: SedDokument,
        personopplysninger: Personopplysninger?
    ) {
        behandling.saksopplysninger.add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL))
        behandling.saksopplysninger.add(lagSaksopplysning(MedlemskapDokument(), SaksopplysningType.MEDL))
        behandling.saksopplysninger.add(lagSaksopplysning(InntektDokument(), SaksopplysningType.INNTK))
        behandling.saksopplysninger.add(lagSaksopplysning(UtbetalingDokument(), SaksopplysningType.UTBETAL))

        every { persondataFasade.hentPerson(any()) } returns personopplysninger
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat()
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

    private fun lagBehandlingsresultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.id = 2L
        return behandlingsresultat
    }
}
