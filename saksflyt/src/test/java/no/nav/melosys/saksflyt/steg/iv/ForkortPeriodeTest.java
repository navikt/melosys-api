package no.nav.melosys.saksflyt.steg.iv;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_VALIDERING;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ForkortPeriodeTest {

    private ForkortPeriode forkortPeriode;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;

    @Before
    public void setUp() throws MelosysException {
        avklartefaktaService = mock(AvklartefaktaService.class);
        forkortPeriode = new ForkortPeriode(avklartefaktaService, behandlingsresultatService, eessiService);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(34L);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(eessiService.hentMottakerinstitusjonFraBuc(any(Fagsak.class), any(BucType.class))).thenReturn("SE:123");
    }

    @Test
    public void utfør() throws MelosysException {
        long behandlingId = 34L;
        Endretperiode endretperiodeKode = Endretperiode.ARBEIDSFORHOLD_AVSLUTTET;

        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(behandlingId);
        p.setBehandling(behandling);
        p.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiodeKode);

        forkortPeriode.utfør(p);

        verify(avklartefaktaService).leggTilBegrunnelse(behandlingId, Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiodeKode.getKode());
        assertThat(p.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<List<String>>() {})).containsExactly("SE:123");
        assertThat(p.getSteg()).isEqualTo(IV_VALIDERING);
    }
}