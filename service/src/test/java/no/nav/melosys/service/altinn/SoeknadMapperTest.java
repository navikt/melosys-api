package no.nav.melosys.service.altinn;

import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.UtenlandskIdent;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.soknad_altinn.ObjectFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoeknadMapperTest {
    @Test
    void lagSoeknad() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        URL url = getClass().getClassLoader().getResource("altinn/NAV_MedlemskapArbeidEOS.xml");
        MedlemskapArbeidEOSM medlemskapArbeidEOSM =
            ((JAXBElement<MedlemskapArbeidEOSM>) jaxbContext.createUnmarshaller().unmarshal(url)).getValue();

        Soeknad soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM);

        assertThat(soeknad.soeknadsland.landkoder).contains("FI");
        assertThat(soeknad.periode.getFom()).isEqualTo("2019-08-05");
        assertThat(soeknad.periode.getTom()).isEqualTo("2019-08-06");
        assertThat(soeknad.personOpplysninger.utenlandskIdent)
            .contains(new UtenlandskIdent("utenlandskIDnummer", "FI"));
    }
}