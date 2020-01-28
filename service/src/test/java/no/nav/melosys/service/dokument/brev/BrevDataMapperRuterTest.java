package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.mapper.*;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BrevDataMapperRuterTest {
    @Test
    public void oppslagAvInnvilgelseYrkesaktivGirInnvelgelsesbrevMapper() throws Exception {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV);
        assertThat(resultat).isInstanceOf(InnvilgelsesbrevMapper.class);
    }

    @Test
    public void oppslagAvHenleggelsebrevGirHenleggelsesbrevMapper() throws Exception {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.MELDING_HENLAGT_SAK);
        assertThat(resultat).isInstanceOf(HenleggelsesbrevMapper.class);
    }

    @Test
    public void oppslagAvAvslagArbeidsgiverbrevGirAvslagArbeidsgiverMapper() throws Exception {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.AVSLAG_ARBEIDSGIVER);
        assertThat(resultat).isInstanceOf(AvslagArbeidsgiverMapper.class);
    }


    @Test
    public void oppslagAvAvslagManglendeOpplysningerGirAvslagManglendeOpplysningerMapper() throws Exception {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER);
        assertThat(resultat).isInstanceOf(AvslagManglendeOpplysningerMapper.class);
    }
}
