package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettAnmodningsperiodeFraSedTest {

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;

    private OpprettAnmodningsperiodeFraSed opprettAnmodningsperiodeFraSed;

    @BeforeEach
    public void setup() {
        opprettAnmodningsperiodeFraSed = new OpprettAnmodningsperiodeFraSed(anmodningsperiodeService, behandlingService);
    }

    @Test
    void utfør_medEksisterendeSedDokument_forventKall() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(lagSedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        opprettAnmodningsperiodeFraSed.utfør(prosessinstans);

        verify(behandlingService).hentBehandling(anyLong());
        verify(anmodningsperiodeService).lagreAnmodningsperioder(eq(1L), any());
    }

    private SedDokument lagSedDokument() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusYears(1)));
        sedDokument.setLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        sedDokument.setLovvalgslandKode(Landkoder.DE);
        sedDokument.setUnntakFraLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setUnntakFraLovvalgslandKode(Landkoder.NO);

        return sedDokument;
    }
}