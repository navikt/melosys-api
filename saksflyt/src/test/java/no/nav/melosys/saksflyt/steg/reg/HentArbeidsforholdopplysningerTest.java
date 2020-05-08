package no.nav.melosys.saksflyt.steg.reg;

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
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentArbeidsforholdopplysningerTest {

    @Mock
    private AaregFasade aaregFasade;
    @Mock
    private BehandlingService behandlingService;
    @InjectMocks
    private RegisteropplysningerService registeropplysningerService;

    private HentArbeidsforholdopplysninger agent;
    // Antall måneder tilbake i tid vi henter arbeidsforholdhistorikk for
    private final int ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER = 6;
    private Behandling behandling;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        agent = new HentArbeidsforholdopplysninger(registeropplysningerService);
        ReflectionTestUtils.setField(registeropplysningerService, "arbeidsforholdhistorikkAntallMåneder", ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);

        behandling = new Behandling();
        behandling.setId(222L);
        behandling.setFagsak(new Fagsak());
        behandling.setSaksopplysninger(new HashSet<>());
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.INNTK);
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(any(), any(), any())).thenReturn(saksopplysning);
    }

    @Test
    public void utfoerSteg() throws TekniskException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusMonths(1), LocalDate.now());

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        LocalDate fom = periode.getFom().minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate tom = LocalDate.now();

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, fom, tom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_INNT_OPPL);
    }

    @Test
    public void utfoerSteg_fremtidigPeriode() throws TekniskException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().plusYears(1), LocalDate.now().plusYears(2));

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        LocalDate forventetFom = LocalDate.now().minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate forventetTom = LocalDate.now();

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, forventetFom, forventetTom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_INNT_OPPL);
    }

    @Test
    public void utfoerSteg_åpenPeriode() throws TekniskException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusYears(2), null);

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        LocalDate forventetFom = LocalDate.now().minusYears(2).minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate forventetTom = forventetFom.plusYears(1);

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, forventetFom, forventetTom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_INNT_OPPL);
    }
}
