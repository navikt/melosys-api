package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VilkårForBestemmelseIkkeYrkesaktivTest {
    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private lateinit var vilkårForBestemmelse: VilkårForBestemmelseIkkeYrkesaktiv

    @BeforeEach
    fun setUp() {
        vilkårForBestemmelse = VilkårForBestemmelseIkkeYrkesaktiv(mottatteOpplysningerService)
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1, ett eller flere land utenfor Norge`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(
                        Land_iso2.NO.toString(), "AB", "PR") } }
            }
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD to Ikkeyrkesaktivoppholdtype.MIDLERTIDIG_2_1_FJERDE_LEDD.name),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1_BOSATT_NORGE_FORUT
            ),
            Vilkår(
                FTRL_2_1_OPPHOLD_UNDER_12MND
            ),
            Vilkår(
                FTRL_2_1_LOVLIG_OPPHOLD
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1, kun Norge`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply { soeknadsland = Soeknadsland().apply { landkoder = listOf(
                        Land_iso2.NO.toString()) } }
            }
        every {mottatteOpplysningerService.hentMottatteOpplysninger(1L)} returns mottatteOpplysninger


        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1_BOSATT_NORGE
            ),
            Vilkår(
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
            Vilkår(
                FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER
            ),
            Vilkår(
                FTRL_2_5_LÅN_STIPEND_LÅNEKASSEN
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_ANDRE_LEDD, barn`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.BARN_2_5_ANDRE_LEDD.name),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_5_MEDFØLGENDE_A_E,
            ),
            Vilkår(
                FTRL_2_5_FORSØRGET_FAMILIEMEDLEM
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_ANDRE_LEDD, ektefelle a til b`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_A_TIL_B.name),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_5_MEDFØLGENDE_A_E,
                defaultOppfylt = true
            ),
            Vilkår(
                FTRL_2_5_FORSØRGET_FAMILIEMEDLEM
            ),
            Vilkår(
                FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_ANDRE_LEDD, ektefelle c til e`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_5_ANDRE_LEDD_C_TIL_E.name),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_5_MEDFØLGENDE_A_E,
                defaultOppfylt = true
            ),
            Vilkår(
                FTRL_2_5_FORSØRGET_FAMILIEMEDLEM
            ),
            Vilkår(
                FTRL_FORUTGÅENDE_TRYGDETID
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_7_FJERDE_LEDD`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FJERDE_LEDD,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1A_TRYGDEKOORDINGERING
            ),
            Vilkår(
                FTRL_2_7_FORSØRGET_FAMILIEMEDLEM
            ),
            Vilkår(
                FTRL_2_7_INGEN_SÆRLIGE_GRUNNER_TALER_IMOT
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_FØRSTE_LEDD_B`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1A_TRYGDEKOORDINGERING
            ),
            Vilkår(
                FTRL_2_8_STUDENT_UVIVERSITET_HØGSKOLE
            ),
            Vilkår(
                FTRL_FORUTGÅENDE_TRYGDETID
            ),
            Vilkår(
                FTRL_2_8_NÆR_TILKNYTNING_NORGE
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_FØRSTE_LEDD_C`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_C,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1A_TRYGDEKOORDINGERING
            ),
            Vilkår(
                FTRL_FORUTGÅENDE_TRYGDETID
            ),
            Vilkår(
                FTRL_2_8_NÆR_TILKNYTNING_NORGE
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_ANDRE_LEDD, særlig grunn`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
            emptyMap(),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1A_TRYGDEKOORDINGERING
            ),
            Vilkår(
                FTRL_FORUTGÅENDE_TRYGDETID
            ),
            Vilkår(
                FTRL_2_8_NÆR_TILKNYTNING_NORGE
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_FJERDE_LEDD, barn`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.BARN_2_8_FJERDE_LEDD.name),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1A_TRYGDEKOORDINGERING
            ),
            Vilkår(
                FTRL_2_8_FORSØRGET_FAMILIEMEDLEM
            ),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_FJERDE_LEDD, ektefelle`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD.name),
            1L
        )


        vilkår.shouldContainExactly(
            Vilkår(
                FTRL_2_1A_TRYGDEKOORDINGERING
            ),
            Vilkår(
                FTRL_2_8_FORSØRGET_FAMILIEMEDLEM
            ),
            Vilkår(
                FTRL_FORUTGÅENDE_TRYGDETID
            ),
            Vilkår(
                FTRL_2_8_NÆR_TILKNYTNING_NORGE
            ),
        )
    }
}
