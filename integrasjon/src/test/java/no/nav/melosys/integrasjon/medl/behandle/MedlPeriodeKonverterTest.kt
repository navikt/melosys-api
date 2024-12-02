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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class MedlPeriodeKonverterTest {
    @Test
    fun tilGrunnlagMedltype() {
        Assertions.assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))
            .isEqualTo(GrunnlagMedl.FO_12_2)
        Assertions.assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1))
            .isEqualTo(GrunnlagMedl.FO_16)
        Assertions.assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2))
            .isEqualTo(GrunnlagMedl.FO_16)
        Assertions.assertThat(tilGrunnlagMedltype(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3))
            .isEqualTo(GrunnlagMedl.STORBRIT_NIRLAND_7_3)
        Assertions.assertThat(tilGrunnlagMedltype(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11))
            .isEqualTo(GrunnlagMedl.FO_987_2009_14_11)

        Assertions.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1) }
            .withMessageContaining("støttes ikke i MEDL")
    }

    @Test
    fun tilGrunnlagMedltype_FtrlOgSpesielleGrupper() {
        Assertions.assertThat(tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A))
            .isEqualTo(GrunnlagMedl.FTL_2_8_1_LEDD_A)
        Assertions.assertThat(tilGrunnlagMedltype(Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO))
            .isEqualTo(GrunnlagMedl.TILLEGGSAVTALE_NATO)
        Assertions.assertThat(tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B))
            .isEqualTo(GrunnlagMedl.FTL_2_8_1_LEDD_B)
        Assertions.assertThat(tilGrunnlagMedltype(Vertslandsavtale_bestemmelser.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14))
            .isEqualTo(GrunnlagMedl.DET_INTERNASJONALE_BARENTSSEKRETARIATET_14)

        Assertions.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5) }
            .withMessageContaining("Folketrygdloven bestemmelse støttes ikke.")
    }

    @Test
    fun tilLovvalgBestemmelse() {
        Assertions.assertThat(tilLovvalgBestemmelse(GrunnlagMedl.FO_12_2))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2)
        Assertions.assertThat(tilLovvalgBestemmelse(GrunnlagMedl.FO_16))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1)

        Assertions.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilLovvalgBestemmelse(GrunnlagMedl.MEDFT) }
            .withMessageContaining("GrunnlagMedlKode er ukjent")
    }

    @Test
    fun tilMedlTrygdedekningForFtrl() {
        Assertions.assertThat(tilMedlTrygdedekningForFtrl(Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL))
            .isEqualTo(DekningMedl.TILLEGSAVTALE_NATO_DEKNING)

        Assertions.assertThatExceptionOfType(TekniskException::class.java)
            .isThrownBy { tilMedlTrygdedekningForFtrl(Trygdedekninger.UNNTATT_USA_5_2_G) }
            .withMessageContaining("Dekningstype støttes ikke for FTRL: %s".formatted(Trygdedekninger.UNNTATT_USA_5_2_G.kode))
    }

    @Test
    fun hentFellesKodeForDekningtype() {
        val trygdeDekning = Trygdedekninger.UTEN_DEKNING
        val dekningMedl = tilMedlTrygdeDekning(trygdeDekning)
        Assertions.assertThat(dekningMedl).isEqualTo(DekningMedl.UNNTATT)
    }

    @Test
    fun hentLovvalgbestemmelse() {
        Assertions.assertThat(hentLovvalgBestemmelse(lovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null)))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        Assertions.assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
                )
            )
        )
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        Assertions.assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
                )
            )
        )
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        Assertions.assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A,
                    Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1
                )
            )
        )
            .isEqualTo(Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1)
        Assertions.assertThat(
            hentLovvalgBestemmelse(
                lovvalgsperiode(
                    Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
                )
            )
        )
            .isEqualTo(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1)
        Assertions.assertThat(
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
