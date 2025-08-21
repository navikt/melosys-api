package no.nav.melosys.domain.eessi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import org.junit.jupiter.api.Test

internal class BestemmelseTilBucTypeTest {

    @Test
    fun fraBestemmelse_art11_LABUC05() {
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B) shouldBe BucType.LA_BUC_05
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C) shouldBe BucType.LA_BUC_05
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E) shouldBe BucType.LA_BUC_05
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2) shouldBe BucType.LA_BUC_05
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART15) shouldBe BucType.LA_BUC_05
    }

    @Test
    fun fraBestemmelse_art12_LABUC04() {
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1) shouldBe BucType.LA_BUC_04
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2) shouldBe BucType.LA_BUC_04
    }

    @Test
    fun fraBestemmelse_art13_LABUC02() {
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8) shouldBe BucType.LA_BUC_02
        BucType.fraBestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A) shouldBe BucType.LA_BUC_02
    }

    @Test
    fun fraBestemmelse_art16_LABUC04() {
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1) shouldBe BucType.LA_BUC_01
        BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2) shouldBe BucType.LA_BUC_01
    }

    @Test
    fun fraBestemmelse_ikkeStøtteBestemmelse_forventException() {
        val exception = shouldThrow<IllegalArgumentException> {
            BucType.fraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET)
        }
        exception.message shouldContain "kan ikke mappes til en BucType"
    }
}