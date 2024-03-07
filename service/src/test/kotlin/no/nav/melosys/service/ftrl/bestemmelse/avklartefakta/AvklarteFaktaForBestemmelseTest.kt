package no.nav.melosys.service.ftrl.bestemmelse.avklartefakta

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype.ARBIED_I_NORGE_2_2
import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype.ARBEID_PÅ_NORSK_SOKKEL_2_2
import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype.MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD
import no.nav.melosys.domain.kodeverk.Arbeidssituasjontype.VEKSELVIS_ARBEID_2_1_FJERDE_LEDD
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivoppholdtype.MIDLERTIDIG_2_1_FJERDE_LEDD
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivoppholdtype.VEKSELVIS_2_1_FJERDE_LEDD
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivrelasjontype.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarteFaktaForBestemmelseTest {
    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var avklarteFaktaForBestemmelse: AvklarteFaktaForBestemmelse

    @BeforeEach
    fun setUp() {
        avklarteFaktaForBestemmelse = AvklarteFaktaForBestemmelse(mottatteOpplysningerService, behandlingService)
    }


    @Test
    fun `avklarte fakta for FTRL_KAP2_2_1, ett eller flere land utenfor Norge, er ARBEIDSSITUASJON, yrkesaktiv`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(Land_iso2.NO.toString(), "AB", "PR") } }
            }
        val behandling = Behandling().apply { tema = Behandlingstema.YRKESAKTIV }
        every {behandlingService.hentBehandling(1L)} returns behandling
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(
                Avklartefaktatyper.ARBEIDSSITUASJON,
                listOf(MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD, VEKSELVIS_ARBEID_2_1_FJERDE_LEDD).map(Arbeidssituasjontype::name)
            )
        )
    }


    @Test
    fun `avklarte fakta for FTRL_KAP2_2_1, kun norge, ingen avklarte fakta yrkesaktiv`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(Land_iso2.NO.toString()) } }
            }
        val behandling = Behandling().apply { tema = Behandlingstema.YRKESAKTIV }
        every {behandlingService.hentBehandling(1L)} returns behandling
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger

        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1, 1L
        ).shouldBeEmpty()
    }


    @Test
    fun `avklarte fakta for FTRL_KAP2_2_2, er ARBEIDSSITUASJON, yrkesaktiv`() {
        val behandling = Behandling().apply { tema = Behandlingstema.YRKESAKTIV }
        every {behandlingService.hentBehandling(1L)} returns behandling

        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(
                Avklartefaktatyper.ARBEIDSSITUASJON,
                listOf(ARBIED_I_NORGE_2_2, ARBEID_PÅ_NORSK_SOKKEL_2_2).map(Arbeidssituasjontype::name)
            )
        )
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_1_FØRSTE_LEDD, ett eller flere land utenfor Norge, er IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(Land_iso2.NO.toString(), "AB", "PR") } }
            }

        val behandling = Behandling().apply { tema = Behandlingstema.IKKE_YRKESAKTIV }
        every {behandlingService.hentBehandling(1L)} returns behandling
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(
                Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD,
                listOf(MIDLERTIDIG_2_1_FJERDE_LEDD, VEKSELVIS_2_1_FJERDE_LEDD).map(Ikkeyrkesaktivoppholdtype::name)
            )
        )
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_1_FØRSTE_LEDD, Norge, ingen avklarte fakta`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(Land_iso2.NO.toString()) } }
            }
        val behandling = Behandling().apply { tema = Behandlingstema.YRKESAKTIV }

        every {behandlingService.hentBehandling(1L)} returns behandling
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger

        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD, 1L
        ).shouldBeEmpty()
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_5_ANDRE_LEDD er IKKE_YRKESAKTIV_RELASJON`() {
        val behandling = Behandling().apply { tema = Behandlingstema.IKKE_YRKESAKTIV }
        every {behandlingService.hentBehandling(1L)} returns behandling

        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(
                Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, listOf(
                    BARN_2_5_ANDRE_LEDD, EKTEFELLE_2_5_ANDRE_LEDD_A_TIL_B, EKTEFELLE_2_5_ANDRE_LEDD_C_TIL_E
                ).map(Ikkeyrkesaktivrelasjontype::name)
            )
        )
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_8_FJERDE_LEDD er IKKE_YRKESAKTIV_RELASJON`() {
        val behandling = Behandling().apply { tema = Behandlingstema.IKKE_YRKESAKTIV }
        every {behandlingService.hentBehandling(1L)} returns behandling

        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(
                Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, listOf(
                    BARN_2_8_FJERDE_LEDD, EKTEFELLE_2_8_FJERDE_LEDD
                ).map(Ikkeyrkesaktivrelasjontype::name)
            )
        )
    }
}
