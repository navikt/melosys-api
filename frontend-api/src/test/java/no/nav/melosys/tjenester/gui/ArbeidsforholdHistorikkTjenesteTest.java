package no.nav.melosys.tjenester.gui;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.service.SaksopplysningerService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ArbeidsforholdHistorikkTjenesteTest {

    private ArbeidsforholdHistorikkTjeneste tjeneste;

    @Before
    public void setUp() throws JAXBException {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        SaksopplysningerService saksopplysningerService = new SaksopplysningerService(null, aareg, null, null, null, null);
        tjeneste = new ArbeidsforholdHistorikkTjeneste(saksopplysningerService);
    }

    @Test
    public void getHistoriskArbeidsforholdDokument() throws Exception {
        Response response = tjeneste.hentArbeidsforholdHistorikk(12608035L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(ArbeidsforholdDokument.class);

        ArbeidsforholdDokument dokument = (ArbeidsforholdDokument) response.getEntity();
        assertThat(dokument.getArbeidsforhold().size()).isGreaterThan(0);
        assertThat(dokument.getArbeidsforhold().get(0).getArbeidsavtaler().size()).isGreaterThan(1);
        response.close();
    }

}
