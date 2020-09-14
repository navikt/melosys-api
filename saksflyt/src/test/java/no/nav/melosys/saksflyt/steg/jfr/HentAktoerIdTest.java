package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentAktoerIdTest {

    @Mock
    private TpsFasade tpsFasade;

    private HentAktoerId agent;

    @Before
    public void setUp() {
        agent = new HentAktoerId(tpsFasade);
    }

    @Test
    public void utfoerSteg_BehadlingstypeSOEKNAD_nesteStegBlirJFR_OPPRETT_SAK_OG_BEH() throws IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        String brukerID = "99999999991";
        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        when(tpsFasade.hentAktørIdForIdent(any())).thenReturn("1000104568393");

        agent.utfør(p);

        verify(tpsFasade).hentAktørIdForIdent(brukerID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_SAK_OG_BEH);
    }

    @Test
    public void utfoerSteg_BehadlingstypeENDRET_PERIODE_nesteStegBlirJFR_OPPDATER_JOURNALPOST() throws IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        String brukerID = "99999999991";
        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        when(tpsFasade.hentAktørIdForIdent(any())).thenReturn("1000104568393");

        agent.utfør(p);

        verify(tpsFasade).hentAktørIdForIdent(brukerID);
    }
}