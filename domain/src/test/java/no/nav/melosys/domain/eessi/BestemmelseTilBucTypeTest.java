package no.nav.melosys.domain.eessi;

import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;


public class BestemmelseTilBucTypeTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void fraBestemmelse_art11_LABUC05() {
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B)).isEqualTo(BucType.LA_BUC_05);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C)).isEqualTo(BucType.LA_BUC_05);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E)).isEqualTo(BucType.LA_BUC_05);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2)).isEqualTo(BucType.LA_BUC_05);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART15)).isEqualTo(BucType.LA_BUC_05);
    }

    @Test
    public void fraBestemmelse_art12_LABUC04() {
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)).isEqualTo(BucType.LA_BUC_04);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2)).isEqualTo(BucType.LA_BUC_04);
    }

    @Test
    public void fraBestemmelse_art13_LABUC02() {
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8)).isEqualTo(BucType.LA_BUC_02);
        assertThat(BucType.fraBestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A)).isEqualTo(BucType.LA_BUC_02);
    }

    @Test
    public void fraBestemmelse_art16_LABUC04() {
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1)).isEqualTo(BucType.LA_BUC_01);
        assertThat(BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2)).isEqualTo(BucType.LA_BUC_01);
    }

    @Test
    public void fraBestemmelse_ikkeStøtteBestemmelse_forventException() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("kan ikke mappes til en BucType!");
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
    }

}