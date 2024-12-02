package no.nav.melosys.integrasjon.medl.behandle

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.medl.DekningMedl
import no.nav.melosys.integrasjon.medl.GrunnlagMedl
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.hentLovvalgBestemmelse
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilGrunnlagMedltype
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilLovvalgBestemmelse
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilMedlTrygdeDekning
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilMedlTrygdedekningForFtrl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

internal class MedlPeriodeKonverterTest {
    @Test
    fun tilGrunnlagMedltype() {
        assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))
            .isEqualTo(GrunnlagMedl.FO_12_2)
        assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1))
            .isEqualTo(GrunnlagMedl.FO_16)
        assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2))
            .isEqualTo(GrunnlagMedl.FO_16)
        assertThat(tilGrunnlagMedltype(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3))
            .isEqualTo(GrunnlagMedl.STORBRIT_NIRLAND_7_3)
        assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11))
            .isEqualTo(GrunnlagMedl.FO_987_2009_14_11)

        assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1) }
            .withMessageContaining("støttes ikke i MEDL")
    }

    @Test
    fun tilGrunnlagMedltype_FtrlOgSpesielleGrupper() {
        assertThat(tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A))
            .isEqualTo(GrunnlagMedl.FTL_2_8_1_LEDD_A)
        assertThat(tilGrunnlagMedltype(Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO))
            .isEqualTo(GrunnlagMedl.TILLEGGSAVTALE_NATO)
        assertThat(tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B))
            .isEqualTo(GrunnlagMedl.FTL_2_8_1_LEDD_B)
        assertThat(tilGrunnlagMedltype(Vertslandsavtale_bestemmelser.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14))
            .isEqualTo(GrunnlagMedl.DET_INTERNASJONALE_BARENTSSEKRETARIATET_14)

        assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5) }
            .withMessageContaining("Folketrygdloven bestemmelse støttes ikke.")
    }

    @Test
    fun tilLovvalgBestemmelse() {
        assertThat(tilLovvalgBestemmelse(GrunnlagMedl.FO_12_2))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2)
        assertThat(tilLovvalgBestemmelse(GrunnlagMedl.FO_16))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1)

        assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilLovvalgBestemmelse(GrunnlagMedl.MEDFT) }
            .withMessageContaining("GrunnlagMedlKode er ukjent")
    }

    @Test
    fun tilMedlTrygdedekningForFtrl() {
        assertThat(tilMedlTrygdedekningForFtrl(Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL))
            .isEqualTo(DekningMedl.TILLEGSAVTALE_NATO_DEKNING)

        assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilMedlTrygdedekningForFtrl(Trygdedekninger.UNNTATT_USA_5_2_G) }
            .withMessageContaining("Dekningstype støttes ikke for FTRL: %s".formatted(Trygdedekninger.UNNTATT_USA_5_2_G.kode))
    }

    @Test
    fun hentFellesKodeForDekningtype() {
        val trygdeDekning = Trygdedekninger.UTEN_DEKNING
        val dekningMedl = tilMedlTrygdeDekning(trygdeDekning)
        assertThat(dekningMedl).isEqualTo(DekningMedl.UNNTATT)
    }

    @Test
    fun hentLovvalgbestemmelse() {
        assertThat(hentLovvalgBestemmelse(lovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null)))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
                )
            )
        )
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
                )
            )
        )
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A,
                    Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1
                )
            )
        )
            .isEqualTo(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1)
        assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
                )
            )
        )
            .isEqualTo(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1)
        assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
                )
            )
        )
            .isEqualTo(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5)
    }

    private fun lovvalgsperiode(lovvalgBestemmelse: LovvalgBestemmelse, tilleggBestemmelse: LovvalgBestemmelse?): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.bestemmelse = lovvalgBestemmelse
        lovvalgsperiode.tilleggsbestemmelse = tilleggBestemmelse
        return lovvalgsperiode
    }
}
