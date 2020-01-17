package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.steg.gsak.OpprettGsakSak;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettGsakSakTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private FagsakRepository fagsakRepository;

    private OpprettGsakSak agent;

    @Before
    public void setUp() {
        agent = new OpprettGsakSak(gsakFasade, fagsakRepository);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Behandlingstyper behandlingstyper = Behandlingstyper.SOEKNAD;
        String saksnummer = "MEL-009";
        String aktørID = "1000104568393";

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstyper);
        when(gsakFasade.opprettSak(anyString(), eq(behandlingstyper), anyString())).thenReturn(123L);

        Fagsak fagsak = new Fagsak();
        when(fagsakRepository.findBySaksnummer(any())).thenReturn(fagsak);

        agent.utførSteg(prosessinstans);

        verify(gsakFasade, times(1)).opprettSak(saksnummer, behandlingstyper, aktørID);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.STATUS_BEH_OPPR);
        Assert.notNull(fagsak.getGsakSaksnummer(), "Fagsak skal ha fått Gsak saksnummert");
    }
}