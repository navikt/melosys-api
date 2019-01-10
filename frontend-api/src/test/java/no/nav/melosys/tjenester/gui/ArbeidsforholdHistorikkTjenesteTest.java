package no.nav.melosys.tjenester.gui;

import javax.ws.rs.core.Response;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.SaksopplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArbeidsforholdHistorikkTjenesteTest {

    private ArbeidsforholdHistorikkTjeneste tjeneste;

    @Mock
    SaksopplysningerService saksopplysningerService;

    @Before
    public void setUp() throws IntegrasjonException, SikkerhetsbegrensningException, IkkeFunnetException {
        when(saksopplysningerService.hentArbeidsforholdHistorikk(anyLong())).thenReturn(new ArbeidsforholdDokument());
        tjeneste = new ArbeidsforholdHistorikkTjeneste(saksopplysningerService);
    }

    @Test
    public void getHistoriskArbeidsforholdDokument() throws Exception {
        Response response = tjeneste.hentArbeidsforholdHistorikk(12608035L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(ArbeidsforholdDokument.class);
        verify(saksopplysningerService).hentArbeidsforholdHistorikk(eq(12608035L));
        response.close();
    }
}
