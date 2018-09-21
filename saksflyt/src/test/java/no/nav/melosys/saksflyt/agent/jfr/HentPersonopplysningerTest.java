package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDate;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private HentPersonopplysninger agent;

    @Before
    public void setUp() {
        agent = new HentPersonopplysninger(saksopplysningRepository, tpsFasade);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String brukerID = "99999999991";
        LocalDate fom = LocalDate.now();
        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(fom, fom));
        when(tpsFasade.hentPersonMedAdresse(any())).thenReturn(new Saksopplysning());
        when(tpsFasade.hentPersonhistorikk(any(), any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        verify(tpsFasade, times(1)).hentPersonMedAdresse(brukerID);
        verify(tpsFasade, times(1)).hentPersonhistorikk(brukerID, fom);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }
}