package no.nav.melosys.integrasjon.medl.behandle;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MedlPeriodeKonverterTest {

    @Test
    public void hentFellesKodeForGrunnlagMedltype() throws TekniskException {
        LovvalgsBestemmelser_883_2004 lovvalgsbestemmelse = LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2;
        GrunnlagMedl grunnlagMedltype = MedlPeriodeKonverter.tilGrunnlagMedltype(lovvalgsbestemmelse);
        assertThat(grunnlagMedltype).isEqualTo(GrunnlagMedl.FO_12_2);
    }

    @Test(expected = TekniskException.class)
    public void hentFellesKodeForGrunnlagMedltype_() throws TekniskException {
        LovvalgsBestemmelser_883_2004 lovvalgsbestemmelse = LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_1;
        MedlPeriodeKonverter.tilGrunnlagMedltype(lovvalgsbestemmelse);
    }

    @Test
    public void hentKodeverkForLovvalgsbestemmelse() throws TekniskException {
        GrunnlagMedl grunnlagKode = GrunnlagMedl.FO_12_2;
        LovvalgBestemmelse lovvalgsbestemmelse = MedlPeriodeKonverter.tilLovvalgBestemmelse(grunnlagKode);
        assertThat(lovvalgsbestemmelse).isEqualTo(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2);
    }

    @Test(expected = TekniskException.class)
    public void hentKodeverkForLovvalgsbestemmelseUkjentMedlKode() throws TekniskException {
        GrunnlagMedl grunnlagKode = GrunnlagMedl.MEDFT;
        MedlPeriodeKonverter.tilLovvalgBestemmelse(grunnlagKode);
    }

    @Test
    public void hentFellesKodeForDekningtype() throws TekniskException {
        Trygdedekninger trygdeDekning = Trygdedekninger.UTEN_DEKNING;
        DekningMedl dekningMedl = MedlPeriodeKonverter.tilMedlTrygdeDekning(trygdeDekning);
        assertThat(dekningMedl).isEqualTo(DekningMedl.UNNTATT);
    }

    @Test
    public void validere_OpprettPeriodeRequest_for_Medl() throws TekniskException {

        String aktørId = "12345678910";
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(Landkoder.BE);
        lovvalgsperiode.setFom(LocalDate.of(2015, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2015, 6, 30));
        OpprettPeriodeRequest opprettPeriodeRequest = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest(aktørId, lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL);

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode medlemsperiode = opprettPeriodeRequest.getPeriode();
        assertThat(opprettPeriodeRequest.getIdent().getValue()).isEqualTo(aktørId);

        assertThat(medlemsperiode.getLand().getValue()).isEqualTo("BEL");
        assertThat(medlemsperiode.getFraOgMed().toString()).isEqualTo("2015-01-01");
        assertThat(medlemsperiode.getTilOgMed().toString()).isEqualTo("2015-06-30");
        assertThat(medlemsperiode.getDatoRegistrert().toString()).isEqualTo(LocalDate.now().toString());
        assertThat(medlemsperiode.getStatus().getValue()).isEqualTo("GYLD");
        assertThat(medlemsperiode.getLovvalg().getValue()).isEqualTo("ENDL");
        assertThat(medlemsperiode.getTrygdedekning().getValue()).isEqualTo("Full");
        assertThat(medlemsperiode.getGrunnlagstype().getValue()).isEqualTo("FO_11_3_a");
    }


    @Test
    public void tilGrunnlagMedltype_både_FO_883_2004_ART16_1_og_FO_883_2004_ART16_2_gir_FO_16() throws TekniskException {
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1)).isEqualTo(GrunnlagMedl.FO_16);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_2)).isEqualTo(GrunnlagMedl.FO_16);
    }

    @Test
    public void tilLovvalgBestemmelse_FO_16_gir_FO_883_2004_ART16_1() throws TekniskException {
        assertThat(MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.FO_16)).isEqualTo(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1);
    }

    @Test
    public void opprettPeriodeRequest_medBådeTillegsbestemmelseFO_883_2004_ART11_4_1ogBestemmelse_velgerGrunnlagMedlFO_11_4_1() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2);
        OpprettPeriodeRequest opprettPeriodeRequest = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest("12345678910", lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL);

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode medlemsperiode = opprettPeriodeRequest.getPeriode();

        assertThat(medlemsperiode.getGrunnlagstype().getValue()).isEqualTo("FO_11_4_1");
    }

    @Test
    public void opprettPeriodeRequest_BestemmelseFO_883_2004_ART11_4_2_velgerGrunnlagMedlFO_11_4_2() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2);
        OpprettPeriodeRequest opprettPeriodeRequest = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest("12345678910", lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL);

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode medlemsperiode = opprettPeriodeRequest.getPeriode();

        assertThat(medlemsperiode.getGrunnlagstype().getValue()).isEqualTo("FO_11_4_2");
    }
}
