package no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LovvalgsbestemmelseTest {
    @ParameterizedTest
    @MethodSource("gyldigeBestemmelser")
    void av_gyldigBestemmelse_forventNotNull(LovvalgBestemmelse bestemmelse) {
        assertThat(Lovvalgsbestemmelse.av(bestemmelse)).isNotNull();
    }

    @Test
    void av_ugyldigBestemmelse_forventException() {
        LovvalgBestemmelse ugyldigBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4;
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> Lovvalgsbestemmelse.av(ugyldigBestemmelse))
            .withMessageContaining("støttes ikke for melding om utstedt A1");
    }

    private static final Set<LovvalgBestemmelse> GYLDIGE_BESTEMMELSER = Set.of(
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
    );

    private static Set<LovvalgBestemmelse> gyldigeBestemmelser() {
        return GYLDIGE_BESTEMMELSER;
    }
}
