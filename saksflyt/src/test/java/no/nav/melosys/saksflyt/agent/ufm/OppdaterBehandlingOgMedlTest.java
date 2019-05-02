package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingOgMedlTest {

    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private OppdaterBehandlingOgMedl oppdaterBehandlingOgMedl;

    @Before
    public void setUp() throws Exception {
        oppdaterBehandlingOgMedl = new OppdaterBehandlingOgMedl(lovvalgsperiodeService, oppdaterMedlFelles,medlFasade, saksopplysningRepository);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setPeriode(new Periode(LocalDate.now(), LocalDate.now()));

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(saksopplysning));
        when(lovvalgsperiodeService.lagreLovvalgsperioder(anyLong(),anyCollection()))
            .thenReturn(Collections.singletonList(new Lovvalgsperiode()));
        when(oppdaterMedlFelles.hentLovvalgsperiode(any(Behandling.class))).thenReturn(new Lovvalgsperiode());
    }

    @Test
    public void utførSteg_ikkeEndring_verifiserLagreLovvalgspeirode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, false);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");

        oppdaterBehandlingOgMedl.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_PERIODE);
    }

    @Test
    public void utførSteg_erEndring_verifiserAvsluttTidligereMedlPeriode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(hentFagsak());

        Prosessinstans prosessinstans = hentProsessinstans(behandling, true);
        oppdaterBehandlingOgMedl.utfør(prosessinstans);
        verify(medlFasade).avvisPeriode(any(Lovvalgsperiode.class), any(StatusaarsakMedl.class));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_PERIODE);
    }

    private Fagsak hentFagsak() {
        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        return fagsak;
    }

    private Prosessinstans hentProsessinstans(Behandling behandling, boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, erEndring);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");
        return prosessinstans;
    }
}