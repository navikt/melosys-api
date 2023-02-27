package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.DekningMedl;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MedlPeriodeKonverterTest {

    @Test
    void tilGrunnlagMedltype() {
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))
            .isEqualTo(GrunnlagMedl.FO_12_2);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1))
            .isEqualTo(GrunnlagMedl.FO_16);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2))
            .isEqualTo(GrunnlagMedl.FO_16);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3))
            .isEqualTo(GrunnlagMedl.Storbrit_NIrland_7_3);
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11))
            .isEqualTo(GrunnlagMedl.FO_987_2009_14_11);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> MedlPeriodeKonverter.tilGrunnlagMedltype(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1))
            .withMessageContaining("støttes ikke i MEDL");
    }

    @Test
    void tilGrunnlagMedltype_Ftrl() {
        assertThat(MedlPeriodeKonverter.tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8)).isEqualTo(GrunnlagMedl.FTL_2_8);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> MedlPeriodeKonverter.tilGrunnlagMedltype(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1))
            .withMessageContaining("Folketrygdloven bestemmelse støttes ikke. Kode: FTRL_KAP2_2_1");
    }

    @Test
    void tilLovvalgBestemmelse() {
        assertThat(MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.FO_12_2))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        assertThat(MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.FO_16))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.MEDFT))
            .withMessageContaining("GrunnlagMedlKode er ukjent");
    }

    @Test
    void hentFellesKodeForDekningtype() {
        Trygdedekninger trygdeDekning = Trygdedekninger.UTEN_DEKNING;
        DekningMedl dekningMedl = MedlPeriodeKonverter.tilMedlTrygdeDekningEos(trygdeDekning);
        assertThat(dekningMedl).isEqualTo(DekningMedl.UNNTATT);
    }

    @Test
    void hentLovvalgbestemmelse() {
        assertThat(MedlPeriodeKonverter.hentLovvalgBestemmelse(lovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null)))
            .isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        assertThat(MedlPeriodeKonverter.hentLovvalgBestemmelse(lovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5)))
            .isEqualTo(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);
        assertThat(MedlPeriodeKonverter.hentLovvalgBestemmelse(lovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1)))
            .isEqualTo(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
    }

    private Lovvalgsperiode lovvalgsperiode(LovvalgBestemmelse lovvalgBestemmelse, LovvalgBestemmelse tilleggBestemmelse) {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(lovvalgBestemmelse);
        lovvalgsperiode.setTilleggsbestemmelse(tilleggBestemmelse);
        return lovvalgsperiode;
    }
}
