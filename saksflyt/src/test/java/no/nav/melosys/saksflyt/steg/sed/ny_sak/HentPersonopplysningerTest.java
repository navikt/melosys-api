package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    private HentPersonopplysninger hentPersonopplysninger;

    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private TpsFasade tpsFasade;
    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> captor;

    @Before
    public void setup() throws IkkeFunnetException {
        hentPersonopplysninger = new HentPersonopplysninger(registeropplysningerService, tpsFasade);

        when(tpsFasade.hentIdentForAktørId(eq("123"))).thenReturn("321");
    }

    @Test
    public void utfør() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123");
        prosessinstans.setBehandling(behandling);
        hentPersonopplysninger.utfør(prosessinstans);

        verify(registeropplysningerService).hentOgLagreOpplysninger(captor.capture());

        RegisteropplysningerRequest registeropplysningerRequest = captor.getValue();
        assertThat(registeropplysningerRequest.getFnr()).isEqualTo("321");
        assertThat(registeropplysningerRequest.getFom()).isNull();
        assertThat(registeropplysningerRequest.getTom()).isNull();
        assertThat(registeropplysningerRequest.getOpplysningstyper()).containsExactly(SaksopplysningType.PERSOPL);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE);
    }
}