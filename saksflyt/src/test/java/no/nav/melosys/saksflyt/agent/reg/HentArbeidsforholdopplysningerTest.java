package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
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
    private AaregFasade aaregFasade;

    private HentArbeidsforholdopplysninger agent;

    // Antall måneder tilbake i tid vi henter arbeidsforholdhistorikk for
    private final int ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER = 6;

    @Before
    public void setUp() {
        agent = new HentArbeidsforholdopplysninger(aaregFasade, mock(SaksopplysningRepository.class));
        ReflectionTestUtils.setField(agent, "arbeidsforholdhistorikkAntallMåneder", ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
    }

    @Test
    public void utfoerSteg() throws TekniskException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusMonths(1), LocalDate.now());

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(any(), any(), any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        LocalDate fom = periode.getFom().minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate tom = LocalDate.now();

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, fom, tom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_INNT_OPPL);
    }

}
