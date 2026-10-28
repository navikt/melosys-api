package no.nav.melosys.integrasjon.medl.behandle

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import org.junit.jupiter.api.Test

internal class MedlPeriodeKonverterTest {
    @Test
    fun tilGrunnlagMedltype() {
        tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2) shouldBe GrunnlagMedl.FO_12_2
        tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1) shouldBe GrunnlagMedl.FO_16
        tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2) shouldBe GrunnlagMedl.FO_16
        tilGrunnlagMedltype(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3) shouldBe GrunnlagMedl.STORBRIT_NIRLAND_7_3
        tilGrunnlagMedltype(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11) shouldBe GrunnlagMedl.FO_987_2009_14_11

        shouldThrow<TekniskException> {
            tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1)
        }.message shouldContain "støttes ikke i MEDL"
    }

    @Test
    fun tilGrunnlagMedltype_konvEftaStorbritannia() {
        mapOf(
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_3_A,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3B to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_3_B,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_4,
            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_5,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_14_1,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_14_2,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1A to GrunnlagMedl.KONV_STORBRIT_NIRLAND_15_1_A,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1B1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_15_1_B_I,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2A to GrunnlagMedl.KONV_STORBRIT_NIRLAND_15_2_A,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2B to GrunnlagMedl.KONV_STORBRIT_NIRLAND_15_2_B,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_3 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_15_3,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_4 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_15_4,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_16_1,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_16_3,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_18_1,
        ).forEach { (bestemmelse, grunnlag) ->
            tilGrunnlagMedltype(bestemmelse) shouldBe grunnlag
            tilLovvalgBestemmelse(grunnlag) shouldBe bestemmelse
        }
    }

    @Test
    fun tilGrunnlagMedltype_FtrlOgSpesielleGrupper() {
        tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A) shouldBe GrunnlagMedl.FTL_2_8_1_LEDD_A
        tilGrunnlagMedltype(Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO) shouldBe GrunnlagMedl.TILLEGGSAVTALE_NATO
        tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B) shouldBe GrunnlagMedl.FTL_2_8_1_LEDD_B
        tilGrunnlagMedltype(Vertslandsavtale_bestemmelser.DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14) shouldBe GrunnlagMedl.DET_INTERNASJONALE_BARENTSSEKRETARIATET_14

        shouldThrow<TekniskException> {
            tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5)
        }.message shouldContain "Folketrygdloven bestemmelse støttes ikke."
    }

    @Test
    fun tilLovvalgBestemmelse() {
        tilLovvalgBestemmelse(GrunnlagMedl.FO_12_2) shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
        tilLovvalgBestemmelse(GrunnlagMedl.FO_16) shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1

        shouldThrow<TekniskException> {
            tilLovvalgBestemmelse(GrunnlagMedl.MEDFT)
        }.message shouldContain "GrunnlagMedlKode er ukjent"
    }

    @Test
    fun tilMedlTrygdedekningForFtrl() {
        tilMedlTrygdedekningForFtrl(Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL) shouldBe DekningMedl.TILLEGSAVTALE_NATO_DEKNING

        shouldThrow<TekniskException> {
            tilMedlTrygdedekningForFtrl(Trygdedekninger.UNNTATT_USA_5_2_G)
        }.message shouldContain "Dekningstype støttes ikke for FTRL: ${Trygdedekninger.UNNTATT_USA_5_2_G.kode}"
    }

    @Test
    fun hentFellesKodeForDekningtype() {
        val trygdedekning = Trygdedekninger.UTEN_DEKNING
        val dekningMedl = tilMedlTrygdeDekning(trygdedekning)
        dekningMedl shouldBe DekningMedl.UNNTATT
    }

    @Test
    fun hentLovvalgbestemmelse() {
        hentLovvalgBestemmelse(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null)) shouldBe Lovvalgbestemmelser_883_2004
            .FO_883_2004_ART12_1

        hentLovvalgBestemmelse(
            lagLovvalgsperiode(
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
            )
        ) shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1

        hentLovvalgBestemmelse(
            lagLovvalgsperiode(
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            )
        ) shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1

        hentLovvalgBestemmelse(
            lagLovvalgsperiode(
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A,
                Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1
            )
        ) shouldBe Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1

        hentLovvalgBestemmelse(
            lagLovvalgsperiode(
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            )
        ) shouldBe Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1

        hentLovvalgBestemmelse(
            lagLovvalgsperiode(
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
            )
        ) shouldBe Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
    }

    private fun lagLovvalgsperiode(lovvalgBestemmelse: LovvalgBestemmelse, tilleggBestemmelse: LovvalgBestemmelse?) =
        Lovvalgsperiode().apply {
            bestemmelse = lovvalgBestemmelse
            tilleggsbestemmelse = tilleggBestemmelse
        }
}
