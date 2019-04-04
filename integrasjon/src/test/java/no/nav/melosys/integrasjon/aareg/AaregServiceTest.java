package no.nav.melosys.integrasjon.aareg;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AaregServiceTest {

    private static final Long SIKKERHETSBEGRENSET_ID = 1L;

    private AaregService aaregService;

    @Before
    public void setUp() {
        aaregService = lagAaregService(new ArbeidsforholdMock());
    }

    @Test
    public void getArbeidsforholdDokument() throws Exception {
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker("99999999991", null, null);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();
        assertThat(arbeidsforholdDokument.getArbeidsforhold().size()).isGreaterThan(0);
    }

    @Test
    public void getHistoriskArbeidsforholdDokument() throws Exception {
        Saksopplysning saksopplysning = aaregService.hentArbeidsforholdHistorikk(12608035L);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();
        assertThat(arbeidsforholdDokument.getArbeidsforhold().size()).isGreaterThan(0);
        assertThat(arbeidsforholdDokument.getArbeidsforhold().get(0).getArbeidsavtaler().size()).isGreaterThan(1);
    }

    @Test
    public void hentSikkerhetsbegrensetArbeidsforholdHistorikkKasterUnntak() throws Exception {
        ArbeidsforholdConsumer arbeidsforholdConsumer = mockArbeidsforholdConsumer();
        AaregService instans = lagAaregService(arbeidsforholdConsumer);
        Throwable unntak = catchThrowable(() -> instans.hentArbeidsforholdHistorikk(SIKKERHETSBEGRENSET_ID));
        assertThat(unntak).isInstanceOf(SikkerhetsbegrensningException.class)
                .hasMessageContaining("oppslag av arbeidsforhold");
    }

    @Test
    public void hentArbeidsforholdHistorikkMedDysfunksjonellJaxbContextKasterUnntak() throws Exception {
        ArbeidsforholdConsumer arbeidsforholdConsumer = mockArbeidsforholdConsumer();
        JAXBContext dysfunksjonelljaxbContext = JAXBContext.newInstance(Object.class);
        AaregService instans = lagAaregService(arbeidsforholdConsumer, dysfunksjonelljaxbContext);
        Throwable unntak = catchThrowable(() -> instans.hentArbeidsforholdHistorikk(2L));
        assertThat(unntak).isInstanceOf(IntegrasjonException.class)
                .hasCauseInstanceOf(JAXBException.class)
                .hasMessageContaining("oppslag av arbeidsforhold");
    }

    private static ArbeidsforholdConsumer mockArbeidsforholdConsumer() throws Exception {
        ArbeidsforholdConsumer arbeidsforholdConsumer = mock(ArbeidsforholdConsumer.class);
        HentArbeidsforholdHistorikkRequest request = new HentArbeidsforholdHistorikkRequest();
        request.setArbeidsforholdId(SIKKERHETSBEGRENSET_ID);
        when(arbeidsforholdConsumer
                .hentArbeidsforholdHistorikk(argThat(r -> r.getArbeidsforholdId() == SIKKERHETSBEGRENSET_ID)))
                        .thenThrow(new HentArbeidsforholdHistorikkSikkerhetsbegrensning(null, null));
        return arbeidsforholdConsumer;
    }

    private static AaregService lagAaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        return lagAaregService(arbeidsforholdConsumer, null);
    }

    private static AaregService lagAaregService(ArbeidsforholdConsumer arbeidsforholdConsumer, JAXBContext jaxbContext) {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        return jaxbContext != null ? new AaregService(arbeidsforholdConsumer, dokumentFactory, jaxbContext) :
            new AaregService(arbeidsforholdConsumer, dokumentFactory);
    }

}