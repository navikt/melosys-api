package no.nav.melosys.domain;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.GrunnlagMedl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LovvalgsperiodeTest {

    @Test
    public void hentFellesKodeForGrunnlagMedltypeTest() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_3A);
        GrunnlagMedl grunnlagMedltype = lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
        assertThat(GrunnlagMedl.FO_11_3_a == grunnlagMedltype);

        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_1);
        assertThat(null == grunnlagMedltype);


    }
}
