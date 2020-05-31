package no.nav.melosys.service.sob;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SobServiceTest {

    private static final String SAKSNUMMER = "MEL-1234";
    private static final long BEHANDING_ID = 1L;
    private static final String AKTØR_ID = "98987878";

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private BehandlingService behandlingService;

    @Captor
    private ArgumentCaptor<BehandlingStatusMapper> captor;

    private SobService sobService;

    @Before
    public void setUp() throws Exception {
        sobService = new SobService(sakOgBehandlingFasade, tpsFasade, behandlingService);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(AKTØR_ID);
    }

    @Test
    public void sakOgBehandlingOpprettet_forventMapperMedVerdier() throws FunksjonellException, TekniskException {
        sobService.sakOgBehandlingOpprettet(SAKSNUMMER, BEHANDING_ID, AKTØR_ID);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(eq(BEHANDING_ID));
        verify(tpsFasade, never()).hentAktørIdForIdent(anyString());
        verify(sakOgBehandlingFasade).sendBehandlingOpprettet(captor.capture());

        BehandlingStatusMapper behandlingStatusMapper = captor.getValue();
        assertThat(behandlingStatusMapper.getAktoerREF().getAktoerId()).isEqualTo(AKTØR_ID);
        assertThat(behandlingStatusMapper.getBehandlingsID()).contains(String.valueOf(BEHANDING_ID));
        assertThat(behandlingStatusMapper.getApplikasjonSakREF()).isEqualTo(SAKSNUMMER);
        assertThat(behandlingStatusMapper.getSakstema().getValue()).isEqualTo(Tema.UFM.getKode());
    }

    @Test
    public void sakOgBehandlingAvsluttet_forventMapperMedVerdier() throws FunksjonellException, TekniskException {
        sobService.sakOgBehandlingAvsluttet(SAKSNUMMER, BEHANDING_ID, AKTØR_ID);

        verify(behandlingService).hentBehandling(eq(BEHANDING_ID));
        verify(tpsFasade, never()).hentAktørIdForIdent(anyString());
        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(captor.capture());

        BehandlingStatusMapper behandlingStatusMapper = captor.getValue();
        assertThat(behandlingStatusMapper.getAktoerREF().getAktoerId()).isEqualTo(AKTØR_ID);
        assertThat(behandlingStatusMapper.getBehandlingsID()).contains(String.valueOf(BEHANDING_ID));
        assertThat(behandlingStatusMapper.getApplikasjonSakREF()).isEqualTo(SAKSNUMMER);
        assertThat(behandlingStatusMapper.getSakstema().getValue()).isEqualTo(Tema.UFM.getKode());
    }

    @Test
    public void sakOgBehandlingAvsluttet_ingenAktørID_forventKallMotTps() throws FunksjonellException, TekniskException {
        sobService.sakOgBehandlingAvsluttet(SAKSNUMMER, BEHANDING_ID, null);

        verify(behandlingService).hentBehandling(eq(BEHANDING_ID));
        verify(tpsFasade).hentAktørIdForIdent(anyString());
        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(captor.capture());

        BehandlingStatusMapper behandlingStatusMapper = captor.getValue();
        assertThat(behandlingStatusMapper.getAktoerREF().getAktoerId()).isEqualTo(AKTØR_ID);
        assertThat(behandlingStatusMapper.getBehandlingsID()).contains(String.valueOf(BEHANDING_ID));
        assertThat(behandlingStatusMapper.getApplikasjonSakREF()).isEqualTo(SAKSNUMMER);
        assertThat(behandlingStatusMapper.getSakstema().getValue()).isEqualTo(Tema.UFM.getKode());
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setId(1L);

        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "123";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        return behandling;
    }
}
