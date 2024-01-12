package no.nav.melosys.service.kontroll.unntak

import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kontroll.feature.unntak.AnmodningUnntakKontrollService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.validering.Kontrollfeil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate


@ExtendWith(MockitoExtension::class)
internal class AnmodningUnntakKontrollTest {
    @Mock
    private val anmodningsperiodeService: AnmodningsperiodeService? = null

    @Mock
    private val avklarteVirksomheterService: AvklarteVirksomheterService? = null

    @Mock
    private val behandlingService: BehandlingService? = null

    @Mock
    private val persondataFasade: PersondataFasade? = null

    private val behandlingID = 33L
    private val anmodningsperiode = Anmodningsperiode()

    private var anmodningUnntakKontrollService: AnmodningUnntakKontrollService? = null

    @BeforeEach
    fun setup() {
        anmodningsperiode.fom = LocalDate.now()
        anmodningsperiode.tom = LocalDate.now().plusYears(2)
        Mockito.`when`(anmodningsperiodeService!!.hentFørsteAnmodningsperiode(behandlingID)).thenReturn(anmodningsperiode)

        Mockito.`when`(persondataFasade!!.hentPerson(ArgumentMatchers.anyString()))
            .thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger())
        Mockito.`when`(avklarteVirksomheterService!!.hentAntallAvklarteVirksomheter(ArgumentMatchers.any())).thenReturn(1)

        anmodningUnntakKontrollService = AnmodningUnntakKontrollService(
            anmodningsperiodeService, avklarteVirksomheterService, behandlingService, persondataFasade
        )
    }

    @Test
    fun utførKontroller_manglerAdresse_returnererKode() {
        Mockito.`when`(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(SaksbehandlingDataFactory.lagBehandling())
        Mockito.`when`(persondataFasade!!.hentPerson(ArgumentMatchers.anyString()))
            .thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser())

        val resultat = anmodningUnntakKontrollService!!.utførKontroller(behandlingID)
        Assertions.assertThat(resultat)
            .extracting<Kontroll_begrunnelser, RuntimeException> { obj: Kontrollfeil -> obj.kode }
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE)
    }

    @Test
    fun utførKontroller_anmodningsperiodeManglerSluttdato_returnererKode() {
        Mockito.`when`(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(SaksbehandlingDataFactory.lagBehandling())
        anmodningsperiode.tom = null

        val resultat = anmodningUnntakKontrollService!!.utførKontroller(behandlingID)
        Assertions.assertThat(resultat)
            .extracting<Kontroll_begrunnelser, RuntimeException> { obj: Kontrollfeil -> obj.kode }
            .containsExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO)
    }

    @Test
    fun utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder = listOf(FysiskArbeidssted())
        Mockito.`when`(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID))
            .thenReturn(SaksbehandlingDataFactory.lagBehandling(mottatteOpplysningerData))

        val resultat = anmodningUnntakKontrollService!!.utførKontroller(behandlingID)
        Assertions.assertThat(resultat)
            .extracting<Kontroll_begrunnelser, RuntimeException> { obj: Kontrollfeil -> obj.kode }
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND)
    }

    @Test
    fun utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.foretakUtland = listOf(ForetakUtland())
        Mockito.`when`(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID))
            .thenReturn(SaksbehandlingDataFactory.lagBehandling(mottatteOpplysningerData))

        val resultat = anmodningUnntakKontrollService!!.utførKontroller(behandlingID)
        Assertions.assertThat(resultat)
            .extracting<Kontroll_begrunnelser, RuntimeException> { obj: Kontrollfeil -> obj.kode }
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL)
    }

    @Test
    fun utførKontroller_flereArbeidsgivere_returnererKode() {
        Mockito.`when`(behandlingService!!.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(SaksbehandlingDataFactory.lagBehandling())
        Mockito.`when`(avklarteVirksomheterService!!.hentAntallAvklarteVirksomheter(ArgumentMatchers.any())).thenReturn(2)

        val resultat = anmodningUnntakKontrollService!!.utførKontroller(behandlingID)
        Assertions.assertThat(resultat).extracting<Kontroll_begrunnelser, RuntimeException> { obj: Kontrollfeil -> obj.kode }
            .contains(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET)
    }
}
