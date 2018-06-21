package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.service.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentMedlemskapsopplysningerTest {

    @Mock
    private MedlFasade medlFasade;

    private HentMedlemskapsopplysninger agent;

    // Antall måneder tilbake i tid vi henter medlemskaphistorikk for
    private final int MEDLEMSKAPHISTORIKK_ANTALL_ÅR = 5;


    @Before
    public void setUp() {
        FagsakService fagsakService = mock(FagsakService.class);
        agent = new HentMedlemskapsopplysninger(medlFasade, fagsakService, MEDLEMSKAPHISTORIKK_ANTALL_ÅR);
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
        when(medlFasade.hentPeriodeListe(any(), any(), any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        LocalDate fom = periode.getFom().minusYears(MEDLEMSKAPHISTORIKK_ANTALL_ÅR);
        LocalDate tom = LocalDate.now();

        verify(medlFasade, times(1)).hentPeriodeListe(brukerID, fom, tom);
        assertThat(p.getSteg()).isEqualTo(OPPRETT_OPPGAVE);
    }

}
