package no.nav.melosys.saksflyt.agent.iv;

import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.IV_SENDBREV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMEDLTest {

    private OppdaterMEDL agent;

    @Mock
    private MedlFasade medlFasade;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;

    @Before
    public void setUp() {
        agent = new OppdaterMEDL(medlFasade, tpsFasade, lovvalgsperiodeRepository);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        Properties properties = new Properties();
        p.addData(properties);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_2);
        lovvalgsperiode.setLovvalgsland(Landkoder.CH);
        lovvalgsperiode.setDekning(TrygdeDekning.UTEN_DEKNING);

        when(lovvalgsperiodeRepository.findByBehandlingsresultatId(anyLong())).thenReturn(lovvalgsperiode);

        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(IV_SENDBREV);
    }
}