package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import javax.xml.datatype.DatatypeConfigurationException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.kodeverk.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.AvvisPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;

public final class MedlPeriodeKonverter {

    private MedlPeriodeKonverter() {
        throw new IllegalStateException("Utility");
    }

    private static final BiMap<LovvalgBestemmelse, GrunnlagMedl> lovvalgsbestemmelseTilGrunnlagMedlTabell;

    static {
        BiMap<LovvalgBestemmelse, GrunnlagMedl> tbl = HashBiMap.create();
        // Article 11
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, GrunnlagMedl.FO_11_3_A);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B, GrunnlagMedl.FO_11_3_B);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C, GrunnlagMedl.FO_11_3_C);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E, GrunnlagMedl.FO_11_3_E);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2, GrunnlagMedl.FO_11_4_2);
        tbl.put(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1, GrunnlagMedl.FO_11_4_1);
        // Article 12
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, GrunnlagMedl.FO_12_1);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2, GrunnlagMedl.FO_12_2);
        // Article 13
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, GrunnlagMedl.FO_13_1_A);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, GrunnlagMedl.FO_13_1_B);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2, GrunnlagMedl.FO_13_B_II);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3, GrunnlagMedl.FO_13_B_III);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4, GrunnlagMedl.FO_13_B_IV);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, GrunnlagMedl.FO_13_2_A);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B, GrunnlagMedl.FO_13_2_B);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, GrunnlagMedl.FO_13_3);
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4, GrunnlagMedl.FO_13_4);
        // Article 16
        tbl.put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, GrunnlagMedl.FO_16);
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
        if (bestemmelse.equals(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2)) {
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
                                                                         Medlemskapsperiode periodeMedBestemmelse,
                                                                         PeriodestatusMedl periodestatusMedl,
                                                                         LovvalgMedl lovvalgMedl,
                                                                         KildedokumenttypeMedl kildedokumenttypeMedl) throws TekniskException {

        OpprettPeriodeRequest request = new OpprettPeriodeRequest();

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Foedselsnummer ident = new no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Foedselsnummer();
        ident.setValue(fnr);

        Medlemsperiode periode = opprettPeriode(periodeMedBestemmelse, periodestatusMedl, lovvalgMedl, kildedokumenttypeMedl);

        request.setIdent(ident);
        request.setPeriode(periode);

        return request;
    }

    static OppdaterPeriodeRequest konverterTilOppdaterPeriodeRequest(Lovvalgsperiode lovvalgsperiode,
                                                                     PeriodestatusMedl periodestatusMedl,
                                                                     LovvalgMedl lovvalgMedl,
                                                                     KildedokumenttypeMedl kildedokumenttypeMedl, int versjon) throws TekniskException {
        OppdaterPeriodeRequest request = new OppdaterPeriodeRequest();

        request.setPeriodeId(lovvalgsperiode.getMedlPeriodeID());
        request.setVersjon(versjon);

        Medlemsperiode periode = opprettPeriode(lovvalgsperiode, periodestatusMedl, lovvalgMedl, kildedokumenttypeMedl);

        request.setPeriode(periode);

        return request;
    }

    private static Medlemsperiode opprettPeriode(Medlemskapsperiode periodeMedBestemmelse, PeriodestatusMedl periodestatusMedl, LovvalgMedl lovvalgMedl, KildedokumenttypeMedl kildedokumenttypeMedl) throws TekniskException {
        Medlemsperiode periode = new Medlemsperiode();
        try {
            periode.setFraOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(periodeMedBestemmelse.getFom()));
            periode.setTilOgMed(KonverteringsUtils.localDateToXMLGregorianCalendar(periodeMedBestemmelse.getTom()));
            periode.setDatoRegistrert(KonverteringsUtils.localDateToXMLGregorianCalendar(LocalDate.now()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException(e);
        }
        if (periodestatusMedl != null) {
            periode.setStatus(new Statuskode().withValue(periodestatusMedl.getKode()));
        }

        if (lovvalgMedl != null) {
            periode.setLovvalg(new Lovvalg().withValue(lovvalgMedl.getKode()));
        }

        if (periodeMedBestemmelse.getDekning() != null) {
            DekningMedl dekningMedl = tilMedlTrygdeDekning(periodeMedBestemmelse.getDekning());
            periode.setTrygdedekning(new Trygdedekning().withValue(dekningMedl.getKode()));
        }

        if (periodeMedBestemmelse.getLovvalgsland() != null) {
            String lovvalgLand = LandkoderUtils.tilIso3(periodeMedBestemmelse.getLovvalgsland().getKode());
            periode.setLand(new Landkode().withValue(lovvalgLand));
        }

        LovvalgBestemmelse bestemmelse = hentLovvalgBestemmelse(periodeMedBestemmelse);
        if (bestemmelse != null) {
            GrunnlagMedl grunnlagMedl = tilGrunnlagMedltype(bestemmelse);
            periode.setGrunnlagstype(new Grunnlagstype().withValue(grunnlagMedl.getKode()));
        }

        if (kildedokumenttypeMedl != null) {
            periode.setKildedokumenttype(new Kildedokumenttype().withValue(kildedokumenttypeMedl.getKode()));
        }
        return periode;
    }

    private static LovvalgBestemmelse hentLovvalgBestemmelse(Medlemskapsperiode lovvalgsperiode) {
        final boolean harTilleggsbestemmelseART11_4_1 = lovvalgsperiode.getTilleggsbestemmelse() != null && lovvalgsperiode.getTilleggsbestemmelse().equals(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        LovvalgBestemmelse bestemmelse;
        if (harTilleggsbestemmelseART11_4_1) {
            bestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1;
        } else {
            bestemmelse = lovvalgsperiode.getBestemmelse();
        }
        return bestemmelse;
    }

    static AvvisPeriodeRequest konverterTilAvvisPeriodeRequest(Long medlId, StatusaarsakMedl årsak) {
        AvvisPeriodeRequest request = new AvvisPeriodeRequest();
        request.setPeriodeId(medlId);
        request.setAarsak(new Statusaarsak().withValue(årsak.getKode()));
        return request;
    }
}
