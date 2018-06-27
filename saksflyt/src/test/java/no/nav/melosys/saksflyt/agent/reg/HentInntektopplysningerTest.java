package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentInntektopplysningerTest {

    @Mock
    InntektFasade inntektFasade;

    private HentInntektopplysninger agent;

    // Antall måneder tilbake i tid vi henter inntekthistorikk for
    private final int INNTEKTSHISTORIKK_ANTALL_MÅNEDER = 12;

    @Before
    public void setUp() {
        FagsakService fagsakService = mock(FagsakService.class);
        agent = new HentInntektopplysninger(inntektFasade, mock(SaksopplysningRepository.class));
        ReflectionTestUtils.setField(agent, "inntektshistorikkAntallMåneder", INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
    }

    @Test
    public void utfoerSteg() throws IntegrasjonException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now(), null);

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);
        when(inntektFasade.hentInntektListe(any(), any(), any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        YearMonth fom = YearMonth.from(periode.getFom()).minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth tom = YearMonth.now();

        verify(inntektFasade, times(1)).hentInntektListe(brukerID, fom, tom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_ORG_OPPL);
    }

}
