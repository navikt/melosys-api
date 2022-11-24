package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsaarsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtledMottaksdatoTest {
    private static final LocalDate MOTTAKSDATO = LocalDate.of(2022, 12, 1);
    private static final Instant REGISTRERT_DATO = Instant.parse("2021-12-01T12:00:00.00Z");
    private static final LocalDate REGISTRERT_DATO_LOCALDATE = LocalDate.ofInstant(REGISTRERT_DATO, ZoneId.systemDefault());
    private static final Instant FORSENDELSE_MOTTATT = Instant.parse("2020-12-01T12:00:00.00Z");
    private static final String JOURNALPOST_ID = "journalpostId";

    @Mock
    private JoarkFasade joarkFasade;

    private final FakeUnleash unleash = new FakeUnleash();

    private UtledMottaksdato utledMottaksdato;

    @BeforeEach
    void setUp() {
        utledMottaksdato = new UtledMottaksdato(joarkFasade, unleash);
    }

    @Test
    void getMottaksdato_toggleErAvOgMottatteOpplysningerHarMottaksdato_returnererMottaksdato() {
        unleash.disableAll();
        var behandling = new Behandling();
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottaksdato(MOTTAKSDATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(MOTTAKSDATO);
        verify(joarkFasade, never()).hentJournalpost(any());
    }

    @Test
    void getMottaksdato_toggleErAvOgBehandlingHarIkkeMottatteOpplysninger_returnererRegistrertDato() {
        unleash.disableAll();
        var behandling = new Behandling();
        behandling.setRegistrertDato(REGISTRERT_DATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(REGISTRERT_DATO_LOCALDATE);
        verify(joarkFasade, never()).hentJournalpost(any());
    }

    @Test
    void getMottaksdato_toggleErAvOgMottatteOpplysningerHarIkkeMottaksdato_returnererRegistrertDato() {
        unleash.disableAll();
        var behandling = new Behandling();
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottaksdato(null);
        behandling.setRegistrertDato(REGISTRERT_DATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(REGISTRERT_DATO_LOCALDATE);
        verify(joarkFasade, never()).hentJournalpost(any());
    }

    @Test
    void getMottaksdato_toggleErPåOgBehandlingsårsakFinnes_returnererMottaksdato() {
        unleash.enableAll();
        var behandling = new Behandling();
        behandling.setBehandlingsårsak(new Behandlingsaarsak());
        behandling.getBehandlingsårsak().setMottaksdato(MOTTAKSDATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(MOTTAKSDATO);
        verify(joarkFasade, never()).hentJournalpost(any());
    }

    @Test
    void getMottaksdato_toggleErPåBehandlingsårsakFinnesIkkeJournalpostHarDato_returnererForsendelseMottatt() {
        unleash.enableAll();
        var journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setForsendelseMottatt(FORSENDELSE_MOTTATT);
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);
        var behandling = new Behandling();
        behandling.setInitierendeJournalpostId(JOURNALPOST_ID);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(LocalDate.ofInstant(FORSENDELSE_MOTTATT, ZoneId.systemDefault()));
    }

    @Test
    void getMottaksdato_toggleErPåBehandlingsårsakFinnesIkkeJournalpostHarIkkeDato_returnererRegistrertDato() {
        unleash.enableAll();
        var journalpost = new Journalpost(JOURNALPOST_ID);
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);
        var behandling = new Behandling();
        behandling.setRegistrertDato(REGISTRERT_DATO);
        behandling.setInitierendeJournalpostId(JOURNALPOST_ID);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(REGISTRERT_DATO_LOCALDATE);
    }

    @Test
    void getMottaksdato_toggleErPåBehandlingsårsakFinnesIkkeHarIkkeInitierendeJournalpost_returnererRegistrertDato() {
        unleash.enableAll();
        var behandling = new Behandling();
        behandling.setRegistrertDato(REGISTRERT_DATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling);


        assertThat(utledetDato).isEqualTo(REGISTRERT_DATO_LOCALDATE);
        verify(joarkFasade, never()).hentJournalpost(any());
    }


    @Test
    void getMottaksdato_behandlingsårsakFinnes_returnererMottaksdato() {
        var journalpost = new Journalpost(JOURNALPOST_ID);
        var behandling = new Behandling();
        behandling.setBehandlingsårsak(new Behandlingsaarsak());
        behandling.getBehandlingsårsak().setMottaksdato(MOTTAKSDATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling, journalpost);


        assertThat(utledetDato).isEqualTo(MOTTAKSDATO);
    }

    @Test
    void getMottaksdato_behandlingsårsakFinnesIkkeJournalpostHarDato_returnererForsendelseMottatt() {
        var journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setForsendelseMottatt(FORSENDELSE_MOTTATT);
        var behandling = new Behandling();


        var utledetDato = utledMottaksdato.getMottaksdato(behandling, journalpost);


        assertThat(utledetDato).isEqualTo(LocalDate.ofInstant(FORSENDELSE_MOTTATT, ZoneId.systemDefault()));
    }

    @Test
    void getMottaksdato_behandlingsårsakFinnesIkkeJournalpostHarIkkeDato_returnererRegistrertDato() {
        var journalpost = new Journalpost(JOURNALPOST_ID);
        var behandling = new Behandling();
        behandling.setRegistrertDato(REGISTRERT_DATO);


        var utledetDato = utledMottaksdato.getMottaksdato(behandling, journalpost);


        assertThat(utledetDato).isEqualTo(REGISTRERT_DATO_LOCALDATE);
    }
}
