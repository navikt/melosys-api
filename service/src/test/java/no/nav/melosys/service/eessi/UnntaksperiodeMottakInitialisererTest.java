package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kafka.model.Periode;
import no.nav.melosys.service.kafka.model.Statsborgerskap;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeMottakInitialisererTest {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock
    private FagsakService fagsakService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    
    private UnntaksperiodeMottakInitialiserer unntaksperiodeMottakInitialiserer;
    
    @Before
    public void setup() {
        unntaksperiodeMottakInitialiserer = new UnntaksperiodeMottakInitialiserer(fagsakService, lovvalgsperiodeService);
    }

    @Test
    public void initialiserProsessinstans_ikkeEndring_skalBehandlesVidere() {
        Prosessinstans prosessinstans = hentProsessinstans(false, LocalDate.now(), LocalDate.now().plusYears(1));

        unntaksperiodeMottakInitialiserer.initialiserProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
    }

    @Test
    public void initialiserProsessinstans_erEndringIkkeEndretPeriode_skalIkkeBehandles() throws Exception {
        
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusYears(1);
        Prosessinstans prosessinstans = hentProsessinstans(true, fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        when(lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(anyLong())).thenReturn(lovvalgsperiode);
        unntaksperiodeMottakInitialiserer.initialiserProsessinstans(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }

    @Test
    public void initialiserProsessinstans_erEndringFinnerIkkeFagsak_skalBehandles() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusYears(1);
        Prosessinstans prosessinstans = hentProsessinstans(true, fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.empty());
        unntaksperiodeMottakInitialiserer.initialiserProsessinstans(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
    }

    @Test
    public void initialiserProsessinstans_erEndringNyTomErNull_skalBehandles() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = null;
        Prosessinstans prosessinstans = hentProsessinstans(true, fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom.plusMonths(1));
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));

        unntaksperiodeMottakInitialiserer.initialiserProsessinstans(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
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
    
    private Prosessinstans hentProsessinstans(boolean erEndring, LocalDate fom, LocalDate tom) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(erEndring, fom, tom));
        return prosessinstans;
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

        Periode periode = new Periode();
        periode.setFom(dateTimeFormatter.format(fom));
        periode.setTom(tom != null ? dateTimeFormatter.format(tom) : null);
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");
        return melding;
    }
}