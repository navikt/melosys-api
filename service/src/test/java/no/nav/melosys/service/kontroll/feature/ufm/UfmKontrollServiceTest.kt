package no.nav.melosys.service.kontroll.feature.ufm

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Kontrollresultat
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
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
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class UfmKontrollServiceTest {
    @Mock
    private val kontrollresultatRepository: KontrollresultatRepository? = null

    @Mock
    private val behandlingsresultatService: BehandlingsresultatService? = null

    @Mock
    private val behandlingService: BehandlingService? = null

    @Mock
    private val persondataFasade: PersondataFasade? = null

    @Captor
    private val kontrollresultaterCaptor: ArgumentCaptor<List<Kontrollresultat>>? = null
    private var ufmKontrollService: UfmKontrollService? = null
    private val behandling = SaksbehandlingDataFactory.lagBehandling()
    @BeforeEach
    fun setup() {
        ufmKontrollService = UfmKontrollService(
            kontrollresultatRepository,
            behandlingsresultatService,
            behandlingService,
            persondataFasade
        )
        val sedDokument = SedDokument()
        sedDokument.sedType = SedType.A009
        sedDokument.lovvalgslandKode = Landkoder.NO
        sedDokument.lovvalgsperiode = Periode(
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        )
        behandling.saksopplysninger.add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL))
        behandling.saksopplysninger.add(lagSaksopplysning(MedlemskapDokument(), SaksopplysningType.MEDL))
        behandling.saksopplysninger.add(lagSaksopplysning(InntektDokument(), SaksopplysningType.INNTK))
        behandling.saksopplysninger.add(lagSaksopplysning(UtbetalingDokument(), SaksopplysningType.UTBETAL))
    }

    @Test
    fun utførKontrollerOgRegistrerFeil() {
        val BEHANDLING_ID = 1L
        Mockito.`when`(persondataFasade!!.hentPerson(ArgumentMatchers.any()))
            .thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger())
        Mockito.`when`(behandlingService!!.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling)
        Mockito.`when`(behandlingsresultatService!!.hentBehandlingsresultat(BEHANDLING_ID))
            .thenReturn(lagBehandlingsresultat())
        ufmKontrollService!!.utførKontrollerOgRegistrerFeil(BEHANDLING_ID)
        Mockito.verify(behandlingService).hentBehandlingMedSaksopplysninger(ArgumentMatchers.anyLong())
        Mockito.verify(kontrollresultatRepository).deleteByBehandlingsresultat(
            ArgumentMatchers.any(
                Behandlingsresultat::class.java
            )
        )
        Mockito.verify(kontrollresultatRepository).saveAll(
            kontrollresultaterCaptor!!.capture()
        )
        val kontrollresultater = kontrollresultaterCaptor.value
        Assertions.assertThat(kontrollresultater).hasSize(2)
        Assertions.assertThat(kontrollresultater)
            .extracting<Kontroll_begrunnelser, RuntimeException> { obj: Kontrollresultat -> obj.begrunnelse }
            .containsExactlyInAnyOrder(
                Kontroll_begrunnelser.LOVVALGSLAND_NORGE,
                Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND
            )
        Assertions.assertThat(kontrollresultater)
            .extracting<Behandlingsresultat, RuntimeException> { obj: Kontrollresultat -> obj.behandlingsresultat }
            .extracting<Long, RuntimeException> { obj: Behandlingsresultat -> obj.id }
            .containsExactlyInAnyOrder(2L, 2L)
    }

    @Test
    fun utførKontroller_periodeIkkeGyldig_forventEttTreff() {
        val sedDokument = behandling.hentSedDokument()
        sedDokument.lovvalgsperiode = Periode(
            LocalDate.now(),
            LocalDate.now().minusYears(1)
        )
        Assertions.assertThat(ufmKontrollService!!.utførKontroller(behandling))
            .containsExactly(Kontroll_begrunnelser.FEIL_I_PERIODEN)
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
