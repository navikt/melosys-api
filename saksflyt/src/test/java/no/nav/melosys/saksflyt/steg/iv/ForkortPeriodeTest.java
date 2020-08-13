package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
    @Mock
    private LandvelgerService landvelgerService;

    private final long behandlingId = 34;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws MelosysException {
        avklartefaktaService = mock(AvklartefaktaService.class);
        forkortPeriode = new ForkortPeriode(avklartefaktaService, behandlingsresultatService, eessiService, landvelgerService);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingId);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utfør_harTidligereBUC_setterMottakerInstitusjoner() throws MelosysException {
        Endretperiode endretperiodeKode = Endretperiode.ARBEIDSFORHOLD_AVSLUTTET;

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(lagBehandling());
        p.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiodeKode);

        Set<String> mottakerInstitusjoner = Set.of("SE:123");

        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), any(Collection.class))).thenReturn(true);
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(List.of(
            new BucInformasjon("123", BucType.LA_BUC_04.name(), LocalDate.now(), mottakerInstitusjoner, Collections.emptyList())
        ));

        forkortPeriode.utfør(p);

        verify(avklartefaktaService).leggTilBegrunnelse(behandlingId, Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiodeKode.getKode());
        assertThat(p.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<Set<String>>(){})).isEqualTo(mottakerInstitusjoner);
        assertThat(p.getSteg()).isEqualTo(IV_VALIDERING);
    }

    @Test
    public void utfør_ikkeEessiReady_ingenMottakerInstitusjoner() throws MelosysException {
        Endretperiode endretperiodeKode = Endretperiode.ARBEIDSFORHOLD_AVSLUTTET;

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(lagBehandling());
        p.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiodeKode);

        Set<String> mottakerInstitusjoner = Collections.emptySet();

        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), any(Collection.class))).thenReturn(true);
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(List.of(
            new BucInformasjon("123", BucType.LA_BUC_04.name(), LocalDate.now(), mottakerInstitusjoner, Collections.emptyList())
        ));

        forkortPeriode.utfør(p);

        verify(avklartefaktaService).leggTilBegrunnelse(behandlingId, Avklartefaktatyper.AARSAK_ENDRING_PERIODE, endretperiodeKode.getKode());
        assertThat(p.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<Set<String>>(){})).isEqualTo(mottakerInstitusjoner);
        assertThat(p.getSteg()).isEqualTo(IV_VALIDERING);
    }

    @Test
    public void utfør_erEessiReadyFinnerIngenBuc_kasterException() throws MelosysException {
        Endretperiode endretperiodeKode = Endretperiode.ARBEIDSFORHOLD_AVSLUTTET;

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(lagBehandling());
        p.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiodeKode);

        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), any(Collection.class))).thenReturn(true);
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(Collections.emptyList());

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("er EESSI-ready, men har ingen tidligere buc tilknyttet seg");
        forkortPeriode.utfør(p);
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingId);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        behandling.setFagsak(fagsak);
        return behandling;
    }
}