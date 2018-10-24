package no.nav.melosys.domain;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.GrunnlagMedl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

public class LovvalgsperiodeTest {

    @Test(expected = RuntimeException.class)
    public void hentFellesKodeForGrunnlagMedltypeTest() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_3A);
        GrunnlagMedl grunnlagMedltype = lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
        assertThat(grunnlagMedltype).isEqualTo(GrunnlagMedl.FO_11_3_a);

        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_1);
        given(lovvalgsperiode.hentFellesKodeForGrunnlagMedltype()).willThrow(new RuntimeException("Feil lovvlagsbestemmelse koden :ART11_1:Kun omfattet i en medlemsstat (art 11.1)"));
    }
}
