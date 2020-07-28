package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Properties;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
    public void utfør_typeJfrNySak_tilStegJfrHentPersOppl() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_NY_SAK, journalpostID);

        agent.utfør(p);

        verify(joarkFasade).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_HENT_REGISTER_OPPL);
    }

    @Test
    public void utfør_typeJfrNyBehandling_tilStegJfrHentPersOppl() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_NY_BEHANDLING, journalpostID);

        agent.utfør(p);

        verify(joarkFasade).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_HENT_REGISTER_OPPL);
    }

    @Test
    public void utfør_typeJfrKnytt_tilStegJfrSettVurderDokument() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_KNYTT, journalpostID);

        agent.utfør(p);

        verify(joarkFasade).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_SETT_VURDER_DOKUMENT);
    }

    @Test
    public void utfør_typeJfrKnyttOgEndretPeriode_tilStegJfrReplikerBehandling() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(ProsessType.JFR_NY_BEHANDLING, journalpostID);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);

        agent.utfør(p);

        verify(joarkFasade).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.REPLIKER_BEHANDLING);
    }

    @Test
    public void utfør_typeUgyldig_tilStegFeiletMaskinelt() throws MelosysException {
        String journalpostID = "Journal_ID";
        Prosessinstans p = nyProsessinstans(null, journalpostID);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> agent.utfør(p))
            .withMessageContaining("Ukjent prosesstype");

        verify(joarkFasade).ferdigstillJournalføring(journalpostID);
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