package no.nav.melosys.service.dokument.brev;

import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProduserbartDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.BrevDataMapper;
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelsesbrevMapper;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class BrevDataMapperRuterTest {

    public class IkkeInstansierbarMapper implements BrevDataMapper {

        private IkkeInstansierbarMapper() {
        }

        @Override
        public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
            throw new UnsupportedOperationException();
        }

    }

    @Test
    public void oppslagAvInnvilgelseYrkesaktivGirInnvelgelsesbrevMapper() throws Exception {
        BrevDataMapper resultat = BrevDataMapperRuter.brevDataMapper(ProduserbartDokument.INNVILGELSE_YRKESAKTIV);
        assertThat(resultat).isInstanceOf(InnvilgelsesbrevMapper.class);
    }

    @Test
    public void oppslagAvUkjentDoktypeKasterUnntak() {
        Throwable unntak = catchThrowable(() -> BrevDataMapperRuter.brevDataMapper(ProduserbartDokument.MELDING_HENLAGT_SAK));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasNoCause().hasMessageMatching("ProduserbartDokument .* støttes ikke");
    }

    @Test
    public void oppslagAvIkkeInstansierbarMapperKasterUnntak() {
        BrevDataMapperRuter.mappere.put(ProduserbartDokument.MELDING_HENLAGT_SAK, IkkeInstansierbarMapper.class);
        Throwable unntak = catchThrowable(() -> BrevDataMapperRuter.brevDataMapper(ProduserbartDokument.MELDING_HENLAGT_SAK));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasCauseInstanceOf(InstantiationException.class);
    }
}