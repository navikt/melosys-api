package no.nav.melosys.integrasjon.medl.behandle;

import java.time.LocalDate;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MedlPeriodeKonverterTest {

    @Test
    public void tilGrunnlagMedltype() throws TekniskException {
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))
            .isEqualTo(GrunnlagMedl.FO_12_2);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1))
            .isEqualTo(GrunnlagMedl.FO_16);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2))
            .isEqualTo(GrunnlagMedl.FO_16);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1))
            .withMessageContaining("støttes ikke i MEDL");
    }

    @Test
    public void tilLovvalgBestemmelse() throws TekniskException {
        assertThat(MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.FO_12_2))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        assertThat(MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.FO_16))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.MEDFT))
            .withMessageContaining("GrunnlagMedlKode er ukjent");
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
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(Landkoder.BE);
        lovvalgsperiode.setFom(LocalDate.of(2015, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2015, 6, 30));
        OpprettPeriodeRequest opprettPeriodeRequest = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest(aktørId, lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, KildedokumenttypeMedl.HENV_SOKNAD);

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
        assertThat(medlemsperiode.getKildedokumenttype().getValue()).isEqualTo("Henv_Soknad");
    }


    @Test
    public void opprettPeriodeRequest_medBådeTillegsbestemmelseFO_883_2004_ART11_4_1ogBestemmelse_velgerGrunnlagMedlFO_11_4_1() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        OpprettPeriodeRequest opprettPeriodeRequest = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest("12345678910", lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, KildedokumenttypeMedl.HENV_SOKNAD);

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode medlemsperiode = opprettPeriodeRequest.getPeriode();

        assertThat(medlemsperiode.getGrunnlagstype().getValue()).isEqualTo("FO_11_4_1");
    }

    @Test
    public void opprettPeriodeRequest_BestemmelseFO_883_2004_ART11_4_2_velgerGrunnlagMedlFO_11_4_2() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        OpprettPeriodeRequest opprettPeriodeRequest = MedlPeriodeKonverter.konverterTilOpprettPeriodRequest("12345678910", lovvalgsperiode, PeriodestatusMedl.GYLD, LovvalgMedl.ENDL, KildedokumenttypeMedl.HENV_SOKNAD);

        no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.informasjon.Medlemsperiode medlemsperiode = opprettPeriodeRequest.getPeriode();

        assertThat(medlemsperiode.getGrunnlagstype().getValue()).isEqualTo("FO_11_4_2");
    }
}
