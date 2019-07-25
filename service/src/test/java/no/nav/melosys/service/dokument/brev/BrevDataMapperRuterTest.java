package no.nav.melosys.service.dokument.brev;

import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.*;
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
    public void oppslagAvUkjentDoktypeKasterUnntak() {
        BrevDataMapperRuter.mappere.remove(Produserbaredokumenter.MELDING_HENLAGT_SAK); //Fjerner mapperen for å kunne teste på mapper ikke funnet

        Throwable unntak = catchThrowable(() -> BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.MELDING_HENLAGT_SAK));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasNoCause().hasMessageMatching("Produserbaredokumenter .* støttes ikke");

        BrevDataMapperRuter.mappere.put(Produserbaredokumenter.MELDING_HENLAGT_SAK, AnmodningUnntakMapper.class); //Legger til mapperen igjen slik at andre tester virker.
    }

    @Test
    public void oppslagAvIkkeInstansierbarMapperKasterUnntak() {
        BrevDataMapperRuter.mappere.put(Produserbaredokumenter.MELDING_HENLAGT_SAK, IkkeInstansierbarMapper.class);
        Throwable unntak = catchThrowable(() -> BrevDataMapperRuter.brevDataMapper(Produserbaredokumenter.MELDING_HENLAGT_SAK));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasCauseInstanceOf(NoSuchMethodException.class);
    }
}
