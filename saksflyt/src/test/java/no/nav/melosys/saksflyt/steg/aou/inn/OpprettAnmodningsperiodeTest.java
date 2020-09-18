package no.nav.melosys.saksflyt.steg.aou.inn;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.felles.OpprettSedDokumentFelles;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettAnmodningsperiodeTest {

    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private OpprettSedDokumentFelles opprettSedDokumentFelles;
    @Mock
    private BehandlingService behandlingService;

    private OpprettAnmodningsperiode opprettAnmodningsperiode;

    @Before
    public void setup() {
        opprettAnmodningsperiode = new OpprettAnmodningsperiode(anmodningsperiodeService, opprettSedDokumentFelles, behandlingService);
    }

    @Test
    public void utfør_medEksisterendeSedDokument_forventKall() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(lagSedDokument());
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        prosessinstans.setBehandling(behandling);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        opprettAnmodningsperiode.utfør(prosessinstans);

        verify(behandlingService).hentBehandling(anyLong());
        verify(anmodningsperiodeService).lagreAnmodningsperioder(eq(1L), any());
    }

    @Test
    public void utfør_utenEksisterendeSedDokument_forventOpprettSedDokument() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, new MelosysEessiMelding());

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(lagSedDokument());
        when(opprettSedDokumentFelles.opprettSedSaksopplysning(any(MelosysEessiMelding.class), any(Behandling.class))).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        opprettAnmodningsperiode.utfør(prosessinstans);

        verify(behandlingService).hentBehandling(anyLong());
        verify(opprettSedDokumentFelles).opprettSedSaksopplysning(any(MelosysEessiMelding.class), any(Behandling.class));
        verify(anmodningsperiodeService).lagreAnmodningsperioder(eq(1L), any());
    }

    @Test(expected = FunksjonellException.class)
    public void utfør_ingenBehandling_forventException() throws FunksjonellException, TekniskException {
        opprettAnmodningsperiode.utfør(new Prosessinstans());
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