package no.nav.melosys.service.dokument;

import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProduserbartDokument;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.mapper.BrevDataMapper;
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelsesbrevMapper;
import no.nav.melosys.service.dokument.sed.AbstraktSedData;
import no.nav.melosys.service.dokument.sed.mapper.A009Mapper;
import no.nav.melosys.service.dokument.sed.mapper.AbstraktSedMapper;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class DokumentDataMapperRuterTest {

    public class IkkeInstansierbarMapper implements BrevDataMapper {

        private IkkeInstansierbarMapper() {
        }

        @Override
        public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
            throw new UnsupportedOperationException();
        }

    }

    public class IkkeInstansierbarSedMapper extends AbstraktSedMapper {

        private IkkeInstansierbarSedMapper() {
        }

        @Override
        protected Medlemskap hentMedlemskap(AbstraktSedData sedData) {
            return null;
        }

        @Override
        protected SedType getSedType() {
            return null;
        }
    }

    @Test
    public void oppslagAvInnvilgelseYrkesaktivGirInnvelgelsesbrevMapper() throws Exception {
        BrevDataMapper resultat = DokumentDataMapperRuter.brevDataMapper(ProduserbartDokument.INNVILGELSE_YRKESAKTIV);
        assertThat(resultat).isInstanceOf(InnvilgelsesbrevMapper.class);
    }

    @Test
    public void oppslagAvUkjentDoktypeKasterUnntak() {
        Throwable unntak = catchThrowable(() -> DokumentDataMapperRuter.brevDataMapper(ProduserbartDokument.MELDING_HENLAGT_SAK));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasNoCause().hasMessageMatching("ProduserbartDokument .* støttes ikke");
    }

    @Test
    public void oppslagAvIkkeInstansierbarMapperKasterUnntak() {
        DokumentDataMapperRuter.mappere.put(ProduserbartDokument.MELDING_HENLAGT_SAK, IkkeInstansierbarMapper.class);
        Throwable unntak = catchThrowable(() -> DokumentDataMapperRuter.brevDataMapper(ProduserbartDokument.MELDING_HENLAGT_SAK));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasCauseInstanceOf(InstantiationException.class);
    }

    @Test
    public void oppslagavSedGirKorrektMapper() throws Exception {
        AbstraktSedMapper sedMapper = DokumentDataMapperRuter.sedMapper(SedType.A009);
        assertThat(sedMapper).isInstanceOf(A009Mapper.class);
    }

    @Test
    public void oppslagAvIkkeInstansierbarSedMapperKasterUnntak() {
        DokumentDataMapperRuter.sedMappere.put(SedType.A012, IkkeInstansierbarSedMapper.class);
        Throwable unntak = catchThrowable(() -> DokumentDataMapperRuter.sedMapper(SedType.A012));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasCauseInstanceOf(InstantiationException.class);
    }
}
