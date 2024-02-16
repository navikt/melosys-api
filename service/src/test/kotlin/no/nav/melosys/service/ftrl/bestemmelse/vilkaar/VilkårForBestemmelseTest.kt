package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Ikkeyrkesaktivoppholdtype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VilkårForBestemmelseTest {
    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private lateinit var vilkårForBestemmelse: VilkårForBestemmelse

    @BeforeEach
    fun setUp() {
        vilkårForBestemmelse = VilkårForBestemmelse(mottatteOpplysningerService)
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1_FØRSTE_LEDD, ett eller flere land utenfor Norge`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(
                        Land_iso2.NO.toString(), "AB", "PR") } }
            }
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD to Ikkeyrkesaktivoppholdtype.MIDLERTIDIG_2_1_FJERDE_LEDD.name),
            1L
        )


        vilkår.shouldContainExactly(
            VilkårForBestemmelse.Vilkår(
                FTRL_2_1_BOSATT_NORGE_FORUT
            ),
            VilkårForBestemmelse.Vilkår(
                FTRL_2_1_OPPHOLD_UNDER_12MND
            ),
            VilkårForBestemmelse.Vilkår(
                FTRL_2_1_LOVLIG_OPPHOLD
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1_FØRSTE_LEDD, kun Norge`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(
                        Land_iso2.NO.toString()) } }
            }
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            VilkårForBestemmelse.Vilkår(
                FTRL_2_1_BOSATT_NORGE
            ),
            VilkårForBestemmelse.Vilkår(
                FTRL_2_1_LOVLIG_OPPHOLD
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_H`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            VilkårForBestemmelse.Vilkår(
                FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER
            ),
            VilkårForBestemmelse.Vilkår(
                FTRL_2_5_LÅN_STIPEND_LÅNEKASSEN
            ),
        )
    }
}
