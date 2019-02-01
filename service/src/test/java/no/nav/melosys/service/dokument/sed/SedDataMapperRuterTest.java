package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.mapper.A009Mapper;
import no.nav.melosys.service.dokument.sed.mapper.AbstraktSedMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class SedDataMapperRuterTest {

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
    public void oppslagavSedGirKorrektMapper() throws Exception {
        AbstraktSedMapper sedMapper = SedDataMapperRuter.sedMapper(SedType.A009);
        assertThat(sedMapper).isInstanceOf(A009Mapper.class);
    }

    @Test
    public void oppslagAvIkkeInstansierbarSedMapperKasterUnntak() {
        SedDataMapperRuter.sedMappere.put(SedType.A012, IkkeInstansierbarSedMapper.class);
        Throwable unntak = catchThrowable(() -> SedDataMapperRuter.sedMapper(SedType.A012));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasCauseInstanceOf(InstantiationException.class);
    }
}
