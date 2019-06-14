package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class GrunnleggendeValideringTest {

    private GrunnleggendeValidering agent;
    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_AKTIV_BEHANDLING = "MELTEST-2";
    private final static String SAKSNUMMER_MED_AKTIV_BEHANDLING_OG_INAKTIV_BEHANDLING = "MELTEST-3";
    private final static String SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING = "MELTEST-4";

    @Before
    public void setUp() {
        FagsakRepository fagsakRepository = Mockito.mock(FagsakRepository.class);
        agent = new GrunnleggendeValidering(fagsakRepository);

        Fagsak fagsakUtenBehandlinger = new Fagsak();
        fagsakUtenBehandlinger.setBehandlinger(Collections.emptyList());
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

        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsakUtenBehandlinger);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_AKTIV_BEHANDLING)).thenReturn(fagsakMedAktivBehandling);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_AKTIV_BEHANDLING_OG_INAKTIV_BEHANDLING)).thenReturn(fagsakMedInaktivOgAktivBehandling);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING)).thenReturn(fagsakMedInaktivBehandling);
    }

    @Test
    public void utførSteg_ukjentProsess_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_nySak_tilInkommendeDok() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(LocalDate.now(), LocalDate.now().plusYears(1)));
        prosessinstans.setData(lagProsessData_nySak());
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_JOURNALFOERINGSTYPE);
    }

    @Test
    public void utførSteg_nySakManglerSøknadsperiode_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(lagProsessData_nySak());
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_knyttManglerSaksnummer_tilInkommendeDok() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        lagProsessData_knytt();
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "");
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_knyttManglerBrukerID_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(BRUKER_ID.getKode());
        prosessinstans.setData(properties);
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_knyttManglerJournalpostID_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(JOURNALPOST_ID.getKode());
        prosessinstans.setData(properties);
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_knyttManglerTittel_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(HOVEDDOKUMENT_TITTEL.getKode());
        prosessinstans.setData(properties);
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_knyttManglerDokumentID_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(DOKUMENT_ID.getKode());
        prosessinstans.setData(properties);
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void utførSteg_knyttManglerSakbehandler_feiler() throws TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(SAKSBEHANDLER.getKode());
        prosessinstans.setData(properties);
        agent.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedUtenBehandlinger_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utførSteg(p);
        AssertionsForInterfaceTypes.assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        AssertionsForInterfaceTypes.assertThat(p.getHendelser()).isNotEmpty();
        AssertionsForClassTypes.assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som mangler en inaktiv behandling");
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedAktivBehandling_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utførSteg(p);
        AssertionsForInterfaceTypes.assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        AssertionsForInterfaceTypes.assertThat(p.getHendelser()).isNotEmpty();
        AssertionsForClassTypes.assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som har en aktiv behandling");
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedInaktivOgMedAktivBehandling_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING_OG_INAKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utførSteg(p);
        AssertionsForInterfaceTypes.assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        AssertionsForInterfaceTypes.assertThat(p.getHendelser()).isNotEmpty();
        AssertionsForClassTypes.assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Ulovlig behandlingstype. Du kan ikke ha ENDRET_PERIODE på en sak som har en aktiv behandling");
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedInaktivOgUtenAktivBehandling_steg_jfrOppdaterJournalpost() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utførSteg(p);
    }

    private static Properties lagProsessData_nySak() {
        Properties data = new Properties();
        data.setProperty(ProsessDataKey.OPPHOLDSLAND.getKode(), "DK");
        data.setProperty(ProsessDataKey.BRUKER_ID.getKode(), "aktørID");
        data.setProperty(ProsessDataKey.JOURNALPOST_ID.getKode(), "journalpostID");
        data.setProperty(ProsessDataKey.HOVEDDOKUMENT_TITTEL.getKode(), "tittel");
        data.setProperty(ProsessDataKey.DOKUMENT_ID.getKode(), "dokumentID");
        data.setProperty(ProsessDataKey.SAKSBEHANDLER.getKode(), "Z0099");
        return data;
    }

    private static Properties lagProsessData_knytt() {
        Properties data = new Properties();
        data.setProperty(ProsessDataKey.BRUKER_ID.getKode(), "aktørID");
        data.setProperty(ProsessDataKey.JOURNALPOST_ID.getKode(), "journalpostID");
        data.setProperty(ProsessDataKey.HOVEDDOKUMENT_TITTEL.getKode(), "tittel");
        data.setProperty(ProsessDataKey.DOKUMENT_ID.getKode(), "dokumentID");
        data.setProperty(ProsessDataKey.SAKSBEHANDLER.getKode(), "Z0099");
        data.setProperty(ProsessDataKey.SAKSNUMMER.getKode(), "MEL-1234");
        return data;
    }
}