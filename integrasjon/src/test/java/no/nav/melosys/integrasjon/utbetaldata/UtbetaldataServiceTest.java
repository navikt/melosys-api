package no.nav.melosys.integrasjon.utbetaldata;

import java.time.LocalDate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.soap.SOAPFaultException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingConsumer;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtbetaldataServiceTest {

    @Mock
    private UtbetalingConsumer utbetalingConsumer;

    private UtbetaldataService utbetaldataService;

    private XsltTemplatesFactory xsltTemplatesFactory;

    @BeforeEach
    public void setup() throws TransformerConfigurationException {
        xsltTemplatesFactory = mock(XsltTemplatesFactory.class);
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), xsltTemplatesFactory);

        utbetaldataService = new UtbetaldataService(utbetalingConsumer, dokumentFactory);
    }

    @Test
    void hentUtbetalingerBarnetrygd_medTreff_verifiserSaksopplysning() throws Exception {
        Templates xsltTemplates = TransformerFactory.newInstance().newTemplates(new StreamSource(
            ClassLoader.getSystemResourceAsStream("utbetaling/utbetaldata_1.0.xslt")));
        when(xsltTemplatesFactory.getXsltTemplates(any(), anyString())).thenReturn(xsltTemplates);
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenReturn(hentResponse("77777777776"));

        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1));

        verify(utbetalingConsumer).hentUtbetalingsinformasjon(any());

        assertThat(saksopplysning).isNotNull();
        assertThat(saksopplysning.getType()).isEqualTo(SaksopplysningType.UTBETAL);
        assertThat(saksopplysning.getVersjon()).isEqualTo("1.0");
        assertThat(saksopplysning.getKilder()).isNotNull();
        SaksopplysningKilde kilde = saksopplysning.getKilder().iterator().next();
        assertThat(kilde.getMottattDokument()).isNotEmpty();
        assertThat(kilde.getKilde()).isEqualTo(SaksopplysningKildesystem.UTBETALDATA);

        UtbetalingDokument utbetalingDokument = (UtbetalingDokument) saksopplysning.getDokument();
        assertThat(utbetalingDokument).isNotNull();
        assertThat(utbetalingDokument.utbetalinger.size()).isEqualTo(1);
        assertThat(utbetalingDokument.utbetalinger.iterator().next().ytelser.size()).isEqualTo(1);
        assertThat(utbetalingDokument.utbetalinger.iterator().next().ytelser.iterator().next().type).isEqualToIgnoringCase("Barnetrygd");
    }

    @Test
    void hentUtbetalingerBarnetrygd_medForskjelligeYtelserIEnUtbetaling_verifiserSaksopplysning() throws Exception {
        Templates xsltTemplates = TransformerFactory.newInstance().newTemplates(new StreamSource(
            ClassLoader.getSystemResourceAsStream("utbetaling/utbetaldata_1.0.xslt")));
        when(xsltTemplatesFactory.getXsltTemplates(any(), anyString())).thenReturn(xsltTemplates);
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenReturn(hentResponse("77777777777"));

        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1));

        verify(utbetalingConsumer).hentUtbetalingsinformasjon(any());

        assertThat(saksopplysning).isNotNull();
        assertThat(saksopplysning.getType()).isEqualTo(SaksopplysningType.UTBETAL);
        assertThat(saksopplysning.getVersjon()).isEqualTo("1.0");
        assertThat(saksopplysning.getKilder()).isNotNull();
        SaksopplysningKilde kilde = saksopplysning.getKilder().iterator().next();
        assertThat(kilde.getMottattDokument()).isNotEmpty();
        assertThat(kilde.getKilde()).isEqualTo(SaksopplysningKildesystem.UTBETALDATA);

        UtbetalingDokument utbetalingDokument = (UtbetalingDokument) saksopplysning.getDokument();
        assertThat(utbetalingDokument).isNotNull();
        assertThat(utbetalingDokument.utbetalinger.size()).isEqualTo(2);
        assertThat(utbetalingDokument.utbetalinger.get(0).ytelser.size()).isEqualTo(1);
        assertThat(utbetalingDokument.utbetalinger.get(0).ytelser.iterator().next().type).isEqualToIgnoringCase("Barnetrygd");
        assertThat(utbetalingDokument.utbetalinger.get(1).ytelser.size()).isEqualTo(1);
        assertThat(utbetalingDokument.utbetalinger.get(1).ytelser.iterator().next().type).isEqualToIgnoringCase("Barnetrygd");
    }

    @Test
    void hentUtbetalingerBarnetrygd_personIkkeFunnet_forventException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(HentUtbetalingsinformasjonPersonIkkeFunnet.class);
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1)))
            .withMessageContaining("Oppgitt person ble ikke funnet");
    }

    @Test
    void hentUtbetalingerBarnetrygd_ugyldigPeriode_forventException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(HentUtbetalingsinformasjonPeriodeIkkeGyldig.class);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1)))
            .withMessageContaining("Oppgitt periode er ikke");
    }

    @Test
    void hentUtbetalingerBarnetrygd_ikkeTilgang_forventException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(HentUtbetalingsinformasjonIkkeTilgang.class);
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1)))
            .withMessageContaining("Har ikke tilgang");
    }

    @Test
    void hentUtbetalingerBarnetrygd_soapFault_forventIntegrasjonException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(SOAPFaultException.class);
        assertThatExceptionOfType(IntegrasjonException.class)
            .isThrownBy(() -> utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1)));
    }

    @Test
    void hentUtbetalingerBarnetrygd_tomDatoEldreEnnTreÅr_forventTomResponsIngenKall() throws Exception {
        Templates xsltTemplates = TransformerFactory.newInstance().newTemplates(new StreamSource(
            ClassLoader.getSystemResourceAsStream("utbetaling/utbetaldata_1.0.xslt")));
        when(xsltTemplatesFactory.getXsltTemplates(any(), anyString())).thenReturn(xsltTemplates);
        var saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now().minusYears(5), LocalDate.now().minusYears(4));
        UtbetalingDokument dokument = (UtbetalingDokument) saksopplysning.getDokument();
        assertThat(dokument.utbetalinger.size()).isZero();
        verify(utbetalingConsumer, never()).hentUtbetalingsinformasjon(any());
    }

    @Test
    void hentUtbetalingerBarnetrygd_fomDatoEldreEnnTreÅrTomDatoIDag_forventKallMedFomTreÅrSiden() throws Exception {
        final LocalDate fom = LocalDate.now().minusYears(4);
        final LocalDate tom = LocalDate.now();
        final ArgumentCaptor<HentUtbetalingsinformasjonRequest> captor = ArgumentCaptor.forClass(HentUtbetalingsinformasjonRequest.class);

        Templates xsltTemplates = TransformerFactory.newInstance().newTemplates(new StreamSource(
            ClassLoader.getSystemResourceAsStream("utbetaling/utbetaldata_1.0.xslt")));
        when(xsltTemplatesFactory.getXsltTemplates(any(), anyString())).thenReturn(xsltTemplates);
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenReturn(new HentUtbetalingsinformasjonResponse());
        assertThat(utbetaldataService.hentUtbetalingerBarnetrygd("123", fom, tom)).isNotNull();
        verify(utbetalingConsumer).hentUtbetalingsinformasjon(captor.capture());

        var periode = captor.getValue().getPeriode();
        assertThat(KonverteringsUtils.xmlGregorianCalendarToLocalDate(periode.getFom())).isEqualTo(tom.minusYears(3));
        assertThat(KonverteringsUtils.xmlGregorianCalendarToLocalDate(periode.getTom())).isEqualTo(tom);
    }

    private HentUtbetalingsinformasjonResponse hentResponse(String fnr) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(
            no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse.class).createUnmarshaller();

        return ((no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse)
            unmarshaller.unmarshal(ClassLoader.getSystemResource(String.format("mock/utbetaldata/%s.xml", fnr)))
        ).getHentUtbetalingsinformasjonResponse();
    }
}