package no.nav.melosys.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.GrunnlagMedl;
import no.nav.melosys.exception.TekniskException;

public class LovvalgsperiodeTest {

    @Test
    public void hentFellesKodeForGrunnlagMedltype() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3A);
        GrunnlagMedl grunnlagMedltype = lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
        assertThat(grunnlagMedltype).isEqualTo(GrunnlagMedl.FO_11_3_A);
    }

    @Test(expected = TekniskException.class)
    public void hentFellesKodeForGrunnlagMedltype_() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_1);
        lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
    }
}
