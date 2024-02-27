package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
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
            Vilkår(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID),
            Vilkår(Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE, muligeBegrunnelser = toStringList(*Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values())),
        )
    }
}
