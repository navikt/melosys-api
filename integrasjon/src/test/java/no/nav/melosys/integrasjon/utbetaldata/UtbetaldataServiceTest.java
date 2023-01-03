package no.nav.melosys.integrasjon.utbetaldata;

import java.time.LocalDate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.WebServiceException;

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
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.*;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtbetaldataServiceTest {

    @Mock
    private UtbetalingConsumer utbetalingConsumer;
    @Mock
    private XsltTemplatesFactory xsltTemplatesFactory;

    private UtbetaldataService utbetaldataService;

    @BeforeEach
    void setup() {
        xsltTemplatesFactory = mock(XsltTemplatesFactory.class);
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.getJaxb2Marshaller(), xsltTemplatesFactory);

        utbetaldataService = new UtbetaldataService(utbetalingConsumer, dokumentFactory);
    }

    @Test
    void hentUtbetalingerBarnetrygd_medTreff_verifiserSaksopplysning() throws Exception {
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
        assertThat(utbetalingDokument.utbetalinger).hasSize(1);
        assertThat(utbetalingDokument.utbetalinger.iterator().next().ytelser).hasSize(1);
        assertThat(utbetalingDokument.utbetalinger.iterator().next().ytelser.iterator().next().type).isEqualToIgnoringCase("Barnetrygd");
    }

    @Test
    void hentUtbetalingerBarnetrygd_medForskjelligeYtelserIEnUtbetaling_verifiserSaksopplysning() throws Exception {
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
        assertThat(utbetalingDokument.utbetalinger).hasSize(2);
        assertThat(utbetalingDokument.utbetalinger.get(0).ytelser).hasSize(1);
        assertThat(utbetalingDokument.utbetalinger.get(0).ytelser.iterator().next().type).isEqualToIgnoringCase("Barnetrygd");
        assertThat(utbetalingDokument.utbetalinger.get(1).ytelser).hasSize(1);
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
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(WebServiceException.class);
        assertThatExceptionOfType(IntegrasjonException.class)
            .isThrownBy(() -> utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now(), LocalDate.now().plusYears(1)));
    }

    @Test
    void hentUtbetalingerBarnetrygd_tomDatoEldreEnnTreÅr_forventTomResponsIngenKall() throws Exception {
        var saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd("123", LocalDate.now().minusYears(5), LocalDate.now().minusYears(4));
        UtbetalingDokument dokument = (UtbetalingDokument) saksopplysning.getDokument();
        assertThat(dokument.utbetalinger.size()).isZero();
        verify(utbetalingConsumer, never()).hentUtbetalingsinformasjon(any());
    }

    @Test
    void hentUtbetalingerBarnetrygd_fomDatoEldreEnnTreÅrTomDatoIDag_forventKallMedFomTreÅrSiden() throws Exception {
        final LocalDate fom = LocalDate.now().minusYears(4);
        final LocalDate tom = LocalDate.now();
        final ArgumentCaptor<WSHentUtbetalingsinformasjonRequest> captor = ArgumentCaptor.forClass(WSHentUtbetalingsinformasjonRequest.class);

        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenReturn(new WSHentUtbetalingsinformasjonResponse());
        assertThat(utbetaldataService.hentUtbetalingerBarnetrygd("123", fom, tom)).isNotNull();
        verify(utbetalingConsumer).hentUtbetalingsinformasjon(captor.capture());

        var periode = captor.getValue().getPeriode();
        assertThat(KonverteringsUtils.jodaDateTimeToJavaLocalDate(periode.getFom())).isEqualTo(tom.minusYears(3));
        assertThat(KonverteringsUtils.jodaDateTimeToJavaLocalDate(periode.getTom())).isEqualTo(tom);
    }

    private WSHentUtbetalingsinformasjonResponse hentResponse(String fnr) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(
            no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse.class).createUnmarshaller();

        return ((no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse)
            unmarshaller.unmarshal(ClassLoader.getSystemResource(String.format("mock/utbetaldata/%s.xml", fnr)))
        ).getHentUtbetalingsinformasjonResponse();
    }
}
