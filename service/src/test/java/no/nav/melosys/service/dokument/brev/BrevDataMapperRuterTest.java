package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.mapper.AvslagArbeidsgiverMapper;
import no.nav.melosys.service.dokument.brev.mapper.BrevDataMapper;
import no.nav.melosys.service.dokument.brev.mapper.HenleggelsesbrevMapper;
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelsesbrevMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrevDataMapperRuterTest {
    @Test
    void oppslagAvInnvilgelseYrkesaktivGirInnvelgelsesbrevMapper() {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV);
        assertThat(resultat).isInstanceOf(InnvilgelsesbrevMapper.class);
    }

    @Test
    void oppslagAvHenleggelsebrevGirHenleggelsesbrevMapper() {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.MELDING_HENLAGT_SAK);
        assertThat(resultat).isInstanceOf(HenleggelsesbrevMapper.class);
    }

    @Test
    void oppslagAvAvslagArbeidsgiverbrevGirAvslagArbeidsgiverMapper() {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER);
        assertThat(resultat).isInstanceOf(AvslagArbeidsgiverMapper.class);
    }
}
