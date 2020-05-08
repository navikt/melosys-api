package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private BehandlingService behandlingService;
    @InjectMocks
    private RegisteropplysningerService registeropplysningerService;

    private HentPersonopplysninger agent;
    private Behandling behandling;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        agent = new HentPersonopplysninger(registeropplysningerService);

        behandling = new Behandling();
        behandling.setId(222L);
        behandling.setFagsak(new Fagsak());
        behandling.setSaksopplysninger(new HashSet<>());
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        Saksopplysning personopplysning = new Saksopplysning();
        personopplysning.setType(SaksopplysningType.PERSOPL);
        when(tpsFasade.hentPersonMedAdresse(any())).thenReturn(personopplysning);

        Saksopplysning personhistorikkopplysning = new Saksopplysning();
        personhistorikkopplysning.setType(SaksopplysningType.INNTK);
        when(tpsFasade.hentPersonhistorikk(any(), any())).thenReturn(personhistorikkopplysning);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        LocalDate fom = LocalDate.now();
        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(fom, fom));

        agent.utførSteg(p);

        verify(tpsFasade, times(1)).hentPersonMedAdresse(brukerID);
        verify(tpsFasade, times(1)).hentPersonhistorikk(brukerID, fom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }
}