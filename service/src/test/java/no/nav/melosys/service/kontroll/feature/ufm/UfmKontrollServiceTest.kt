package no.nav.melosys.service.kontroll.feature.ufm

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
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

    private val behandling: Behandling = SaksbehandlingDataFactory.lagBehandling()

    @BeforeEach
    fun setup() {
        ufmKontrollService = UfmKontrollService(
            kontrollresultatRepository,
            behandlingsresultatService,
            behandlingService,
            persondataFasade
        )
    }

    @Test
    fun utførKontrollerOgRegistrerFeil_A009_forventKontroll_lovvalgslandNorge() {
        val sedDokument = SedDokument()
        sedDokument.sedType = SedType.A009
        sedDokument.lovvalgslandKode = Landkoder.NO
        sedDokument.lovvalgsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        behandling.saksopplysninger.add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL))
        behandling.saksopplysninger.add(lagSaksopplysning(MedlemskapDokument(), SaksopplysningType.MEDL))
        behandling.saksopplysninger.add(lagSaksopplysning(InntektDokument(), SaksopplysningType.INNTK))
        behandling.saksopplysninger.add(lagSaksopplysning(UtbetalingDokument(), SaksopplysningType.UTBETAL))

        val behandlingId = 1L
        every { persondataFasade.hentPerson(any()) } returns lagPersonopplysninger()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns lagBehandlingsresultat()

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


        ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingId)


        verify { behandlingService.hentBehandlingMedSaksopplysninger(any()) }
        verify { kontrollresultatRepository.deleteByBehandlingsresultat(ofType(Behandlingsresultat::class)) }
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
