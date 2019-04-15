package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingTest {

    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private OppdaterMedlFelles oppdaterMedlFelles;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;


    private OppdaterBehandling oppdaterBehandling;

    @Before
    public void setUp() {
        oppdaterBehandling = new OppdaterBehandling(lovvalgsperiodeService, oppdaterMedlFelles,medlFasade, saksopplysningRepository);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setPeriode(new Periode(LocalDate.now(), LocalDate.now()));

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(saksopplysning));
    }

    @Test
    public void utførSteg_verifiserLagreLovvalgspeirode() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, true);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "12312322");

        oppdaterBehandling.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_PERIODE);
    }
}