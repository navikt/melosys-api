package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektConsumer;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektMock;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InntektServiceTest {
    private InntektService inntektService;

    @Before
    public void setUp() {
        InntektConsumer inntektMock  = new InntektMock();
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), new XsltTemplatesFactory());
        inntektService = new InntektService(inntektMock, dokumentFactory);
    }
    
    @Test
    public void hentInntektListe() throws Exception {
        Saksopplysning saksopplysning = inntektService.hentInntektListe("99999999992", YearMonth.of(2017, 06), YearMonth.of(2017, 8));
        InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();
        assertThat(dokument).isNotNull();
    }
}