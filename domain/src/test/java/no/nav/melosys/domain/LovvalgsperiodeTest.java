package no.nav.melosys.domain;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.GrunnlagMedl;
import no.nav.melosys.exception.TekniskException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LovvalgsperiodeTest {

    @Test
    public void hentFellesKodeForGrunnlagMedltype() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_3A);
        GrunnlagMedl grunnlagMedltype = lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
        assertThat(grunnlagMedltype).isEqualTo(GrunnlagMedl.FO_11_3_A);
    }

    @Test(expected = TekniskException.class)
    public void hentFellesKodeForGrunnlagMedltype_() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_1);
        lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
    }
}
