package no.nav.melosys.saksflyt.agent.iv;

import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.IV_SEND_BREV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    private OppdaterMedl agent;

    @Mock
    private MedlFasade medlFasade;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;

    @Before
    public void setUp() {
        agent = new OppdaterMedl(medlFasade, tpsFasade, lovvalgsperiodeRepository);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        Properties properties = new Properties();
        properties.setProperty(ProsessDataKey.AKTØR_ID.getKode(), "12345678912");
        p.addData(properties);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_2);
        lovvalgsperiode.setLovvalgsland(Landkoder.CH);
        lovvalgsperiode.setDekning(TrygdeDekning.UTEN_DEKNING);

        when(lovvalgsperiodeRepository.findByBehandlingsresultatId(anyLong())).thenReturn(lovvalgsperiode);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("12345678910");

        agent.utførSteg(p);

        verify(medlFasade, times(1)).opprettPeriode(anyString(), Mockito.any(Medlemsperiode.class));
        assertThat(p.getSteg()).isEqualTo(IV_SEND_BREV);
    }
}