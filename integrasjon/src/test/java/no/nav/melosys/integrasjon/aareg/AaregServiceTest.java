package no.nav.melosys.integrasjon.aareg;

import static no.nav.melosys.integrasjon.aareg.AaregFasade.REGELVERK_A_ORDNINGEN;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Before;
import org.junit.Test;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;

public class AaregServiceTest {

    private AaregService aaregService;

    @Before
    public void setUp() {
        ArbeidsforholdConsumer arbeidsforholdConsumer = new ArbeidsforholdMock();
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        aaregService = new AaregService(arbeidsforholdConsumer, dokumentFactory);
    }

    @Test
    public void getArbeidsforholdDokument() throws Exception {
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker("FJERNET", REGELVERK_A_ORDNINGEN, null, null);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();
        assertThat(arbeidsforholdDokument.getArbeidsforhold().size()).isGreaterThan(0);
    }




}