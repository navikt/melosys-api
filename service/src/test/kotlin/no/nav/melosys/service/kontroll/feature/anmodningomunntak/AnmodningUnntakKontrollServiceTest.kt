package no.nav.melosys.service.kontroll.feature.anmodningomunntak

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class AnmodningUnntakKontrollServiceTest {
    @MockK
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    private val behandlingID = 33L
    private val anmodningsperiode = Anmodningsperiode()

    private lateinit var anmodningUnntakKontrollService: AnmodningUnntakKontrollService

    @BeforeEach
    fun setup() {
        anmodningsperiode.fom = LocalDate.now()
        anmodningsperiode.tom = LocalDate.now().plusYears(2)
        every { anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID) } returns anmodningsperiode

        every { persondataFasade.hentPerson(any<String>()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(any()) } returns 1

        anmodningUnntakKontrollService = AnmodningUnntakKontrollService(
            anmodningsperiodeService, avklarteVirksomheterService, behandlingService, persondataFasade
        )
    }

    @Test
    fun utførKontroller_manglerAdresse_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns SaksbehandlingDataFactory.lagBehandling()
        every { persondataFasade.hentPerson(any<String>()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()

        val resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE)
    }

    @Test
    fun utførKontroller_anmodningsperiodeManglerSluttdato_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns SaksbehandlingDataFactory.lagBehandling()
        anmodningsperiode.tom = null

        val resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO)
    }

    @Test
    fun utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder = listOf(FysiskArbeidssted())
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns SaksbehandlingDataFactory.lagBehandling(
            mottatteOpplysningerData
        )

        val resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND)
    }

    @Test
    fun utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        val foretakUtland = ForetakUtland()
        foretakUtland.selvstendigNæringsvirksomhet = false
        mottatteOpplysningerData.foretakUtland = listOf(foretakUtland)
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns SaksbehandlingDataFactory.lagBehandling(
            mottatteOpplysningerData
        )

        val resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL)
    }

    @Test
    fun utførKontroller_flereArbeidsgivere_returnererKode() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns SaksbehandlingDataFactory.lagBehandling()
        every { avklarteVirksomheterService.hentAntallAvklarteVirksomheter(any()) } returns 2

        val resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID)

        resultat.map { it.kode }
            .shouldContainExactly(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET)
    }
}
