package no.nav.melosys.saksflyt.steg.msa;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HentRegisteropplysningerTest {

    @Mock
    private RegisteropplysningerService registeropplysningerService;

    private HentRegisteropplysninger hentRegisteropplysninger;

    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final String fnr = "12313";

    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> captor;

    @Before
    public void setup() {
        hentRegisteropplysninger = new HentRegisteropplysninger(registeropplysningerService);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1));
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(soeknadDokument);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, fnr);
    }

    @Test
    public void utfør() throws MelosysException {
        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService).hentOgLagreOpplysninger(captor.capture());

        RegisteropplysningerRequest req = captor.getValue();
        assertThat(req)
            .extracting(RegisteropplysningerRequest::getFnr, RegisteropplysningerRequest::getFom, RegisteropplysningerRequest::getTom)
            .containsExactly(fnr, LocalDate.now(), LocalDate.now().plusYears(1));

    }
}