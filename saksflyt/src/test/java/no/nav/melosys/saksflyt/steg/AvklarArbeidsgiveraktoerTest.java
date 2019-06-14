package no.nav.melosys.saksflyt.steg;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Yrkesaktivitetstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.iv.AvklarArbeidsgiver;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarArbeidsgiveraktoerTest {

    @Mock
    AktoerRepository aktoerRepository;

    @Mock
    BehandlingRepository behandlingRepository;

    @Mock
    Behandling behandling;

    @Mock
    AvklarteVirksomheterSystemService avklarteVirksomheterService;

    private AvklarArbeidsgiver steg;

    private Prosessinstans p;

    AvklartVirksomhet avklartVirksomhet;
    Fagsak fagsak;

    @Before
    public void setUp() {

        AktoerService aktoerService = new AktoerService(aktoerRepository);
        steg = new AvklarArbeidsgiver(aktoerService, avklarteVirksomheterService, behandlingRepository);

        p = new Prosessinstans();
        p.setBehandling(behandling);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);

        avklartVirksomhet =
            new AvklartVirksomhet("Test", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
    }

    @Test
    public void utfør_medAvklartNorskVirksomhet_arbeidsgiveraktørOpprettes() throws FunksjonellException, TekniskException {
        List<AvklartVirksomhet> avklarteVirksomheter = Collections.singletonList(avklartVirksomhet);
        when(avklarteVirksomheterService.hentArbeidsgivere(any(), any())).thenReturn(avklarteVirksomheter);

        steg.utfør(p);

        verify(aktoerRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));

        Aktoer aktoer = new Aktoer();
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktoer.setOrgnr("123456789");
        verify(aktoerRepository).save(eq(aktoer));
    }

    @Test
    public void utfør_utenAvklartNorskVirksomhet_arbeidsgiveraktorerSlettes() throws FunksjonellException, TekniskException {
        steg.utfør(p);

        verify(aktoerRepository).deleteAllByFagsakAndRolle(eq(fagsak), eq(Aktoersroller.ARBEIDSGIVER));
        verify(aktoerRepository, times(0)).save(any());
    }

    @Test
    public void utfør_iverksettVedtakSjekkSteg_forventIvOppdaterMedl() throws Exception {
        when(avklarteVirksomheterService.hentArbeidsgivere(any(), any())).thenReturn(Collections.singletonList(avklartVirksomhet));
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_OPPDATER_MEDL);
    }
}