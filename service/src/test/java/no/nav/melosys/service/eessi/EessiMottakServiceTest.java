package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import no.nav.melosys.eessi.avro.Periode;
import no.nav.melosys.eessi.avro.Statsborgerskap;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EessiMottakServiceTest {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Captor
    private ArgumentCaptor<Prosessinstans> captor;

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private EessiMottakService eessiMottakService;

    @Before
    public void setUp() {
        eessiMottakService = new EessiMottakService(prosessinstansService, fagsakService, lovvalgsperiodeService);
    }

    @Test
    public void behandleMottatMelding_ikkeEndring_sjekkAlleVerdierErSatt() {
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding(false, LocalDate.now(), LocalDate.now().plusYears(1));
        eessiMottakService.behandleMottattMelding(eessiMelding);

        verify(prosessinstansService).lagre(captor.capture());

        Prosessinstans prosessinstans = captor.getValue();
        assertThat(prosessinstans).isNotNull();
        assertThat(prosessinstans.getData()).isNotEmpty();

        SedDokument sedDokument = prosessinstans.getData(ProsessDataKey.SED_DOKUMENT, SedDokument.class);
        assertThat(sedDokument).isNotNull();
        assertThat(sedDokument.getLovvalgBestemmelse()).isEqualTo(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        assertThat(sedDokument.getPeriode()).isNotNull();
        assertThat(sedDokument.getPeriode().getFom()).isBeforeOrEqualTo(LocalDate.of(2020, 12, 12));
        assertThat(prosessinstans.getData(ProsessDataKey.AKTØR_ID)).isNotNull();
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isNotNull();
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID)).isNotNull();
    }

    @Test
    public void behandleMottatMelding_erEndringIkkeEndretPeriode_skalIkkeBehandles() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusYears(1);
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding(true, fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        when(lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(anyLong()))
            .thenReturn(lovvalgsperiode);
        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong()))
            .thenReturn(Optional.of(hentFagsak()));

        eessiMottakService.behandleMottattMelding(eessiMelding);

        verify(prosessinstansService, never()).lagre(any(Prosessinstans.class));
    }

    @Test
    public void behandleMottatMelding_erEndringFinnerIkkeTidligerBehandling_skalBehandles() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusYears(1);
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding(true, fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong()))
            .thenReturn(Optional.empty());
        eessiMottakService.behandleMottattMelding(eessiMelding);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
    }

    @Test
    public void behandleMottatMelding_erEndringTomErNull_skalBehandles() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = null;
        MelosysEessiMelding eessiMelding = hentMelosysEessiMelding(true, fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom.plusMonths(1));
        lovvalgsperiode.setTom(tom);

        when(lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(anyLong()))
            .thenReturn(lovvalgsperiode);
        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong()))
            .thenReturn(Optional.of(hentFagsak()));
        eessiMottakService.behandleMottattMelding(eessiMelding);

        verify(prosessinstansService).lagre(any(Prosessinstans.class));
    }

    private Fagsak hentFagsak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setRegistrertDato(Instant.now());

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }

    private MelosysEessiMelding hentMelosysEessiMelding(boolean erEndring, LocalDate fom, LocalDate tom) {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId("123");
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setErEndring(erEndring);
        melding.setGsakSaksnummer(432432L);
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");
        melding.setPeriode(
            Periode.newBuilder()
                .setFom(dateTimeFormatter.format(fom))
                .setTom(tom != null ? dateTimeFormatter.format(tom) : null)
                .build()
        );
        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(Statsborgerskap.newBuilder().setLandkode("SE").build()));
        return melding;
    }
}