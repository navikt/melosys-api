package no.nav.melosys.integrasjon.medl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.eessi.sed.Bestemmelse;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*;
import no.nav.melosys.exception.TekniskException;

import static java.util.Optional.ofNullable;

public final class MedlPeriodeKonverter {

    private MedlPeriodeKonverter() {
        throw new IllegalStateException("Utility");
    }

    private static final BiMap<LovvalgBestemmelse, GrunnlagMedl> lovvalgsbestemmelseTilGrunnlagMedlTabell;
    private static final Map<Folketrygdloven_kap2_bestemmelser, GrunnlagMedl> ftrlKap2BestemmelserGrunnLagMedlTabell;

    private static final Collection<LovvalgBestemmelse> TILLEGGSBESTEMMELSER_MAPPES_TIL_MEDL = Set.of(
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8
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
        tbl.put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A, GrunnlagMedl.FO_11_5);
        tbl.put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8, GrunnlagMedl.FO_11_5);
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

        tbl.put(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1, GrunnlagMedl.Storbrit_NIrland_6_1);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_5, GrunnlagMedl.Storbrit_NIrland_6_5);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART7_3, GrunnlagMedl.Storbrit_NIrland_7_3);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2, GrunnlagMedl.Storbrit_NIrland_8_2);

        tbl.put(Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_2, GrunnlagMedl.USA_ART5_2);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_4, GrunnlagMedl.USA_ART5_4);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_5, GrunnlagMedl.USA_ART5_5);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_6, GrunnlagMedl.USA_ART5_6);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_9, GrunnlagMedl.USA_ART5_9);

        tbl.put(Lovvalgbestemmelser_trygdeavtale_ca.CAN_ART6_2, GrunnlagMedl.CAN_ART6_2);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_ca.CAN_ART7, GrunnlagMedl.CAN_ART7);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_ca.CAN_ART9, GrunnlagMedl.CAN_ART9);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_ca.CAN_ART10, GrunnlagMedl.CAN_ART10);
        tbl.put(Lovvalgbestemmelser_trygdeavtale_ca.CAN_ART11, GrunnlagMedl.CAN_ART11);

        //TODO er dette korrekt plass?

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


    public static DekningMedl tilMedlTrygdeDekningEos(Trygdedekninger dekning) {
        return switch (dekning) {
            case FULL_DEKNING_EOSFO, FULL_DEKNING_FTRL -> DekningMedl.FULL;
            case UTEN_DEKNING -> DekningMedl.UNNTATT;
            default -> throw new TekniskException("Dekningstype støttes ikke for EØS:" + dekning.getKode());
        };
    }

    public static DekningMedl tilMedlTrygdeDekningFtrl(Trygdedekninger dekning, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        return switch (bestemmelse) {
            case FTRL_KAP2_2_8, FTRL_KAP2_2_8_FØRSTE_LEDD_A, FTRL_KAP2_2_8_FØRSTE_LEDD_B,
                FTRL_KAP2_2_8_FØRSTE_LEDD_C, FTRL_KAP2_2_8_FØRSTE_LEDD_D, FTRL_KAP2_2_8_ANDRE_LEDD,
                FTRL_KAP2_2_8_TREDJE_LEDD, FTRL_KAP2_2_8_FJERDE_LEDD, FTRL_KAP2_2_8_FEMTE_LEDD -> mapForFtrlKap2_8(dekning);
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
