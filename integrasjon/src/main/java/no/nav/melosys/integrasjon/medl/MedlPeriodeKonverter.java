package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import javax.xml.datatype.DatatypeConfigurationException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.kodeverk.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;

public class MedlPeriodeKonverter {

    private static final BiMap<LovvalgBestemmelse, GrunnlagMedl> lovvalgsbestemmelseTilGrunnlagMedlTabell;

    static {
        BiMap<LovvalgBestemmelse, GrunnlagMedl> tbl = HashBiMap.create();
        // Article 11
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A, GrunnlagMedl.FO_11_3_A);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3B, GrunnlagMedl.FO_11_3_B);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3C, GrunnlagMedl.FO_11_3_C);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3E, GrunnlagMedl.FO_11_3_E);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2, GrunnlagMedl.FO_11_4);
        // Article 12
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1, GrunnlagMedl.FO_12_1);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2, GrunnlagMedl.FO_12_2);
        // Article 13
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, GrunnlagMedl.FO_13_1_A);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1B1, GrunnlagMedl.FO_13_1_B);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1_B2, GrunnlagMedl.FO_13_B_II);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1_B3, GrunnlagMedl.FO_13_B_III);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1_B4, GrunnlagMedl.FO_13_B_IV);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_2A, GrunnlagMedl.FO_13_2_A);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_2B, GrunnlagMedl.FO_13_2_B);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_3, GrunnlagMedl.FO_13_3);
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_4, GrunnlagMedl.FO_13_4);
        // Article 16
        tbl.put(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1, GrunnlagMedl.FO_16);
        lovvalgsbestemmelseTilGrunnlagMedlTabell = tbl;
    }

    public static DekningMedl tilMedlTrygdeDekning(Trygdedekninger dekning) throws TekniskException {
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

    public static GrunnlagMedl tilGrunnlagMedltype(LovvalgBestemmelse bestemmelse) throws TekniskException {
        //ART16_2 er pensjon og brukes foreløpig ikke i Melosys
        //ART16_1 og ART16_2 mappes til samme GrunnlMedl
        if (bestemmelse.equals(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_2)) {
            return GrunnlagMedl.FO_16;
        }
        GrunnlagMedl grunnlagMedltype = lovvalgsbestemmelseTilGrunnlagMedlTabell.get(bestemmelse);
        if (grunnlagMedltype == null) {
            throw new TekniskException("Lovvalgsbestemmelse støttes ikke i MEDL. Kode: " + bestemmelse.getKode() + " Beskrivelse: " + bestemmelse.getBeskrivelse());
        }
        return grunnlagMedltype;
    }

    public static LovvalgBestemmelse tilLovvalgBestemmelse(GrunnlagMedl grunnlagKode) throws TekniskException {
        LovvalgBestemmelse lovvalgBestemmelse = lovvalgsbestemmelseTilGrunnlagMedlTabell.inverse().get(grunnlagKode);
        if (lovvalgBestemmelse == null) {
            throw new TekniskException("GrunnlagMedlKode er ukjent. Kode: " + grunnlagKode.getKode() );
        }
        return lovvalgBestemmelse;
    }

    public static OpprettPeriodeRequest konverterTilOpprettPeriodRequest(String fnr,
                                                                         Lovvalgsperiode lovvalgsperiode,
                                                                         PeriodestatusMedl periodestatusMedl,
                                                                         LovvalgMedl lovvalgMedl) throws TekniskException {

        OpprettPeriodeRequest request = new OpprettPeriodeRequest();

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Foedselsnummer ident = new no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Foedselsnummer();
        ident.setValue(fnr);

        Medlemsperiode periode = opprettPeriode(lovvalgsperiode, periodestatusMedl, lovvalgMedl);

        request.setIdent(ident);
        request.setPeriode(periode);

        return request;
    }

    public static OppdaterPeriodeRequest konverterTilOppdaterPeriodeRequest(Lovvalgsperiode lovvalgsperiode,
                                                                            PeriodestatusMedl periodestatusMedl,
                                                                            LovvalgMedl lovvalgMedl, int versjon) throws TekniskException {
        OppdaterPeriodeRequest request = new OppdaterPeriodeRequest();

        request.setPeriodeId(lovvalgsperiode.getMedlPeriodeID());
        request.setVersjon(versjon);

        Medlemsperiode periode = opprettPeriode(lovvalgsperiode, periodestatusMedl, lovvalgMedl);

        request.setPeriode(periode);

        return request;
    }

    private static Medlemsperiode opprettPeriode(Lovvalgsperiode lovvalgsperiode, PeriodestatusMedl periodestatusMedl, LovvalgMedl lovvalgMedl) throws TekniskException {
        Medlemsperiode periode = new Medlemsperiode();
        try {
            periode.setFraOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(lovvalgsperiode.getFom()));
            periode.setTilOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(lovvalgsperiode.getTom()));
            periode.setDatoRegistrert(KonverteringsUtils.localDateToXMLGregorianCalendar(LocalDate.now()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        if (periodestatusMedl != null) {
            periode.setStatus(new Statuskode().withValue(periodestatusMedl.getKode()));
        }

        if (lovvalgMedl != null) {
            periode.setLovvalg(new Lovvalg().withValue(lovvalgMedl.getKode()));
        }

        if (lovvalgsperiode.getDekning() != null) {
            DekningMedl dekningMedl = tilMedlTrygdeDekning(lovvalgsperiode.getDekning());
            periode.setTrygdedekning(new Trygdedekning().withValue(dekningMedl.getKode()));
        }

        if (lovvalgsperiode.getLovvalgsland() != null) {
            String lovvalgLand = LandkoderUtils.tilIso3(lovvalgsperiode.getLovvalgsland().getKode());
            periode.setLand(new Landkode().withValue(lovvalgLand));
        }

        if (lovvalgsperiode.getBestemmelse() != null) {
            GrunnlagMedl grunnlagMedl = tilGrunnlagMedltype(lovvalgsperiode.getBestemmelse());
            periode.setGrunnlagstype(new Grunnlagstype().withValue(grunnlagMedl.getKode()));
        }
        return periode;
    }
}
