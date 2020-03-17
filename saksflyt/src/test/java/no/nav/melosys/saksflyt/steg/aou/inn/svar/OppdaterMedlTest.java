package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
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
public class OppdaterMedlTest {

    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private BehandlingService behandlingService;

    private OppdaterMedl oppdaterMedl;

    @Before
    public void setup() throws IkkeFunnetException {
        oppdaterMedl = new OppdaterMedl(medlPeriodeService, medlFasade, behandlingService);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
    }

    @Test
    public void utfør_lovvalgsperiodeInnvilget() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        when(medlPeriodeService.hentLovvalgsperiode(any(Behandling.class))).thenReturn(lovvalgsperiode);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(lagBehandling());
        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).hentLovvalgsperiode(any(Behandling.class));
        verify(medlFasade).oppdaterPeriodeEndelig(any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_SEND_SED);
    }

    @Test
    public void utfør_lovvalgsperiodeAvvist() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        lovvalgsperiode.setMedlPeriodeID(1L);
        when(medlPeriodeService.hentLovvalgsperiode(any(Behandling.class))).thenReturn(lovvalgsperiode);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(lagBehandling());
        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).hentLovvalgsperiode(any(Behandling.class));
        verify(medlFasade).avvisPeriode(anyLong(), eq(StatusaarsakMedl.AVVIST));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_SEND_SED);
    }

    @Test
    public void utfør_skalSendeBrev() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        lovvalgsperiode.setMedlPeriodeID(1L);
        when(medlPeriodeService.hentLovvalgsperiode(any(Behandling.class))).thenReturn(lovvalgsperiode);

        Behandling behandling = lagBehandling();
        ((SedDokument) behandling.getSaksopplysninger().iterator().next().getDokument()).setErElektronisk(false);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).hentLovvalgsperiode(any(Behandling.class));
        verify(medlFasade).avvisPeriode(anyLong(), eq(StatusaarsakMedl.AVVIST));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST);
    }

    private Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        behandling.setSaksopplysninger(lagSaksopplysninger());

        return behandling;
    }

    private Set<Saksopplysning> lagSaksopplysninger() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);

        SedDokument sedDokument = new SedDokument();
        saksopplysning.setDokument(sedDokument);

        return Collections.singleton(saksopplysning);
    }
}