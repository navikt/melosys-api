package no.nav.melosys.integrasjon.medl.behandle;

import java.time.LocalDate;

import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.TrygdeDekning;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MedlPeriodeKonverterTest {

    @Test
    public void hentFellesKodeForGrunnlagMedltype() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3A);
        GrunnlagMedl grunnlagMedltype = MedlPeriodeKonverter.hentFellesKodeForGrunnlagMedltype(lovvalgsperiode.getBestemmelse());
        assertThat(grunnlagMedltype).isEqualTo(GrunnlagMedl.FO_11_3_A);
    }

    @Test(expected = TekniskException.class)
    public void hentFellesKodeForGrunnlagMedltype_() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_1);
        MedlPeriodeKonverter.hentFellesKodeForGrunnlagMedltype(lovvalgsperiode.getBestemmelse());
    }

    @Test
    public void hentFellesKodeForDekningtype() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setDekning(TrygdeDekning.UTEN_DEKNING);
        DekningMedl dekningMedl = MedlPeriodeKonverter.hentFellesKodeForTrygdDekningtype(lovvalgsperiode.getDekning());
        assertThat(dekningMedl).isEqualTo(DekningMedl.UNNTATT);
    }

    @Test
    public void validere_OpprettPeriodeRequest_for_Medl() throws TekniskException {

        String aktørId = "12345678910";
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setDekning(TrygdeDekning.FULL_DEKNING_EOSFO);
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
        assertThat(medlemsperiode.getStatus().getValue()).isEqualTo("Gyldig");
        assertThat(medlemsperiode.getLovvalg().getValue()).isEqualTo("Endelig");
        assertThat(medlemsperiode.getTrygdedekning().getValue()).isEqualTo("Full");
        assertThat(medlemsperiode.getGrunnlagstype().getValue()).isEqualTo("FO_11_3_a");
    }

}
