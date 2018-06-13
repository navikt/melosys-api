package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentArbeidsforholdopplysningerTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private AaregFasade aaregFasade;

    private HentArbeidsforholdopplysninger agent;

    // Antall måneder tilbake i tid vi henter arbeidsforholdhistorikk for
    private final int ARBEIDSFORHOLDHISTORIKK_ANTALL_ÅR = 5;

    @Before
    public void setUp() {
        FagsakService fagsakService = mock(FagsakService.class);
        agent = new HentArbeidsforholdopplysninger(binge, repo, aaregFasade, fagsakService);
        ReflectionTestUtils.setField(agent, "arbeidsforholdhistorikkAntallÅr", ARBEIDSFORHOLDHISTORIKK_ANTALL_ÅR);
    }

    @Test
    public void utfoerSteg() throws IntegrasjonException, TekniskException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now(), null);

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(any(), any(), any(), any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        LocalDate fom = periode.getFom().minusYears(ARBEIDSFORHOLDHISTORIKK_ANTALL_ÅR);
        LocalDate tom = LocalDate.now();

        verify(aaregFasade, times(1)).finnArbeidsforholdPrArbeidstaker(brukerID, AaregFasade.REGELVERK_A_ORDNINGEN, fom, tom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_INNT_OPPL);
    }

}
