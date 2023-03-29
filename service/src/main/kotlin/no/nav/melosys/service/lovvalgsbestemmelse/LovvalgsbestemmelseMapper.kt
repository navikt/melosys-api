package no.nav.melosys.service.lovvalgsbestemmelse

import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Land_iso2.*
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_au.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ba.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca_qc.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ch.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_cl.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_fr.FRA
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gr.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_hr.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_il.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_in.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_it.ITA
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_me.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_pt.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_rs.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_si.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_tr.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us.*
import no.nav.melosys.exception.FunksjonellException

class LovvalgsbestemmelseMapper {
    companion object {

        // Oversikt over regler: https://confluence.adeo.no/display/TEESSI/Spesifikke+kodeverk+Trygdeavtaler#SpesifikkekodeverkTrygdeavtaler-Lovvalgsbestemmelser
        fun mapToLovvalgsbestemmelse(
            land: Land_iso2, mappingType: LovvalgsbestemmelseMappingType
        ): Set<LovvalgBestemmelse> {
            return when (land) {
                AU -> mapForAU(mappingType)
                BA -> mapForBA(mappingType)
                CA -> mapForCA(mappingType)
                CL -> mapForCL(mappingType)
                FR -> mapForFR(mappingType)
                GR -> mapForGR(mappingType)
                IT -> mapForIT(mappingType)
                IN -> mapForIN(mappingType)
                IL -> mapForIL(mappingType)
                HR -> mapForHR(mappingType)
                ME -> mapForME(mappingType)
                PT -> mapForPT(mappingType)
                CA_QC -> mapForCAQC(mappingType)
                RS -> mapForRS(mappingType)
                SI -> mapForSI(mappingType)
                GB -> mapForGB(mappingType)
                CH -> mapForCH(mappingType)
                TR -> mapForTR(mappingType)
                US -> mapForUS(mappingType)
                else -> throw FunksjonellException("Støtter ikke mapping til lovvalgsbestemmelse for land ${land}")
            }
        }

        private fun mapForAU(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_au> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf(AUS_ART9_2, AUS_ART9_3, AUS_ART11, AUS_ART14_1)
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(AUS_ART11, AUS_ART14_1)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(AUS, AUS_ART9_2, AUS_ART9_3, AUS_ART11)
            }
        }

        private fun mapForBA(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_ba> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(BIH_ART4_A, BIH_ART5, BIH_ART7)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(BIH, BIH_ART3, BIH_ART4, BIH_ART5, BIH_ART6, BIH_ART7)
            }
        }

        private fun mapForCA(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_ca> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf(CAN_ART6_2, CAN_ART7, CAN_ART10, CAN_ART11)
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(CAN_ART7_4, CAN_ART10_4, CAN_ART11)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(CAN, CAN_ART6_2, CAN_ART7, CAN_ART9, CAN_ART10, CAN_ART11)
            }
        }

        private fun mapForCL(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_cl> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(CHL_ART5_2, CHL_ART6_4, CHL_ART8)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(CHL, CHL_ART6, CHL_ART8)
            }
        }

        private fun mapForFR(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_fr> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV, LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf()

                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(FRA)
            }
        }

        private fun mapForGR(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_gr> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(GRC_ART4_A, GRC_ART7)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(GRC, GRC_ART7)
            }
        }

        private fun mapForIT(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_it> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV, LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(ITA)
            }
        }

        private fun mapForIN(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_in> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV, LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(IND_ART8_1, IND_ART9_1, IND_ART10)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(IND, IND_ART8, IND_ART9, IND_ART10)
            }
        }

        private fun mapForIL(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_il> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(ISR_ART6_5, ISR_ART8, ISR_ART9_1, ISR_ART9_2, ISR_ART9_3)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(ISR, ISR_ART6_4, ISR_ART6_7, ISR_ART7, ISR_ART8)
            }
        }

        private fun mapForHR(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_hr> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(HRV_ART4_A, HRV_ART5, HRV_ART7)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(HRV, HRV_ART3, HRV_ART4, HRV_ART5, HRV_ART6, HRV_ART7)
            }
        }

        private fun mapForME(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_me> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(MNE_ART4_A, MNE_ART5, MNE_ART7)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(MNE, MNE_ART3, MNE_ART4, MNE_ART5, MNE_ART6, MNE_ART7)
            }
        }

        private fun mapForPT(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_pt> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(PRT_ART8_1, PRT_ART11)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(PRT, PRT_ART8, PRT_ART9, PRT_ART10, PRT_ART11)
            }
        }

        private fun mapForCAQC(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_ca_qc> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(QUE_ART7_3, QUE_ART9_3, QUE_ART10)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(QUE, QUE_ART6_3, QUE_ART7, QUE_ART8, QUE_ART9, QUE_ART10)
            }
        }

        private fun mapForRS(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_rs> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(SRB_ART4_A, SRB_ART5, SRB_ART7)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(SRB, SRB_ART3, SRB_ART4, SRB_ART5, SRB_ART6, SRB_ART7)
            }
        }

        private fun mapForSI(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_si> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(SVN_ART4_A, SVN_ART5, SVN_ART7)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(SVN, SVN_ART3, SVN_ART4, SVN_ART5, SVN_ART6, SVN_ART7)
            }
        }

        private fun mapForGB(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_gb> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf(UK_ART6_1, UK_ART6_5, UK_ART7_1, UK_ART8_2, UK_ART9)
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(UK_ART5_4, UK_ART6_2, UK_ART8_5, UK_ART9)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(
                    UK, UK_ART5_4, UK_ART6_1, UK_ART6_10, UK_ART7_1, UK_ART7_3, UK_ART8_2, UK_ART9
                )
            }
        }

        private fun mapForCH(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_ch> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(CHE_ART8_1_A, CHE_ART11)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(CHE, CHE_ART8, CHE_ART9, CHE_ART10, CHE_ART11)
            }
        }

        private fun mapForTR(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_tr> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf()
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(TUR_ART4_A, TUR_ART6)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(TUR, TUR_ART3, TUR_ART4, TUR_ART5, TUR_ART6)
            }
        }

        private fun mapForUS(mappingType: LovvalgsbestemmelseMappingType): Set<Lovvalgsbestemmelser_trygdeavtale_us> {
            return when (mappingType) {
                LovvalgsbestemmelseMappingType.YRKESAKTIV -> setOf(USA_ART5_2, USA_ART5_4, USA_ART5_5, USA_ART5_9)
                LovvalgsbestemmelseMappingType.IKKE_YRKESAKTIV -> setOf(USA_ART5_2, USA_ART5_9)
                LovvalgsbestemmelseMappingType.UNNTAK -> setOf(USA, USA_ART5_2, USA_ART5_3, USA_ART5_4, USA_ART5_5, USA_ART5_6, USA_ART5_9)
            }
        }
    }
}
