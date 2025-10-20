package no.nav.melosys.integrasjon.medl

import com.google.common.collect.HashBiMap
import no.nav.melosys.domain.PeriodeOmLovvalg
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.*
import no.nav.melosys.exception.TekniskException
import java.util.*

object MedlPeriodeKonverter {

    private var lovvalgsbestemmelseTilGrunnlagMedlTabell = HashBiMap.create<LovvalgBestemmelse, GrunnlagMedl>().apply {
        putAll(
            mapOf(
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A to GrunnlagMedl.FO_11_3_A,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B to GrunnlagMedl.FO_11_3_B,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C to GrunnlagMedl.FO_11_3_C,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D to GrunnlagMedl.FO_11_3_D,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E to GrunnlagMedl.FO_11_3_E,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4 to GrunnlagMedl.FO_11_4,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1 to GrunnlagMedl.FO_11_4_1,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2 to GrunnlagMedl.FO_11_4_2,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5 to GrunnlagMedl.FO_11_5,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2 to GrunnlagMedl.FO_11_2,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1 to GrunnlagMedl.FO_12_1,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2 to GrunnlagMedl.FO_12_2,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A to GrunnlagMedl.FO_13_1_A,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1 to GrunnlagMedl.FO_13_1_B,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2 to GrunnlagMedl.FO_13_B_II,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3 to GrunnlagMedl.FO_13_B_III,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4 to GrunnlagMedl.FO_13_B_IV,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A to GrunnlagMedl.FO_13_2_A,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B to GrunnlagMedl.FO_13_2_B,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3 to GrunnlagMedl.FO_13_3,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4 to GrunnlagMedl.FO_13_4,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART15 to GrunnlagMedl.FO_15,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1 to GrunnlagMedl.FO_16,
                Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11 to GrunnlagMedl.FO_987_2009_14_11,
            )
        )
        // Storbritannia KONV EFTA
        putAll(
            mapOf(
                Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_4_1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_3_A,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_2 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_13_4_2,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_14_1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_14_2,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_16_1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_16_3,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1 to GrunnlagMedl.KONV_STORBRIT_NIRLAND_18_1,
            )
        )
        //Australia
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_au.AUS to GrunnlagMedl.AUSTRALIA,
                Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_2 to GrunnlagMedl.AUS_9_2,
                Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3 to GrunnlagMedl.AUS_9_3,
                Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART11 to GrunnlagMedl.AUS_11,
                Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART14_1 to GrunnlagMedl.AUS_14_1,
            )
        )
        //Bosnia og Hercegovina
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH to GrunnlagMedl.BOSNIA_HERCEGOVINA,
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART3 to GrunnlagMedl.BOSNIA_HERCEGOVINA_3,
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART4 to GrunnlagMedl.BOSNIA_HERCEGOVINA_4,
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART4_A to GrunnlagMedl.BOSNIA_HERCEGOVINA_4_A,
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART5 to GrunnlagMedl.BOSNIA_HERCEGOVINA_5,
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART6 to GrunnlagMedl.BOSNIA_HERCEGOVINA_6,
                Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART7 to GrunnlagMedl.BOSNIA_HERCEGOVINA_7,
            )
        )
        // Canada
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN to GrunnlagMedl.CANADA,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2 to GrunnlagMedl.CANADA_6_2,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7 to GrunnlagMedl.CANADA_7,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7_4 to GrunnlagMedl.CANADA_7_4,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART9 to GrunnlagMedl.CANADA_9,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10 to GrunnlagMedl.CANADA_10,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10_4 to GrunnlagMedl.CANADA_10_4,
                Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11 to GrunnlagMedl.CANADA_11,
            )
        )
        // Chile
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_cl.CHL to GrunnlagMedl.CHILE,
                Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART5_2 to GrunnlagMedl.CHILE_5_2,
                Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART6 to GrunnlagMedl.CHILE_6,
                Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART6_4 to GrunnlagMedl.CHILE_6_4,
                Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART8 to GrunnlagMedl.CHILE_8,
            )
        )
        // Frankrike
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_fr.FRA to GrunnlagMedl.FRANKRIKE,
            )
        )
        // Hellas
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_gr.GRC to GrunnlagMedl.HELLAS,
                Lovvalgsbestemmelser_trygdeavtale_gr.GRC_ART4_A to GrunnlagMedl.HELLAS_4_A,
                Lovvalgsbestemmelser_trygdeavtale_gr.GRC_ART7 to GrunnlagMedl.HELLAS_7,
            )
        )
        // India
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_in.IND to GrunnlagMedl.INDIA,
                Lovvalgsbestemmelser_trygdeavtale_in.IND_ART8 to GrunnlagMedl.INDIA_8,
                Lovvalgsbestemmelser_trygdeavtale_in.IND_ART8_1 to GrunnlagMedl.INDIA_8_1,
                Lovvalgsbestemmelser_trygdeavtale_in.IND_ART9 to GrunnlagMedl.INDIA_9,
                Lovvalgsbestemmelser_trygdeavtale_in.IND_ART9_1 to GrunnlagMedl.INDIA_9_1,
                Lovvalgsbestemmelser_trygdeavtale_in.IND_ART10 to GrunnlagMedl.INDIA_10,
            )
        )
        // Israel
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_il.ISR to GrunnlagMedl.ISRAEL,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_4 to GrunnlagMedl.ISRAEL_6_4,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_5 to GrunnlagMedl.ISRAEL_6_5,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_7 to GrunnlagMedl.ISRAEL_6_7,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART7 to GrunnlagMedl.ISRAEL_7,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART8 to GrunnlagMedl.ISRAEL_8,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_1 to GrunnlagMedl.ISRAEL_9_1,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_2 to GrunnlagMedl.ISRAEL_9_2,
                Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_3 to GrunnlagMedl.ISRAEL_9_3,
            )
        )
        // Italia
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_it.ITA to GrunnlagMedl.ITALIA,
            )
        )
        // Kroatia
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV to GrunnlagMedl.KROATIA,
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART3 to GrunnlagMedl.KROATIA_3,
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART4 to GrunnlagMedl.KROATIA_4,
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART4_A to GrunnlagMedl.KROATIA_4_A,
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART5 to GrunnlagMedl.KROATIA_5,
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART6 to GrunnlagMedl.KROATIA_6,
                Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART7 to GrunnlagMedl.KROATIA_7,
            )
        )
        // Montenegro
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_me.MNE to GrunnlagMedl.MONTENEGRO,
                Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART3 to GrunnlagMedl.MONTENEGRO_3,
                Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART4 to GrunnlagMedl.MONTENEGRO_4,
                Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART4_A to GrunnlagMedl.MONTENEGRO_4_A,
                Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART5 to GrunnlagMedl.MONTENEGRO_5,
                Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART6 to GrunnlagMedl.MONTENEGRO_6,
                Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART7 to GrunnlagMedl.MONTENEGRO_7,
            )
        )
        // Portugal
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_pt.PRT to GrunnlagMedl.PORTUGAL,
                Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART8 to GrunnlagMedl.PORTUGAL_8,
                Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART8_1 to GrunnlagMedl.PORTUGAL_8_1,
                Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART9 to GrunnlagMedl.PORTUGAL_9,
                Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART10 to GrunnlagMedl.PORTUGAL_10,
                Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART11 to GrunnlagMedl.PORTUGAL_11,
            )
        )
        // Quebec
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE to GrunnlagMedl.QUEBEC,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART6_3 to GrunnlagMedl.QUEBEC_6_3,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7 to GrunnlagMedl.QUEBEC_7,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7_3 to GrunnlagMedl.QUEBEC_7_3,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART8 to GrunnlagMedl.QUEBEC_8,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART9 to GrunnlagMedl.QUEBEC_9,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART9_3 to GrunnlagMedl.QUEBEC_9_3,
                Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART10 to GrunnlagMedl.QUEBEC_10,
            )
        )
        // Serbia
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB to GrunnlagMedl.SERBIA,
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART3 to GrunnlagMedl.SERBIA_3,
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART4 to GrunnlagMedl.SERBIA_4,
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART4_A to GrunnlagMedl.SERBIA_4_A,
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART5 to GrunnlagMedl.SERBIA_5,
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART6 to GrunnlagMedl.SERBIA_6,
                Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART7 to GrunnlagMedl.SERBIA_7,
            )
        )
        // Slovenia
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_si.SVN to GrunnlagMedl.SLOVENIA,
                Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART3 to GrunnlagMedl.SLOVENIA_3,
                Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART4 to GrunnlagMedl.SLOVENIA_4,
                Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART4_A to GrunnlagMedl.SLOVENIA_4_A,
                Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART5 to GrunnlagMedl.SLOVENIA_5,
                Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART6 to GrunnlagMedl.SLOVENIA_6,
                Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART7 to GrunnlagMedl.SLOVENIA_7,
            )
        )
        // Storbritannia og Nord-Irland
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_gb.UK to GrunnlagMedl.STORBRIT_NIRLAND,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1 to GrunnlagMedl.STORBRIT_NIRLAND_6_1,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_5 to GrunnlagMedl.STORBRIT_NIRLAND_6_5,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_10 to GrunnlagMedl.STORBRIT_NIRLAND_6_10,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_1 to GrunnlagMedl.STORBRIT_NIRLAND_7_1,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3 to GrunnlagMedl.STORBRIT_NIRLAND_7_3,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2 to GrunnlagMedl.STORBRIT_NIRLAND_8_2,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART9 to GrunnlagMedl.STORBRIT_NIRLAND_9,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART5_4 to GrunnlagMedl.STORBRIT_NIRLAND_5_4,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_2 to GrunnlagMedl.STORBRIT_NIRLAND_6_2,
                Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_5 to GrunnlagMedl.STORBRIT_NIRLAND_8_5,
            )
        )
        // Sveits
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_ch.CHE to GrunnlagMedl.SVEITS,
                Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART8 to GrunnlagMedl.SVEITS_8,
                Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART8_1_A to GrunnlagMedl.SVEITS_8_1_A,
                Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART9 to GrunnlagMedl.SVEITS_9,
                Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART10 to GrunnlagMedl.SVEITS_10,
                Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART11 to GrunnlagMedl.SVEITS_11,
            )
        )
        // Tyrkia
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_tr.TUR to GrunnlagMedl.TYRKIA,
                Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART3 to GrunnlagMedl.TYRKIA_3,
                Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART4 to GrunnlagMedl.TYRKIA_4,
                Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART4_A to GrunnlagMedl.TYRKIA_4_A,
                Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART5 to GrunnlagMedl.TYRKIA_5,
                Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART6 to GrunnlagMedl.TYRKIA_6,
            )
        )
        // USA
        putAll(
            mapOf(
                Lovvalgsbestemmelser_trygdeavtale_us.USA to GrunnlagMedl.USA,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_1 to GrunnlagMedl.USA_5_1,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2 to GrunnlagMedl.USA_5_2,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_3 to GrunnlagMedl.USA_5_3,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4 to GrunnlagMedl.USA_5_4,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_5 to GrunnlagMedl.USA_5_5,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6 to GrunnlagMedl.USA_5_6,
                Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9 to GrunnlagMedl.USA_5_9,
                Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A to GrunnlagMedl.FO_1408_14_2_A,
                Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B to GrunnlagMedl.FO_1408_14_2_B,
                Overgangsregelbestemmelser.FO_1408_1971_ART14A_2 to GrunnlagMedl.FO_1408_14_A_2,
                Overgangsregelbestemmelser.FO_1408_1971_ART14C_A to GrunnlagMedl.FO_1408_14_C_A,
                Overgangsregelbestemmelser.FO_1408_1971_ART14C_B to GrunnlagMedl.FO_1408_14_C_B,
            )
        )
    }
    private var ftrlKap2OgSpesielleGrupperBestemmelserTilGrunnLagMedlTabell = HashBiMap.create<Bestemmelse, GrunnlagMedl>().apply {
        // Folketrygdloven kapittel 2
        putAll(
            mapOf(
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1 to GrunnlagMedl.FTL_2_1,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2 to GrunnlagMedl.FTL_2_2,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD to GrunnlagMedl.FTL_2_3_2_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A to GrunnlagMedl.FTL_2_5_1_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_B to GrunnlagMedl.FTL_2_5_1_LEDD_B,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_C to GrunnlagMedl.FTL_2_5_1_LEDD_C,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_D to GrunnlagMedl.FTL_2_5_1_LEDD_D,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E to GrunnlagMedl.FTL_2_5_1_LEDD_E,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_F to GrunnlagMedl.FTL_2_5_1_LEDD_F,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_G to GrunnlagMedl.FTL_2_5_1_LEDD_G,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H to GrunnlagMedl.FTL_2_5_1_LEDD_H,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD to GrunnlagMedl.FTL_2_5_2_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A to GrunnlagMedl.FTL_2_7A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD to GrunnlagMedl.FTL_2_7_1_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FJERDE_LEDD to GrunnlagMedl.FTL_2_7_4_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A to GrunnlagMedl.FTL_2_8_1_LEDD_A,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B to GrunnlagMedl.FTL_2_8_1_LEDD_B,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_C to GrunnlagMedl.FTL_2_8_1_LEDD_C,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD to GrunnlagMedl.FTL_2_8_2_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD to GrunnlagMedl.FTL_2_8_4_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD to GrunnlagMedl.FTL_2_15_2_LEDD,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_D to GrunnlagMedl.FTL_2_8_1_LEDD_D
            )
        )
        // Spesielle grupper
        putAll(
            mapOf(
                Vertslandsavtale_bestemmelser.ARKTISK_RÅDS_SEKRETARIAT_ART16 to GrunnlagMedl.ARKTISK_RÅDS_SEKRETARIAT_16,
                Vertslandsavtale_bestemmelser
                    .DET_INTERNASJONALE_BARENTSSEKRETARIATET_ART14 to GrunnlagMedl.DET_INTERNASJONALE_BARENTSSEKRETARIATET_14,
                Vertslandsavtale_bestemmelser
                    .DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJON_ART16 to GrunnlagMedl.DEN_NORDATLANTISKE_SJØPATTEDYRKOMMISJONEN_16,
                Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO to GrunnlagMedl.TILLEGGSAVTALE_NATO,
            )
        )
    }

    fun tilMedlTrygdeDekning(dekning: Trygdedekninger): DekningMedl =
        when (dekning) {
            Trygdedekninger.FULL_DEKNING_EOSFO, Trygdedekninger.FULL_DEKNING_FTRL, Trygdedekninger.FULL_DEKNING -> DekningMedl.FULL
            Trygdedekninger.UTEN_DEKNING -> DekningMedl.UNNTATT
            Trygdedekninger.UNNTATT_CAN_7_5_B, Trygdedekninger.UNNTATT_USA_5_2_G -> DekningMedl.IKKE_PENSJONSDEL
            else -> throw TekniskException("Dekningstype støttes ikke: ${dekning.kode}")
        }

    fun tilMedlTrygdedekningForFtrl(dekning: Trygdedekninger): DekningMedl =
        when (dekning) {
            Trygdedekninger.FULL_DEKNING_FTRL -> DekningMedl.FULL
            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_7_3_LEDD_B
            Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_7A_2_LEDD_B
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE -> DekningMedl.FTRL_2_9_1_LEDD_A
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_9_2_LEDD_1A
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> DekningMedl.FTRL_2_9_1_LEDD_B
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE -> DekningMedl.FTRL_2_9_3_LEDD_1B
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE -> DekningMedl.FTRL_2_9_2_LEDD_3_LEDD_1C
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE -> DekningMedl.FTRL_2_9_3_LEDD_1C
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON -> DekningMedl.FTRL_2_9_1_LEDD_C
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_9_2_LEDD_1C
            Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL -> DekningMedl.TILLEGSAVTALE_NATO_DEKNING
            else -> throw TekniskException("Dekningstype støttes ikke for FTRL: ${dekning.kode}")
        }

    fun tilGrunnlagMedltype(bestemmelse: LovvalgBestemmelse): GrunnlagMedl {
        //ART16_2 er pensjon og brukes foreløpig ikke i Melosys
        //ART16_1 og ART16_2 mappes til samme GrunnlMedl
        return if (bestemmelse == Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2) {
            GrunnlagMedl.FO_16
        } else lovvalgsbestemmelseTilGrunnlagMedlTabell[bestemmelse]
            ?: throw TekniskException("Lovvalgsbestemmelse støttes ikke i MEDL. Kode: ${bestemmelse.kode} Beskrivelse: ${bestemmelse.beskrivelse}")
    }

    fun tilGrunnlagMedltypeFraOvergangsregler(overgangsregelbestemmelser: Overgangsregelbestemmelser): GrunnlagMedl =
        lovvalgsbestemmelseTilGrunnlagMedlTabell[overgangsregelbestemmelser]!!

    fun tilGrunnlagMedltype(bestemmelse: Bestemmelse?): GrunnlagMedl =
        Optional.ofNullable(ftrlKap2OgSpesielleGrupperBestemmelserTilGrunnLagMedlTabell[bestemmelse])
            .orElseThrow {
                TekniskException(
                    "Folketrygdloven bestemmelse støttes ikke. Kode: ${bestemmelse?.kode} Beskrivelse: ${bestemmelse?.beskrivelse}"
                )
            }

    fun tilLovvalgBestemmelse(grunnlagKode: GrunnlagMedl): LovvalgBestemmelse =
        lovvalgsbestemmelseTilGrunnlagMedlTabell.inverse()[grunnlagKode]
            ?: throw TekniskException("GrunnlagMedlKode er ukjent. Kode: ${grunnlagKode.kode}")

    fun hentLovvalgBestemmelse(lovvalgsperiode: PeriodeOmLovvalg): LovvalgBestemmelse {
        val bestemmelse = if (skalTilleggsbestemmelseMappes(lovvalgsperiode)) {
            lovvalgsperiode.getTilleggsbestemmelse()
        } else {
            lovvalgsperiode.getBestemmelse()
        }
        return bestemmelse ?: throw IllegalStateException("Lovvalgsbestemmelse kan ikke være null")
    }

    fun skalTilleggsbestemmelseMappes(lovvalgsperiode: PeriodeOmLovvalg): Boolean {
        val tilleggsbestemmelse = lovvalgsperiode.getTilleggsbestemmelse()

        if (tilleggsbestemmelse != null) {
            return (lovvalgsperiode.bestemmelse === Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A && tilleggsbestemmelse ===
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1) ||
                (lovvalgsperiode.bestemmelse === Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A &&
                    tilleggsbestemmelse === Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1) ||
                (lovvalgsperiode.bestemmelse === Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A && tilleggsbestemmelse ===
                    Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5)
        }
        return false
    }
}
