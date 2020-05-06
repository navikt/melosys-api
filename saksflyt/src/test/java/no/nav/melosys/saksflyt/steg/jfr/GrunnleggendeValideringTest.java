package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
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

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GrunnleggendeValideringTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private FagsakService fagsakService;

    private GrunnleggendeValidering grunnleggendeValidering;
    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_AKTIV_BEHANDLING = "MELTEST-2";
    private final static String SAKSNUMMER_MED_AKTIV_BEHANDLING_OG_INAKTIV_BEHANDLING = "MELTEST-3";
    private final static String SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING = "MELTEST-4";

    @Before
    public void setUp() throws IkkeFunnetException {
        grunnleggendeValidering = new GrunnleggendeValidering(fagsakService);

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

        when(fagsakService.hentFagsak(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsakUtenBehandlinger);
        when(fagsakService.hentFagsak(SAKSNUMMER_MED_AKTIV_BEHANDLING)).thenReturn(fagsakMedAktivBehandling);
        when(fagsakService.hentFagsak(SAKSNUMMER_MED_AKTIV_BEHANDLING_OG_INAKTIV_BEHANDLING)).thenReturn(fagsakMedInaktivOgAktivBehandling);
        when(fagsakService.hentFagsak(SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING)).thenReturn(fagsakMedInaktivBehandling);
    }

    @Test
    public void utførSteg_ukjentProsess_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("ProsessType");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_nySak_tilInkommendeDok() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(LocalDate.now(), LocalDate.now().plusYears(1)));
        prosessinstans.setData(BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        prosessinstans.setData(BEHANDLINGSTEMA, Behandlingstema.UTSENDT_ARBEIDSTAKER);
        prosessinstans.setData(lagProsessData_nySak());
        grunnleggendeValidering.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_JOURNALFOERINGSTYPE);
    }

    @Test
    public void utførSteg_nySak_feilBehandlingstype() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(LocalDate.now(), LocalDate.now().plusYears(1)));
        prosessinstans.setData(BEHANDLINGSTEMA, Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        prosessinstans.setData(lagProsessData_nySak());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("gjelder ikke en søknad!");
        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_nySakManglerSøknadsperiode_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(lagProsessData_nySak());

        expectedException.expect(FunksjonellException.class);
        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_nySakSøknadsperiodeMedFomEtterTom_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        prosessinstans.setData(lagProsessData_nySak());
        prosessinstans.setData(SØKNADSPERIODE, new Periode(LocalDate.now(), LocalDate.now().minusYears(1)));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("etter");
        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_knyttManglerSaksnummer_tilInkommendeDok() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        lagProsessData_knytt();
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "");

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Det finnes ingen fagsak med saksnummer");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_knyttManglerBrukerID_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(BRUKER_ID.getKode());
        prosessinstans.setData(properties);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Bruker id er ikke oppgitt");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_knyttManglerJournalpostID_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(JOURNALPOST_ID.getKode());
        prosessinstans.setData(properties);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("journalpostID");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_knyttManglerTittel_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(HOVEDDOKUMENT_TITTEL.getKode());
        prosessinstans.setData(properties);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Mangler hoveddokument tittel");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_knyttManglerDokumentID_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(DOKUMENT_ID.getKode());
        prosessinstans.setData(properties);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("dokumentID");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void utførSteg_knyttManglerSakbehandler_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_KNYTT);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);
        Properties properties = lagProsessData_knytt();
        properties.remove(SAKSBEHANDLER.getKode());
        prosessinstans.setData(properties);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("saksbehandler");

        grunnleggendeValidering.utfør(prosessinstans);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedUtenBehandlinger_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("mangler en inaktiv behandling");

        grunnleggendeValidering.utfør(p);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedAktivBehandling_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("har en aktiv behandling");

        grunnleggendeValidering.utfør(p);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedInaktivOgMedAktivBehandling_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING_OG_INAKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("har en aktiv behandling");

        grunnleggendeValidering.utfør(p);
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
        data.setProperty(ProsessDataKey.SAKSNUMMER.getKode(), SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING);
        return data;
    }
}