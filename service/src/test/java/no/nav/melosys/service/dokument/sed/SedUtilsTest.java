package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.eux.BucType;
import no.nav.melosys.eux.model.SedType;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4ClassRunner.class)
public class SedUtilsTest {

    @Test
    public void hentSedTypeFraLovvalgsBestemmelse_forventA009() {
        SedType sedType =SedUtils.hentSedTypeFraLovvalgsBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        assertThat(sedType, is(SedType.A009));
    }

    @Test
    public void hentBucFraLovvalgsBestemmelse_forventLABUC04() {
        BucType bucType = SedUtils.hentBucFraLovvalgsBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        assertThat(bucType, is(BucType.LA_BUC_04));
    }
}