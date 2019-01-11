package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDate;
import java.util.Properties;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.TekniskException;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static org.assertj.core.api.Assertions.assertThat;

public class GrunnleggendeValideringTest {

    private GrunnleggendeValidering agent;

    @Before
    public void setUp() {
        agent = new GrunnleggendeValidering();
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
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_INNKOMMENDE_DOKUMENT);
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