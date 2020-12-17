package no.nav.melosys.integrasjon.utbetaldata;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

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
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingConsumer;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UtbetaldataServiceTest {

    @Mock
    private UtbetalingConsumer utbetalingConsumer;

    private UtbetaldataService utbetaldataService;

    @Before
    public void setup() throws TransformerConfigurationException {
        XsltTemplatesFactory xsltTemplatesFactory = mock(XsltTemplatesFactory.class);
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.jaxb2Marshaller(), xsltTemplatesFactory);

        Templates xsltTemplates = TransformerFactory.newInstance().newTemplates(new StreamSource(
            ClassLoader.getSystemResourceAsStream("utbetaling/utbetaldata_1.0.xslt")));
        when(xsltTemplatesFactory.getXsltTemplates(any(), anyString())).thenReturn(xsltTemplates);

        utbetaldataService = new UtbetaldataService(utbetalingConsumer, dokumentFactory);
    }

    @Test
    public void hentUtbetalingerBarnetrygd_medTreff_verifiserSaksopplysning() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenReturn(hentResponse("77777777776"));

        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd("123", null, null);

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
    public void hentUtbetalingerBarnetrygd_medForskjelligeYtelserIEnUtbetaling_verifiserSaksopplysning() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenReturn(hentResponse("77777777777"));

        Saksopplysning saksopplysning = utbetaldataService.hentUtbetalingerBarnetrygd("123", null, null);

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

    @Test(expected = IkkeFunnetException.class)
    public void hentUtbetalingerBarnetrygd_personIkkeFunnet_forventException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(HentUtbetalingsinformasjonPersonIkkeFunnet.class);
        utbetaldataService.hentUtbetalingerBarnetrygd("123", null, null);
        verify(utbetalingConsumer).hentUtbetalingsinformasjon(any());
    }

    @Test(expected = FunksjonellException.class)
    public void hentUtbetalingerBarnetrygd_ugyldigPeriode_forventException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(HentUtbetalingsinformasjonPeriodeIkkeGyldig.class);
        utbetaldataService.hentUtbetalingerBarnetrygd("123", null, null);
        verify(utbetalingConsumer).hentUtbetalingsinformasjon(any());
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void hentUtbetalingerBarnetrygd_ikkeTilgang_forventException() throws Exception {
        when(utbetalingConsumer.hentUtbetalingsinformasjon(any())).thenThrow(HentUtbetalingsinformasjonIkkeTilgang.class);
        utbetaldataService.hentUtbetalingerBarnetrygd("123", null, null);
        verify(utbetalingConsumer).hentUtbetalingsinformasjon(any());
    }

    private HentUtbetalingsinformasjonResponse hentResponse(String fnr) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(
            no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse.class).createUnmarshaller();

        return ((no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse)
            unmarshaller.unmarshal(ClassLoader.getSystemResource(String.format("mock/utbetaldata/%s.xml", fnr)))
        ).getHentUtbetalingsinformasjonResponse();
    }
}