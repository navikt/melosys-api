package no.nav.melosys.saksflyt.steg.reg;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentInntektopplysningerTest {

    @Mock
    private InntektService inntektService;
    @Mock
    private BehandlingService behandlingService;
    @InjectMocks
    private RegisteropplysningerService registeropplysningerService;

    private HentInntektopplysninger agent;
    // Antall måneder tilbake i tid vi henter inntekthistorikk for
    private final int INNTEKTSHISTORIKK_ANTALL_MÅNEDER = 6;
    private Behandling behandling;

    @Before
    public void setUp() throws FunksjonellException, IntegrasjonException {
        agent = new HentInntektopplysninger(registeropplysningerService);

        behandling = new Behandling();
        behandling.setId(222L);
        behandling.setFagsak(new Fagsak());
        behandling.setSaksopplysninger(new HashSet<>());
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.INNTK);
        when(inntektService.hentInntektListe(any(), any(), any())).thenReturn(saksopplysning);
    }

    @Test
    public void utfoerSteg() throws IntegrasjonException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusMonths(1), LocalDate.now());

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        YearMonth forventetFom = YearMonth.from(periode.getFom()).minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth forventetTom = YearMonth.now();

        verify(inntektService).hentInntektListe(brukerID, forventetFom, forventetTom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_ORG_OPPL);
    }

    @Test
    public void utfoerSteg_fremtidigPeriode() throws IntegrasjonException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().plusYears(1), LocalDate.now().plusYears(2));

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        YearMonth forventetFom = YearMonth.now().minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth forventetTom = YearMonth.now();

        verify(inntektService).hentInntektListe(brukerID, forventetFom, forventetTom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_ORG_OPPL);
    }

    @Test
    public void utfoerSteg_åpenPeriode() throws IntegrasjonException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusYears(2), null);

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        YearMonth forventetFom = YearMonth.now().minusYears(2).minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth forventetTom = forventetFom.plusYears(1);

        verify(inntektService).hentInntektListe(brukerID, forventetFom, forventetTom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_ORG_OPPL);
    }
}
