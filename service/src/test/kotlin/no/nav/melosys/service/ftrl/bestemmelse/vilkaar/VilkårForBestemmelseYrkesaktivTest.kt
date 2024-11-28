package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelseYrkesaktiv.Companion.toStringList
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VilkårForBestemmelseYrkesaktivTest {
    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private lateinit var vilkårForBestemmelse: VilkårForBestemmelseYrkesaktiv


    @BeforeEach
    fun setUp() {
        vilkårForBestemmelse = VilkårForBestemmelseYrkesaktiv(mottatteOpplysningerService)
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1_kun_norge`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply {
                        soeknadsland = Soeknadsland().apply {
                            landkoder = listOf(
                                Land_iso2.NO.toString()
                            )
                        }
                    }
            }

        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns mottatteOpplysninger

        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1_BOSATT_NORGE),
            Vilkår(Vilkaar.FTRL_2_11_UNNTAK_AMBASSADEPERSONELL_MELLOMFOLKELIG_ORG),
            Vilkår(Vilkaar.FTRL_2_1_LOVLIG_OPPHOLD),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1 ett eller flere land utenfor norge, MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply {
                        soeknadsland = Soeknadsland().apply {
                            landkoder = listOf(
                                Land_iso2.NO.toString(), Land_iso2.BA.toString()
                            )
                        }
                    }
            }

        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns mottatteOpplysninger

        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
            mapOf(Avklartefaktatyper.ARBEIDSSITUASJON to Arbeidssituasjontype.MIDLERTIDIG_ARBEID_2_1_FJERDE_LEDD.name),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1_BOSATT_NORGE_FORUT),
            Vilkår(Vilkaar.FTRL_2_1_ARBEID_OPPHOLD_UNDER_12MND),
            Vilkår(Vilkaar.FTRL_2_14_ARBEIDSGIVERAVGIFT),
            Vilkår(Vilkaar.FTRL_2_1_LOVLIG_OPPHOLD)
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_1 ett eller flere land utenfor norge, VEKSELVIS_ARBEID_2_1_FJERDE_LEDD`() {
        val mottatteOpplysninger =
            MottatteOpplysninger().apply {
                mottatteOpplysningerData =
                    SøknadNorgeEllerUtenforEØS().apply {
                        soeknadsland = Soeknadsland().apply {
                            landkoder = listOf(
                                Land_iso2.NO.toString(), Land_iso2.BA.toString()
                            )
                        }
                    }
            }

        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns mottatteOpplysninger

        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1,
            mapOf(Avklartefaktatyper.ARBEIDSSITUASJON to Arbeidssituasjontype.VEKSELVIS_ARBEID_2_1_FJERDE_LEDD.name),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1_VEKSELSVIS_ARBEIDSPERIODE_UNDER_12MND),
            Vilkår(Vilkaar.FTRL_2_14_FRITID_I_NORGE),
            Vilkår(Vilkaar.FTRL_2_14_ARBEIDSGIVERAVGIFT),
            Vilkår(Vilkaar.FTRL_2_1_LOVLIG_OPPHOLD)
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_2, ARBIED_I_NORGE_2_2`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2,
            mapOf(Avklartefaktatyper.ARBEIDSSITUASJON to Arbeidssituasjontype.ARBIED_I_NORGE_2_2.name),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_11_UNNTAK_AMBASSADEPERSONELL_MELLOMFOLKELIG_ORG),
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_2_LOVLIG_ADGANG_ARBEID),
        )
    }


    @Test
    fun `vilkår for FTRL_KAP2_2_2, ARBEID_PÅ_NORSK_SOKKEL_2_2`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2,
            mapOf(Avklartefaktatyper.ARBEIDSSITUASJON to Arbeidssituasjontype.ARBEID_PÅ_NORSK_SOKKEL_2_2.name),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_2_INNRETNING_NATURRESSURSER),
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_2_LOVLIG_ADGANG_ARBEID),
        )
    }


    @Test
    fun `vilkår for FTRL_KAP2_2_3_ANDRE_LEDD`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_3_ARBEIDSGIVER_SVALBARD_JAN_MAYEN),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_A`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_5_NORSKE_STATS_TJENESTE),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_B`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_B,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_5_ARBEID_FOR_PERSON_I_NORSKE_STATS_TJENESTE),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_C`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_C,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_I_FORSVARETS_TJENESTE),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_D`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_D,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_FREDSKORPSDELTAKER_EKSPERT_UTVIKLINGSLAND),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_E`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_NATO_SIVILE_KRIGSTIDSORGANGER),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_F`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_F,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_5_NORSK_SKIP),
            Vilkår(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_5_FØRSTE_LEDD_G`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_G,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_5_NORSK_STATSBORGER_EØS_BORGER),
            Vilkår(Vilkaar.FTRL_ARBEIDSTAKER),
            Vilkår(Vilkaar.FTRL_2_5_NORSK_SIVILT_LUFTFARTSSELSKAP),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_7_FØRSTE_LEDD`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING),
            Vilkår(Vilkaar.FTRL_2_7_IKKE_PLIKTIG_MEDLEM),
            Vilkår(Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING, muligeBegrunnelser = toStringList(*Ftrl_2_7_begrunnelser.values())),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_7A`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING),
            Vilkår(Vilkaar.FTRL_2_7A_BOSATT_I_NORGE),
            Vilkår(Vilkaar.FTRL_2_7A_SKIP_UTENFOR_EØS),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_FØRSTE_LEDD_A`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING),
            Vilkår(Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID),
            Vilkår(Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE),
        )
    }

    @Test
    fun `vilkår for FTRL_KAP2_2_8_ANDRE_LEDD`() {
        val vilkår = vilkårForBestemmelse.hentVilkår(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
            emptyMap(),
            1L
        )

        vilkår.shouldContainExactly(
            Vilkår(Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING),
            Vilkår(Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID),
            Vilkår(Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE, muligeBegrunnelser = toStringList(*Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values())),
        )
    }

    @Test
    fun `vilkår for ARKTISK_RÅDS_SEKRETARIAT_ART16`() {
        vilkårForBestemmelse.hentVilkår(
            Vertslandsavtale_bestemmelser.ARKTISK_RÅDS_SEKRETARIAT_ART16,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD.name),
            1L
        ).shouldContainExactly(Vilkår(Vilkaar.ARKTISK_RÅDS_SEKRETARIAT_ART16_STABSMEDLEM))
    }

    @Test
    fun `vilkår for DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14`() {
        vilkårForBestemmelse.hentVilkår(
            Vertslandsavtale_bestemmelser.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD.name),
            1L
        ).shouldContainExactly(Vilkår(Vilkaar.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14_FAST_STAB))
    }

    @Test
    fun `vilkår for DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16`() {
        vilkårForBestemmelse.hentVilkår(
            Vertslandsavtale_bestemmelser.DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD.name),
            1L
        ).shouldContainExactly(Vilkår(Vilkaar.DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16_TJENESTEMANN))
    }

    @Test
    fun `vilkår for TILLEGGSAVTALE_NATO`() {
        vilkårForBestemmelse.hentVilkår(
            Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO,
            mapOf(Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON to Ikkeyrkesaktivrelasjontype.EKTEFELLE_2_8_FJERDE_LEDD.name),
            1L
        ).shouldContainExactly(Vilkår(Vilkaar.TILLEGGSAVTALE_NATO_SIVILT_ANSATT))
    }
}
