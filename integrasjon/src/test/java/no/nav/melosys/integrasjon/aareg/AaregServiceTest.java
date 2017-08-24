package no.nav.melosys.integrasjon.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.AnsettelsesPeriode;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Gyldighetsperiode;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Yrker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class AaregServiceTest {

    @Mock
    ArbeidsforholdConsumer arbeidsforholdConsumer;

    private AaregService aaregService;

    @Before
    public void setUp() {
        aaregService = new AaregService(arbeidsforholdConsumer);
    }
    
    /* FIXME
    @Test
    public void finnArbeidsforholdPrArbeidstaker() throws Exception {
        String ident = "test";

        FinnArbeidsforholdPrArbeidstakerResponse response = new FinnArbeidsforholdPrArbeidstakerResponse();
        Arbeidsforhold forhold_1 = new Arbeidsforhold();
        Organisasjon aktoer = new Organisasjon();
        aktoer.setNavn("John");
        aktoer.setOrgnummer("1234");
        forhold_1.setArbeidsgiver(aktoer);
        AnsettelsesPeriode ansettelsesPeriode = new AnsettelsesPeriode();
        Gyldighetsperiode gyldighetsperiode = new Gyldighetsperiode();
        XMLGregorianCalendar fom_1 = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        gyldighetsperiode.setFom(fom_1);
        ansettelsesPeriode.setPeriode(gyldighetsperiode );
        forhold_1.setAnsettelsesPeriode(ansettelsesPeriode);
        
        List<Arbeidsavtale> arbeidsavtaler = forhold_1.getArbeidsavtale();
        Arbeidsavtale avtale_1= new Arbeidsavtale();
        Yrker yrke = new Yrker();
        String yrkeStr = "0013004";
        yrke.setValue(yrkeStr);
        avtale_1.setYrke(yrke);
        XMLGregorianCalendar fomBrukPeriode = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        avtale_1.setFomBruksperiode(fomBrukPeriode);
        arbeidsavtaler.add(avtale_1);

        response.getArbeidsforhold().add(forhold_1);

        when(arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(any())).thenReturn(response);

        List<no.nav.melosys.domain.Arbeidsforhold> list = aaregService.finnArbeidsforholdPrArbeidstaker(ident);
        assertThat(list.size()).isEqualTo(1);

        no.nav.melosys.domain.Arbeidsforhold forhold = list.get(0);
        List<no.nav.melosys.domain.Arbeidsavtale> arbeidsavtaleListe = forhold.getArbeidsavtaleListe();
        assertThat(arbeidsavtaleListe.size()).isEqualTo(1);

        no.nav.melosys.domain.Arbeidsavtale arbeidsavtale = arbeidsavtaleListe.get(0);
        assertThat(arbeidsavtale.getYrke()).isEqualTo(yrkeStr);

    }
    // */

}