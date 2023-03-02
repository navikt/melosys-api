package no.nav.melosys.integrasjon.medl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.*;
import no.nav.melosys.exception.TekniskException;

import static java.util.Optional.ofNullable;

public final class MedlPeriodeKonverter {

    private MedlPeriodeKonverter() {
        throw new IllegalStateException("Utility");
    }

    private static final BiMap<LovvalgBestemmelse, GrunnlagMedl> lovvalgsbestemmelseTilGrunnlagMedlTabell;
    private static final Map<Folketrygdloven_kap2_bestemmelser, GrunnlagMedl> ftrlKap2BestemmelserGrunnLagMedlTabell;

    private static final Collection<LovvalgBestemmelse> TILLEGGSBESTEMMELSER_MAPPES_TIL_MEDL = Set.of(
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
    );

    static {
        BiMap<LovvalgBestemmelse, GrunnlagMedl> tbl = HashBiMap.create();
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, GrunnlagMedl.FO_11_3_A);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B, GrunnlagMedl.FO_11_3_B);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C, GrunnlagMedl.FO_11_3_C);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D, GrunnlagMedl.FO_11_3_D);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E, GrunnlagMedl.FO_11_3_E);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4, GrunnlagMedl.FO_11_4);
        tbl.put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1, GrunnlagMedl.FO_11_4_1);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2, GrunnlagMedl.FO_11_4_2);
        tbl.put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5, GrunnlagMedl.FO_11_5);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, GrunnlagMedl.FO_12_1);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2, GrunnlagMedl.FO_12_2);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, GrunnlagMedl.FO_13_1_A);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, GrunnlagMedl.FO_13_1_B);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2, GrunnlagMedl.FO_13_B_II);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3, GrunnlagMedl.FO_13_B_III);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4, GrunnlagMedl.FO_13_B_IV);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, GrunnlagMedl.FO_13_2_A);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B, GrunnlagMedl.FO_13_2_B);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, GrunnlagMedl.FO_13_3);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4, GrunnlagMedl.FO_13_4);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART15, GrunnlagMedl.FO_15);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, GrunnlagMedl.FO_16);
        tbl.put(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11, GrunnlagMedl.FO_987_2009_14_11);

        // Storbritannia og Nord-Irland
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK, GrunnlagMedl.STORBRIT_NIRLAND);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1, GrunnlagMedl.STORBRIT_NIRLAND_6_1);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_5, GrunnlagMedl.STORBRIT_NIRLAND_6_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_10, GrunnlagMedl.STORBRIT_NIRLAND_6_10);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_1, GrunnlagMedl.STORBRIT_NIRLAND_7_1);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3, GrunnlagMedl.STORBRIT_NIRLAND_7_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2, GrunnlagMedl.STORBRIT_NIRLAND_8_2);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART9, GrunnlagMedl.STORBRIT_NIRLAND_9);

        // USA
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_us.USA, GrunnlagMedl.USA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_2, GrunnlagMedl.USA_ART5_2);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4, GrunnlagMedl.USA_ART5_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_5, GrunnlagMedl.USA_ART5_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_6, GrunnlagMedl.USA_ART5_6);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_9, GrunnlagMedl.USA_ART5_9);

        // Canada
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN, GrunnlagMedl.CANADA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2, GrunnlagMedl.CAN_ART6_2);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7, GrunnlagMedl.CAN_ART7);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART7_4, GrunnlagMedl.CAN_ART7_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART9, GrunnlagMedl.CANADA_9);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10, GrunnlagMedl.CANADA_10);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART10_4, GrunnlagMedl.CANADA_10_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART11, GrunnlagMedl.CANADA_11);

        //Australia
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_au.AUS, GrunnlagMedl.AUSTRALIA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_2, GrunnlagMedl.AUS_ART9_2);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART9_3, GrunnlagMedl.AUS_ART9_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART11, GrunnlagMedl.AUS_ART11);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_au.AUS_ART14_1, GrunnlagMedl.AUS_ART14_1);

        //Bosnia og Hercegovina
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH, GrunnlagMedl.BOSNIA_HERCEGOVINA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART3, GrunnlagMedl.BOSNIA_HERCEGOVINA_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART4, GrunnlagMedl.BOSNIA_HERCEGOVINA_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART4_A, GrunnlagMedl.BOSNIA_HERCEGOVINA_4_A);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART5, GrunnlagMedl.BOSNIA_HERCEGOVINA_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART6, GrunnlagMedl.BOSNIA_HERCEGOVINA_6);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ba.BIH_ART7, GrunnlagMedl.BOSNIA_HERCEGOVINA_7);

        //Chile
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_cl.CHL, GrunnlagMedl.CHILE);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART5_2, GrunnlagMedl.CHILE_5_2);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART6, GrunnlagMedl.CHILE_6);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART6_4, GrunnlagMedl.CHILE_6_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_cl.CHL_ART8, GrunnlagMedl.CHILE_8);

        //Frankrike
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_fr.FRA, GrunnlagMedl.FRANKRIKE);

        //Hellas
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gr.GRC, GrunnlagMedl.HELLAS);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gr.GRC_ART4_A, GrunnlagMedl.HELLAS_4_A);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_gr.GRC_ART7, GrunnlagMedl.HELLAS_7);

        //Italia
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_it.ITA, GrunnlagMedl.ITALIA);

        //India
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_in.IND, GrunnlagMedl.INDIA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_in.IND_ART8, GrunnlagMedl.INDIA_8);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_in.IND_ART8_1, GrunnlagMedl.INDIA_8_1);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_in.IND_ART9, GrunnlagMedl.INDIA_9);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_in.IND_ART9_1, GrunnlagMedl.INDIA_9_1);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_in.IND_ART10, GrunnlagMedl.INDIA_10);

        //Israel
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR, GrunnlagMedl.ISRAEL);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_4, GrunnlagMedl.ISRAEL_6_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_5, GrunnlagMedl.ISRAEL_6_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART6_7, GrunnlagMedl.ISRAEL_6_7);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART7, GrunnlagMedl.ISRAEL_7);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART8, GrunnlagMedl.ISRAEL_8);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_1, GrunnlagMedl.ISRAEL_9_1);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_2, GrunnlagMedl.ISRAEL_9_2);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_il.ISR_ART9_3, GrunnlagMedl.ISRAEL_9_3);

        // Montenegro
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE, GrunnlagMedl.MONTENEGRO);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART3, GrunnlagMedl.MONTENEGRO_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART4, GrunnlagMedl.MONTENEGRO_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART4_A, GrunnlagMedl.MONTENEGRO_4_A);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART5, GrunnlagMedl.MONTENEGRO_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART6, GrunnlagMedl.MONTENEGRO_6);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_me.MNE_ART7, GrunnlagMedl.MONTENEGRO_7);

        //Portugal
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_pt.PRT, GrunnlagMedl.PORTUGAL);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART8, GrunnlagMedl.PORTUGAL_8);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART8_1, GrunnlagMedl.PORTUGAL_8_1);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART9, GrunnlagMedl.PORTUGAL_9);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART10, GrunnlagMedl.PORTUGAL_10);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_pt.PRT_ART11, GrunnlagMedl.PORTUGAL_11);

        //Québec
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE, GrunnlagMedl.QUEBEC);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART6_3, GrunnlagMedl.QUEBEC_6_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7, GrunnlagMedl.QUEBEC_7);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART7_3, GrunnlagMedl.QUEBEC_7_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART8, GrunnlagMedl.QUEBEC_8);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART9, GrunnlagMedl.QUEBEC_9);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART9_3, GrunnlagMedl.QUEBEC_9_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ca_qc.QUE_ART10, GrunnlagMedl.QUEBEC_10);

        //Serbia
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB, GrunnlagMedl.SERBIA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART3, GrunnlagMedl.SERBIA_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART4, GrunnlagMedl.SERBIA_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART4_A, GrunnlagMedl.SERBIA_4_A);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART5, GrunnlagMedl.SERBIA_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART6, GrunnlagMedl.SERBIA_6);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_rs.SRB_ART7, GrunnlagMedl.SERBIA_7);

        //Sveits
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ch.CHE, GrunnlagMedl.SVEITS);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART8, GrunnlagMedl.SVEITS_8);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART8_1_A, GrunnlagMedl.SVEITS_8_1_A);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART9, GrunnlagMedl.SVEITS_9);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART10, GrunnlagMedl.SVEITS_10);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_ch.CHE_ART11, GrunnlagMedl.SVEITS_11);

        // Tyrkia
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_tr.TUR, GrunnlagMedl.TYRKIA);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART3, GrunnlagMedl.TYRKIA_3);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART4, GrunnlagMedl.TYRKIA_4);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART4_A, GrunnlagMedl.TYRKIA_4_A);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART5, GrunnlagMedl.TYRKIA_5);
        tbl.put(Lovvalgsbestemmelser_trygdeavtale_tr.TUR_ART6, GrunnlagMedl.TYRKIA_6);


        tbl.put(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A, GrunnlagMedl.FO_1408_14_2_A);
        tbl.put(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B, GrunnlagMedl.FO_1408_14_2_B);
        tbl.put(Overgangsregelbestemmelser.FO_1408_1971_ART14A_2, GrunnlagMedl.FO_1408_14_A_2);
        tbl.put(Overgangsregelbestemmelser.FO_1408_1971_ART14C_A, GrunnlagMedl.FO_1408_14_C_A);
        tbl.put(Overgangsregelbestemmelser.FO_1408_1971_ART14C_B, GrunnlagMedl.FO_1408_14_C_B);

        lovvalgsbestemmelseTilGrunnlagMedlTabell = tbl;


        ftrlKap2BestemmelserGrunnLagMedlTabell = Map.of(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_B, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_C, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_D, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_TREDJE_LEDD, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FJERDE_LEDD, GrunnlagMedl.FTL_2_8,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FEMTE_LEDD, GrunnlagMedl.FTL_2_8
        );
    }


    public static DekningMedl tilMedlTrygdeDekning(Trygdedekninger dekning) {
        return switch (dekning) {
            case FULL_DEKNING_EOSFO, FULL_DEKNING_FTRL -> DekningMedl.FULL;
            case UTEN_DEKNING -> DekningMedl.UNNTATT;
            case UNNTATT_CAN_7_5_B, UNNTATT_USA_5_2_G -> DekningMedl.IKKEPENDEL;
            default -> throw new TekniskException("Dekningstype støttes ikke for EØS:" + dekning.getKode());
        };
    }

    public static DekningMedl tilMedlTrygdeDekningFtrl(Trygdedekninger dekning, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        return switch (bestemmelse) {
            case FTRL_KAP2_2_8, FTRL_KAP2_2_8_FØRSTE_LEDD_A, FTRL_KAP2_2_8_FØRSTE_LEDD_B,
                FTRL_KAP2_2_8_FØRSTE_LEDD_C, FTRL_KAP2_2_8_FØRSTE_LEDD_D, FTRL_KAP2_2_8_ANDRE_LEDD,
                FTRL_KAP2_2_8_TREDJE_LEDD, FTRL_KAP2_2_8_FJERDE_LEDD, FTRL_KAP2_2_8_FEMTE_LEDD ->
                mapForFtrlKap2_8(dekning);
            default -> throw new TekniskException("Bestemmelse støttes ikke for FTRL: " + bestemmelse.getKode());
        };
    }

    private static DekningMedl mapForFtrlKap2_8(Trygdedekninger dekning) {
        return switch (dekning) {
            case HELSEDEL -> DekningMedl.FTRL_2_9_1_LEDD_A;
            case HELSEDEL_MED_SYKE_OG_FORELDREPENGER -> DekningMedl.FTRL_2_9_2_LEDD_1A;
            case PENSJONSDEL -> DekningMedl.FTRL_2_9_1_LEDD_B;
            case HELSE_OG_PENSJONSDEL -> DekningMedl.FTRL_2_9_1_LEDD_C;
            case HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER -> DekningMedl.FTRL_2_9_2_LEDD_1C;
            default -> throw new TekniskException("Dekningstype støttes ikke for FTRL:" + dekning.getKode());
        };
    }

    public static GrunnlagMedl tilGrunnlagMedltype(LovvalgBestemmelse bestemmelse) {
        //ART16_2 er pensjon og brukes foreløpig ikke i Melosys
        //ART16_1 og ART16_2 mappes til samme GrunnlMedl
        if (bestemmelse.equals(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2)) {
            return GrunnlagMedl.FO_16;
        }
        GrunnlagMedl grunnlagMedltype = lovvalgsbestemmelseTilGrunnlagMedlTabell.get(bestemmelse);
        if (grunnlagMedltype == null) {
            throw new TekniskException("Lovvalgsbestemmelse støttes ikke i MEDL. Kode: " + bestemmelse.getKode() + " Beskrivelse: " + bestemmelse.getBeskrivelse());
        }
        return grunnlagMedltype;
    }

    public static GrunnlagMedl tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser bestemmelse) {
        return ofNullable(ftrlKap2BestemmelserGrunnLagMedlTabell.get(bestemmelse))
            .orElseThrow(() -> new TekniskException("Folketrygdloven bestemmelse støttes ikke. Kode: " +
                bestemmelse.getKode() + " Beskrivelse: " + bestemmelse.getBeskrivelse()));
    }

    public static LovvalgBestemmelse tilLovvalgBestemmelse(GrunnlagMedl grunnlagKode) {
        LovvalgBestemmelse lovvalgBestemmelse = lovvalgsbestemmelseTilGrunnlagMedlTabell.inverse().get(grunnlagKode);
        if (lovvalgBestemmelse == null) {
            throw new TekniskException("GrunnlagMedlKode er ukjent. Kode: " + grunnlagKode.getKode());
        }
        return lovvalgBestemmelse;
    }

    public static LovvalgBestemmelse hentLovvalgBestemmelse(PeriodeOmLovvalg lovvalgsperiode) {
        final boolean tilleggsbestemmelseSkalMappes = lovvalgsperiode.getTilleggsbestemmelse() != null
            && TILLEGGSBESTEMMELSER_MAPPES_TIL_MEDL.contains(lovvalgsperiode.getTilleggsbestemmelse());

        LovvalgBestemmelse bestemmelse;
        if (tilleggsbestemmelseSkalMappes) {
            bestemmelse = lovvalgsperiode.getTilleggsbestemmelse();
        } else {
            bestemmelse = lovvalgsperiode.getBestemmelse();
        }
        return bestemmelse;
    }
}
