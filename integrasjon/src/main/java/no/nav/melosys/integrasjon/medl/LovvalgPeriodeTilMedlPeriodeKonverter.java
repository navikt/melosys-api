package no.nav.melosys.integrasjon.medl;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.TrygdeDekning;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.TrygdeDekning.FULL_DEKNING_EOSFO;
import static no.nav.melosys.domain.TrygdeDekning.UTEN_DEKNING;

public class LovvalgPeriodeTilMedlPeriodeKonverter {
    private static final Map<LovvalgBestemmelse, GrunnlagMedl> lovvalgsbestemmelseTilGrunnlagMedlTabell;

    static {
        Map<LovvalgBestemmelse, GrunnlagMedl> tbl = new HashMap<>();
        // Article 11
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3A, GrunnlagMedl.FO_11_3_A);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3B, GrunnlagMedl.FO_11_3_B);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3C, GrunnlagMedl.FO_11_3_C);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3E, GrunnlagMedl.FO_11_3_E);
        // Article 12
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1, GrunnlagMedl.FO_12_1);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2, GrunnlagMedl.FO_12_2);
        // Article 13
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1A, GrunnlagMedl.FO_13_1_A);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B1, GrunnlagMedl.FO_13_1_B);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B2, GrunnlagMedl.FO_13_B_II);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B3, GrunnlagMedl.FO_13_B_III);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_1B4, GrunnlagMedl.FO_13_B_IV);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_2A, GrunnlagMedl.FO_13_2_A);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_2B, GrunnlagMedl.FO_13_2_B);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_3, GrunnlagMedl.FO_13_3);
        tbl.put(LovvalgBestemmelse_883_2004.FO_883_2004_ART13_4, GrunnlagMedl.FO_13_4);
        lovvalgsbestemmelseTilGrunnlagMedlTabell = tbl;
    }

    public DekningMedl hentFellesKodeForTrygdDekningtype(TrygdeDekning dekning) throws TekniskException {
        DekningMedl dekningMedltype;
        switch (dekning) {
            case FULL_DEKNING_EOSFO:
                dekningMedltype = DekningMedl.FULL;
                break;
            case UTEN_DEKNING:
                dekningMedltype = DekningMedl.UNNTATT;
                break;
            default:
                throw new TekniskException("Dekningstype støttes ikke:" + dekning.getKode());
        }
        return dekningMedltype;
    }

    public GrunnlagMedl hentFellesKodeForGrunnlagMedltype(LovvalgBestemmelse bestemmelse) throws TekniskException {
        GrunnlagMedl grunnlagMedltype = lovvalgsbestemmelseTilGrunnlagMedlTabell.get(bestemmelse);
        if (grunnlagMedltype == null) {
            throw new TekniskException("Lovvalgsbestemmelse støttes ikke i MEDL. Kode: " + bestemmelse.getKode() + " Beskrivelse: " + bestemmelse.getBeskrivelse());
        }
        return grunnlagMedltype;
    }

}
