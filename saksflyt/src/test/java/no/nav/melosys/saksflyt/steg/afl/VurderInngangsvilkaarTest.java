package no.nav.melosys.saksflyt.steg.afl;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
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
public class VurderInngangsvilkaarTest {
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private FagsakService fagsakService;

    private VurderInngangsvilkaar vurderInngangsvilkaar;

    @Before
    public void setup() {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, fagsakService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        p.setBehandling(behandling);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setPeriode(new Periode());
        melosysEessiMelding.getPeriode().setFom(LocalDate.now());
        melosysEessiMelding.getPeriode().setTom(LocalDate.now().plusYears(1));
        melosysEessiMelding.setLovvalgsland("IDK");
        p.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(eq(behandling.getId()), any(), any())).thenReturn(true);

        vurderInngangsvilkaar.utfør(p);

        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(anyLong(), any(), any());
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AFL_REGISTERKONTROLL);
    }
}