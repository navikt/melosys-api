package no.nav.melosys.integrasjon.medl

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import no.nav.melosys.domain.PeriodeOmLovvalg
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.*
import no.nav.melosys.exception.TekniskException
import java.util.*

class MedlPeriodeKonverter private constructor() {
    init {
        throw IllegalStateException("Utility")
    }

    companion object {
        private var lovvalgsbestemmelseTilGrunnlagMedlTabell: BiMap<LovvalgBestemmelse, GrunnlagMedl> = HashBiMap.create()
        private var ftrlKap2BestemmelserTilGrunnLagMedlTabell: BiMap<Folketrygdloven_kap2_bestemmelser, GrunnlagMedl> = HashBiMap.create()
        private val TILLEGGSBESTEMMELSER_MAPPES_TIL_MEDL: Collection<LovvalgBestemmelse> = setOf(
            Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
            Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
        )

        init {
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A] = GrunnlagMedl.FO_11_3_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B] = GrunnlagMedl.FO_11_3_B
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C] = GrunnlagMedl.FO_11_3_C
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D] = GrunnlagMedl.FO_11_3_D
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E] = GrunnlagMedl.FO_11_3_E
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4] = GrunnlagMedl.FO_11_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1] = GrunnlagMedl.FO_11_4_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2] = GrunnlagMedl.FO_11_4_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5] = GrunnlagMedl.FO_11_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2] = GrunnlagMedl.FO_11_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1] = GrunnlagMedl.FO_12_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2] = GrunnlagMedl.FO_12_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A] = GrunnlagMedl.FO_13_1_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1] = GrunnlagMedl.FO_13_1_B
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2] = GrunnlagMedl.FO_13_B_II
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3] = GrunnlagMedl.FO_13_B_III
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4] = GrunnlagMedl.FO_13_B_IV
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A] = GrunnlagMedl.FO_13_2_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B] = GrunnlagMedl.FO_13_2_B
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3] = GrunnlagMedl.FO_13_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4] = GrunnlagMedl.FO_13_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART15] = GrunnlagMedl.FO_15
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1] = GrunnlagMedl.FO_16
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11] = GrunnlagMedl.FO_987_2009_14_11

            // Australia
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_au.AUS] = GrunnlagMedl.AUSTRALIA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_2] = GrunnlagMedl.AUS_9_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3] = GrunnlagMedl.AUS_9_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART11] = GrunnlagMedl.AUS_11
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART14_1] = GrunnlagMedl.AUS_14_1

            // Bosnia og Hercegovina
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH] = GrunnlagMedl.BOSNIA_HERCEGOVINA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART3] = GrunnlagMedl.BOSNIA_HERCEGOVINA_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART4] = GrunnlagMedl.BOSNIA_HERCEGOVINA_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART4_A] = GrunnlagMedl.BOSNIA_HERCEGOVINA_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART5] = GrunnlagMedl.BOSNIA_HERCEGOVINA_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART6] = GrunnlagMedl.BOSNIA_HERCEGOVINA_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART7] = GrunnlagMedl.BOSNIA_HERCEGOVINA_7

            // Canada
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN] = GrunnlagMedl.CANADA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2] = GrunnlagMedl.CANADA_6_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7] = GrunnlagMedl.CANADA_7
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7_4] = GrunnlagMedl.CANADA_7_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART9] = GrunnlagMedl.CANADA_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10] = GrunnlagMedl.CANADA_10
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10_4] = GrunnlagMedl.CANADA_10_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11] = GrunnlagMedl.CANADA_11

            // Chile
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_cl.CHL] = GrunnlagMedl.CHILE
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART5_2] = GrunnlagMedl.CHILE_5_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART6] = GrunnlagMedl.CHILE_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART6_4] = GrunnlagMedl.CHILE_6_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART8] = GrunnlagMedl.CHILE_8

            // Frankrike
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_fr.FRA] = GrunnlagMedl.FRANKRIKE

            // Hellas
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gr.GRC] = GrunnlagMedl.HELLAS
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gr.GRC_ART4_A] = GrunnlagMedl.HELLAS_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gr.GRC_ART7] = GrunnlagMedl.HELLAS_7

            // India
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_in.IND] = GrunnlagMedl.INDIA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_in.IND_ART8] = GrunnlagMedl.INDIA_8
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_in.IND_ART8_1] = GrunnlagMedl.INDIA_8_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_in.IND_ART9] = GrunnlagMedl.INDIA_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_in.IND_ART9_1] = GrunnlagMedl.INDIA_9_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_in.IND_ART10] = GrunnlagMedl.INDIA_10

            // Israel
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR] = GrunnlagMedl.ISRAEL
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_4] = GrunnlagMedl.ISRAEL_6_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_5] = GrunnlagMedl.ISRAEL_6_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_7] = GrunnlagMedl.ISRAEL_6_7
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART7] = GrunnlagMedl.ISRAEL_7
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART8] = GrunnlagMedl.ISRAEL_8
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_1] = GrunnlagMedl.ISRAEL_9_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_2] = GrunnlagMedl.ISRAEL_9_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_3] = GrunnlagMedl.ISRAEL_9_3

            // Italia
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_it.ITA] = GrunnlagMedl.ITALIA

            // Kroatia
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV] = GrunnlagMedl.KORATIA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART3] = GrunnlagMedl.KORATIA_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART4] = GrunnlagMedl.KORATIA_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART4_A] = GrunnlagMedl.KORATIA_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART5] = GrunnlagMedl.KORATIA_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART6] = GrunnlagMedl.KORATIA_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_hr.HRV_ART7] = GrunnlagMedl.KORATIA_7

            // Montenegro
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE] = GrunnlagMedl.MONTENEGRO
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART3] = GrunnlagMedl.MONTENEGRO_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART4] = GrunnlagMedl.MONTENEGRO_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART4_A] = GrunnlagMedl.MONTENEGRO_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART5] = GrunnlagMedl.MONTENEGRO_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART6] = GrunnlagMedl.MONTENEGRO_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART7] = GrunnlagMedl.MONTENEGRO_7

            // Portugal
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_pt.PRT] = GrunnlagMedl.PORTUGAL
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART8] = GrunnlagMedl.PORTUGAL_8
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART8_1] = GrunnlagMedl.PORTUGAL_8_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART9] = GrunnlagMedl.PORTUGAL_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART10] = GrunnlagMedl.PORTUGAL_10
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART11] = GrunnlagMedl.PORTUGAL_11

            // Québec
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE] = GrunnlagMedl.QUEBEC
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART6_3] = GrunnlagMedl.QUEBEC_6_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7] = GrunnlagMedl.QUEBEC_7
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7_3] = GrunnlagMedl.QUEBEC_7_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART8] = GrunnlagMedl.QUEBEC_8
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART9] = GrunnlagMedl.QUEBEC_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART9_3] = GrunnlagMedl.QUEBEC_9_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART10] = GrunnlagMedl.QUEBEC_10

            // Serbia
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB] = GrunnlagMedl.SERBIA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART3] = GrunnlagMedl.SERBIA_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART4] = GrunnlagMedl.SERBIA_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART4_A] = GrunnlagMedl.SERBIA_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART5] = GrunnlagMedl.SERBIA_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART6] = GrunnlagMedl.SERBIA_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART7] = GrunnlagMedl.SERBIA_7

            // Slovenia
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN] = GrunnlagMedl.SLOVENIA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART3] = GrunnlagMedl.SLOVENIA_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART4] = GrunnlagMedl.SLOVENIA_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART4_A] = GrunnlagMedl.SLOVENIA_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART5] = GrunnlagMedl.SLOVENIA_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART6] = GrunnlagMedl.SLOVENIA_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_si.SVN_ART7] = GrunnlagMedl.SLOVENIA_7

            // Storbritannia og Nord-Irland
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK] = GrunnlagMedl.STORBRIT_NIRLAND
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1] = GrunnlagMedl.STORBRIT_NIRLAND_6_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_5] = GrunnlagMedl.STORBRIT_NIRLAND_6_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_10] = GrunnlagMedl.STORBRIT_NIRLAND_6_10
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_1] = GrunnlagMedl.STORBRIT_NIRLAND_7_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3] = GrunnlagMedl.STORBRIT_NIRLAND_7_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2] = GrunnlagMedl.STORBRIT_NIRLAND_8_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART9] = GrunnlagMedl.STORBRIT_NIRLAND_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART5_4] = GrunnlagMedl.STORBRIT_NIRLAND_5_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_2] = GrunnlagMedl.STORBRIT_NIRLAND_6_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_5] = GrunnlagMedl.STORBRIT_NIRLAND_8_5

            // Sveits
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ch.CHE] = GrunnlagMedl.SVEITS
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART8] = GrunnlagMedl.SVEITS_8
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART8_1_A] = GrunnlagMedl.SVEITS_8_1_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART9] = GrunnlagMedl.SVEITS_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART10] = GrunnlagMedl.SVEITS_10
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART11] = GrunnlagMedl.SVEITS_11

            // Tyrkia
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_tr.TUR] = GrunnlagMedl.TYRKIA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART3] = GrunnlagMedl.TYRKIA_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART4] = GrunnlagMedl.TYRKIA_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART4_A] = GrunnlagMedl.TYRKIA_4_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART5] = GrunnlagMedl.TYRKIA_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART6] = GrunnlagMedl.TYRKIA_6

            // USA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA] = GrunnlagMedl.USA
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_1] = GrunnlagMedl.USA_5_1
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2] = GrunnlagMedl.USA_5_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_3] = GrunnlagMedl.USA_5_3
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4] = GrunnlagMedl.USA_5_4
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_5] = GrunnlagMedl.USA_5_5
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6] = GrunnlagMedl.USA_5_6
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9] = GrunnlagMedl.USA_5_9
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A] = GrunnlagMedl.FO_1408_14_2_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B] = GrunnlagMedl.FO_1408_14_2_B
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Overgangsregelbestemmelser.FO_1408_1971_ART14A_2] = GrunnlagMedl.FO_1408_14_A_2
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Overgangsregelbestemmelser.FO_1408_1971_ART14C_A] = GrunnlagMedl.FO_1408_14_C_A
            lovvalgsbestemmelseTilGrunnlagMedlTabell[Overgangsregelbestemmelser.FO_1408_1971_ART14C_B] = GrunnlagMedl.FO_1408_14_C_B

            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1] = GrunnlagMedl.FTL_2_1
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_2] = GrunnlagMedl.FTL_2_2
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD] = GrunnlagMedl.FTL_2_3_2_LEDD
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A] = GrunnlagMedl.FTL_2_5_1_LEDD_A
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_B] = GrunnlagMedl.FTL_2_5_1_LEDD_B
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_C] = GrunnlagMedl.FTL_2_5_1_LEDD_C
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_D] = GrunnlagMedl.FTL_2_5_1_LEDD_D
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_E] = GrunnlagMedl.FTL_2_5_1_LEDD_E
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_F] = GrunnlagMedl.FTL_2_5_1_LEDD_F
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_G] = GrunnlagMedl.FTL_2_5_1_LEDD_G
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_H] = GrunnlagMedl.FTL_2_5_1_LEDD_H
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_ANDRE_LEDD] = GrunnlagMedl.FTL_2_5_2_LEDD
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A] = GrunnlagMedl.FTL_2_7A
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD] = GrunnlagMedl.FTL_2_7_1_LEDD
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FJERDE_LEDD] = GrunnlagMedl.FTL_2_7_4_LEDD
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A] = GrunnlagMedl.FTL_2_8_1_LEDD_A
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B] = GrunnlagMedl.FTL_2_8_1_LEDD_B
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_C] = GrunnlagMedl.FTL_2_8_1_LEDD_C
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD] = GrunnlagMedl.FTL_2_8_2_LEDD
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD] = GrunnlagMedl.FTL_2_8_4_LEDD
            ftrlKap2BestemmelserTilGrunnLagMedlTabell[Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD] = GrunnlagMedl.FTL_2_15_2_LEDD
        }

        @JvmStatic
        fun tilMedlTrygdeDekning(dekning: Trygdedekninger): DekningMedl =
            when (dekning) {
                Trygdedekninger.FULL_DEKNING_EOSFO, Trygdedekninger.FULL_DEKNING_FTRL, Trygdedekninger.FULL_DEKNING -> DekningMedl.FULL
                Trygdedekninger.UTEN_DEKNING -> DekningMedl.UNNTATT
                Trygdedekninger.UNNTATT_CAN_7_5_B, Trygdedekninger.UNNTATT_USA_5_2_G -> DekningMedl.IKKE_PENSJONSDEL
                else -> throw TekniskException("Dekningstype støttes ikke: ${dekning.kode}")
            }

        fun tilMedlTrygdedekningForFtrl(dekning: Trygdedekninger): DekningMedl =
            when (dekning) {
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE -> DekningMedl.FTRL_2_9_1_LEDD_A
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_9_2_LEDD_1A
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON -> DekningMedl.FTRL_2_9_1_LEDD_B
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON -> DekningMedl.FTRL_2_9_1_LEDD_C
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_9_2_LEDD_1C
                Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_7_3_LEDD_B
                Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER -> DekningMedl.FTRL_2_7A_2_LEDD_B
                Trygdedekninger.FULL_DEKNING_FTRL -> DekningMedl.FULL
                else -> throw TekniskException("Dekningstype støttes ikke for FTRL: ${dekning.kode}")
            }

        @JvmStatic
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

        @JvmStatic
        fun tilGrunnlagMedltype(bestemmelse: Folketrygdloven_kap2_bestemmelser): GrunnlagMedl =
            Optional.ofNullable(ftrlKap2BestemmelserTilGrunnLagMedlTabell[bestemmelse])
                .orElseThrow {
                    TekniskException(
                        "Folketrygdloven bestemmelse støttes ikke. Kode: ${bestemmelse.kode} Beskrivelse: ${bestemmelse.beskrivelse}"
                    )
                }

        @JvmStatic
        fun tilLovvalgBestemmelse(grunnlagKode: GrunnlagMedl): LovvalgBestemmelse =
            lovvalgsbestemmelseTilGrunnlagMedlTabell.inverse()[grunnlagKode]
                ?: throw TekniskException("GrunnlagMedlKode er ukjent. Kode: ${grunnlagKode.kode}")

        @JvmStatic
        fun hentLovvalgBestemmelse(lovvalgsperiode: PeriodeOmLovvalg): LovvalgBestemmelse {
            val tilleggsbestemmelseSkalMappes = (lovvalgsperiode.getTilleggsbestemmelse() != null
                && TILLEGGSBESTEMMELSER_MAPPES_TIL_MEDL.contains(lovvalgsperiode.getTilleggsbestemmelse()))
            val bestemmelse = if (tilleggsbestemmelseSkalMappes) {
                lovvalgsperiode.getTilleggsbestemmelse()
            } else {
                lovvalgsperiode.getBestemmelse()
            }
            return bestemmelse
        }
    }
}
