package no.nav.melosys.service.ftrl.bestemmelse.avklartefakta

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarteFaktaForBestemmelseTest {
    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private lateinit var avklarteFaktaForBestemmelse: AvklarteFaktaForBestemmelse

    @BeforeEach
    fun setUp() {
        avklarteFaktaForBestemmelse = AvklarteFaktaForBestemmelse(mottatteOpplysningerService)
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_1_FØRSTE_LEDD, ett eller flere land utenfor Norge, er IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD`() {
        var mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(Landkoder.NO.toString(), "AB", "PR") } }
            }
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD, emptyList())
        )
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_1_FØRSTE_LEDD, Norge, ingen avklarte fakta`() {
        var mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(Landkoder.NO.toString()) } }
            }
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger

        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD, 1L
        ).shouldBeEmpty()
    }

    @Test
    fun `avklarte fakta for FTRL_KAP2_2_5_ANDRE_LEDD er IKKE_YRKESAKTIV_RELASJON`() {
        avklarteFaktaForBestemmelse.hentAvklarteFakta(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD, 1L
        ).shouldContainExactly(
            AvklarteFaktaForBestemmelse.AvklarteFaktaType(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON, emptyList())
        )
    }
}
