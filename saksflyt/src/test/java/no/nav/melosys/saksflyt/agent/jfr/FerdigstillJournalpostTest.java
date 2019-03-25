package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Properties;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FerdigstillJournalpostTest {

    @Mock
    private JoarkFasade joarkFasade;

    private FerdigstillJournalpost agent;

    @Before
    public void setUp() {
        agent = new FerdigstillJournalpost(joarkFasade);
    }

    @Test
    public void utførSteg_typeJfrNySak_tilStegJfrHentPersOppl() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_NY_SAK, journalpostID);

        agent.utførSteg(p);

        verify(joarkFasade, times(1)).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_HENT_PERS_OPPL);
    }

    @Test
    public void utførSteg_typeJfrNyBehandling_tilStegJfrHentPersOppl() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_NY_BEHANDLING, journalpostID);

        agent.utførSteg(p);

        verify(joarkFasade, times(1)).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_HENT_PERS_OPPL);
    }

    @Test
    public void utførSteg_typeJfrKnytt_tilStegJfrSettVurderDokument() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_KNYTT, journalpostID);

        agent.utførSteg(p);

        verify(joarkFasade, times(1)).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_SETT_VURDER_DOKUMENT);
    }

    @Test
    public void utførSteg_typeJfrKnyttOgEndretPeriode_tilStegJfrReplikerBehandling() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_NY_BEHANDLING, journalpostID);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);

        agent.utførSteg(p);

        verify(joarkFasade, times(1)).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.REPLIKER_BEHANDLING);
    }

    @Test
    public void utførSteg_typeUgyldig_tilStegFeiletMaskinelt() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(null, journalpostID);

        agent.utførSteg(p);

        verify(joarkFasade, times(1)).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    private Prosessinstans nyProsessinstans(ProsessType prosessType, String journalpostID) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(prosessType);
        Properties properties = new Properties();
        properties.setProperty(ProsessDataKey.JOURNALPOST_ID.getKode(), journalpostID);
        prosessinstans.setData(properties);
        return prosessinstans;
    }
}