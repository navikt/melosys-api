package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import javax.xml.datatype.DatatypeConfigurationException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.TrygdeDekning;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.kodeverk.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;

public class MedlPeriodeKonverter {

    private static final BiMap<LovvalgBestemmelse, GrunnlagMedl> lovvalgsbestemmelseTilGrunnlagMedlTabell;

    static {
        BiMap<LovvalgBestemmelse, GrunnlagMedl> tbl = HashBiMap.create();
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

    public static DekningMedl tilMedlTrygdeDekning(TrygdeDekning dekning) throws TekniskException {
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

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode periode = new no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode();
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

        request.setIdent(ident);
        request.setPeriode(periode);

        return request;
    }
}
