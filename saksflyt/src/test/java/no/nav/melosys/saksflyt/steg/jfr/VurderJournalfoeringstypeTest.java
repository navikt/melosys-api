package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Arrays;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VurderJournalfoeringstypeTest {

    private VurderJournalfoeringstype agent;

    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_AKTIV_BEHANDLING = "MELTEST-2";
    private final static String SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING = "MELTEST-4";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private FagsakService fagsakService;

    @Before
    public void setUp() throws IkkeFunnetException {
        agent = new VurderJournalfoeringstype(fagsakService);

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(Collections.emptyList());

        Fagsak fagsakMedAktivBehandling = new Fagsak();
        Fagsak fagsakMedInaktivBehandling = new Fagsak();
        Fagsak fagsakMedInaktivOgAktivBehandling = new Fagsak();
        Behandling aktivBehandling = new Behandling();
        aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Behandling inaktivBehandling = new Behandling();
        inaktivBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsakMedAktivBehandling.setBehandlinger(Collections.singletonList(aktivBehandling));
        fagsakMedInaktivBehandling.setBehandlinger(Collections.singletonList(inaktivBehandling));
        fagsakMedInaktivOgAktivBehandling.setBehandlinger(Arrays.asList(aktivBehandling, inaktivBehandling));

        when(fagsakService.hentFagsak(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsak);
        when(fagsakService.hentFagsak(SAKSNUMMER_MED_AKTIV_BEHANDLING)).thenReturn(fagsakMedAktivBehandling);
        when(fagsakService.hentFagsak(SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING)).thenReturn(fagsakMedInaktivBehandling);
    }

    @Test
    public void ukjentProsesstype_Feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> agent.utfør(p))
            .withMessageContaining("Ukjent prosesstype");
    }

    @Test
    public void nySak_tilHentAktørID() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }

    @Test
    public void knyttTilFagsakMedAktivBehandling_tilOppdaterJournalpost() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }

    @Test
    public void knyttMedBehandlingstypeNull_tilOppdaterJournalpost() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }

    @Test
    public void knyttTilFagsakUtenAktivBehandling_tilNyBehandling() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        agent.utfør(p);
        assertThat(p.getType()).isEqualTo(ProsessType.JFR_NY_BEHANDLING);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedInaktivOgUtenAktivBehandling_steg_jfrOppdaterJournalpost() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedAktivBehandling_kasterException() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> agent.utfør(p))
            .withMessageContaining("Man kan ikke endre lovvalgsperiode");
    }
}
