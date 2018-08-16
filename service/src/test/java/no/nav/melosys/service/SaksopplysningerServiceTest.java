package no.nav.melosys.service;

import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.ereg.EregService;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektMock;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaksopplysningerServiceTest {

    private SaksopplysningerService saksopplysningerService;

    @Mock
    private TpsFasade tpsFasade;

    @Before
    public void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        MedlFasade medl = new MedlService(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);

        saksopplysningerService = new SaksopplysningerService(tpsFasade, aareg, ereg, medl, inntekt);

        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallÅr", 5);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 12);
        ReflectionTestUtils.setField(saksopplysningerService, "medlemskaphistorikkAntallÅr", 5);
    }

    @Test
    public void hentArbeidsforholdHistorikk() throws MelosysException {
        final Long arbeidsforholdsID = 12608035L;
        ArbeidsforholdDokument dokument = saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        assertFalse(dokument.getArbeidsforhold().isEmpty());
        assertTrue(dokument.getArbeidsforhold().get(0).getArbeidsavtaler().size() > 1);
    }

    @Ignore
    @Test
    public void hentSaksopplysninger() throws Exception {
        // Skru av logging for denne testen siden den skaper mye forventet støy
        final Logger log = (Logger) LoggerFactory.getLogger(SaksopplysningerService.class);
        Level opprinneligLevel = log.getLevel();
        // log.setLevel(Level.OFF);

        final String[] identer = new String[]{"88888888884", "88888888885"};

        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(String.valueOf(returnsFirstArg()));
        when(tpsFasade.hentPersonMedAdresse(anyString())).thenReturn(new Saksopplysning());

        for (String fnr : identer) {
            Set<Saksopplysning> saksopplysninger = saksopplysningerService.hentSaksopplysninger(fnr);

            assertFalse(saksopplysninger.isEmpty());
        }

        // Skru på logging igjen
        log.setLevel(opprinneligLevel);
    }
}