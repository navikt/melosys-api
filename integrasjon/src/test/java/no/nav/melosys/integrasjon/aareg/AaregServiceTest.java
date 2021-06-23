package no.nav.melosys.integrasjon.aareg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AaregServiceTest {
    private static final Long SIKKERHETSBEGRENSET_ID = 1L;

    private AaregService aaregService;
    private Jaxb2Marshaller marshaller;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public void setupBeforeAll() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void setUp() {
        marshaller = JaxbConfig.jaxb2Marshaller();
        aaregService = lagAaregService(new ArbeidsforholdMock());
    }

    @Test
    public void getArbeidsforholdDokument() throws Exception {
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker("99999999991", null, null);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();
        assertThat(arbeidsforholdDokument.getArbeidsforhold().size()).isGreaterThan(0);
    }

    @Test
    public void getArbeidsforholdDokument_CheckJsonResult() throws Exception {
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker("88888888885", null, null);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();

        String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arbeidsforholdDokument);
        assertThat(result).isEqualTo(expectedSoapApiResult);
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

    private static ArbeidsforholdConsumer mockArbeidsforholdConsumer() throws Exception {
        ArbeidsforholdConsumer arbeidsforholdConsumer = mock(ArbeidsforholdConsumer.class);
        HentArbeidsforholdHistorikkRequest request = new HentArbeidsforholdHistorikkRequest();
        request.setArbeidsforholdId(SIKKERHETSBEGRENSET_ID);
        when(arbeidsforholdConsumer
            .hentArbeidsforholdHistorikk(argThat(r -> r.getArbeidsforholdId() == SIKKERHETSBEGRENSET_ID)))
            .thenThrow(new HentArbeidsforholdHistorikkSikkerhetsbegrensning(null, null));
        return arbeidsforholdConsumer;
    }

    private AaregService lagAaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        DokumentFactory dokumentFactory = new DokumentFactory(marshaller, new XsltTemplatesFactory());
        FakeUnleash unleash = new FakeUnleash();
        return new AaregService(arbeidsforholdConsumer, dokumentFactory, new ArbeidsforholdRestConsumer(null), unleash);
    }

    private final String expectedSoapApiResult = """
        [ {
          "arbeidsforholdID" : "V974600951R131438S1001L0001",
          "arbeidsforholdIDnav" : 39525427,
          "ansettelsesPeriode" : {
            "fom" : [ 2016, 4, 1 ],
            "tom" : null
          },
          "arbeidsforholdstype" : "Ordinært arbeidsforhold",
          "arbeidsavtaler" : [ {
            "arbeidstidsordning" : {
              "kode" : "ikkeSkift"
            },
            "avloenningstype" : "Fastlønn",
            "yrke" : {
              "kode" : "3119136",
              "term" : "INGENIØR (ØVRIG TEKNISK VIRKSOMHET)"
            },
            "gyldighetsperiode" : {
              "fom" : [ 2016, 4, 1 ],
              "tom" : null
            },
            "avtaltArbeidstimerPerUke" : 37.5,
            "stillingsprosent" : 100.0,
            "sisteLoennsendringsdato" : [ 2016, 4, 1 ],
            "beregnetAntallTimerPrUke" : 37.5,
            "endringsdatoStillingsprosent" : [ 2016, 4, 1 ],
            "skipsregister" : {
              "kode" : null
            },
            "skipstype" : {
              "kode" : null
            },
            "maritimArbeidsavtale" : false,
            "beregnetStillingsprosent" : null,
            "antallTimerGammeltAa" : null,
            "fartsomraade" : null
          } ],
          "permisjonOgPermittering" : [ ],
          "utenlandsopphold" : [ ],
          "arbeidsgivertype" : "ORGANISASJON",
          "arbeidsgiverID" : "974600951",
          "arbeidstakerID" : "88888888885",
          "opplysningspliktigtype" : "ORGANISASJON",
          "opplysningspliktigID" : "964338531",
          "opprettelsestidspunkt" : 1460536367.299000000,
          "sistBekreftet" : 1498651290.000000000,
          "Aordning" : true,
          "timerTimelonnet" : [ ]
        } ]""";
}
