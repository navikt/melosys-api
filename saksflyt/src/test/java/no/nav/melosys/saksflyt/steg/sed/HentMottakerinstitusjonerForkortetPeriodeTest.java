package no.nav.melosys.saksflyt.steg.sed;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HentMottakerinstitusjonerForkortetPeriodeTest {

    private HentMottakerinstitusjonerForkortetPeriode hentMottakerinstitusjonerForkortetPeriode;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private LandvelgerService landvelgerService;

    private final long behandlingId = 34;

    @BeforeEach
    public void setUp() {
        hentMottakerinstitusjonerForkortetPeriode = new HentMottakerinstitusjonerForkortetPeriode(behandlingsresultatService, eessiService, landvelgerService);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingId);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_harTidligereBUC_setterMottakerInstitusjoner() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(lagBehandling());
        Set<String> mottakerInstitusjoner = Set.of("SE:123");

        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), any(Collection.class))).thenReturn(true);
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(List.of(
            new BucInformasjon("123", true, BucType.LA_BUC_04.name(), LocalDate.now(), mottakerInstitusjoner, Collections.emptyList())
        ));

        hentMottakerinstitusjonerForkortetPeriode.utfør(p);

        assertThat(p.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<Set<String>>(){})).isEqualTo(mottakerInstitusjoner);
    }

    @Test
    void utfør_ikkeEessiReady_ingenMottakerInstitusjoner() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(lagBehandling());
        Set<String> mottakerInstitusjoner = Collections.emptySet();

        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), any(Collection.class))).thenReturn(true);
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(List.of(
            new BucInformasjon("123", true, BucType.LA_BUC_04.name(), LocalDate.now(), mottakerInstitusjoner, Collections.emptyList())
        ));

        hentMottakerinstitusjonerForkortetPeriode.utfør(p);

        assertThat(p.getData(ProsessDataKey.EESSI_MOTTAKERE, new TypeReference<Set<String>>(){})).isEqualTo(mottakerInstitusjoner);
    }

    @Test
    void utfør_erEessiReadyFinnerIngenBuc_kasterException() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(lagBehandling());

        when(eessiService.landErEessiReady(eq(BucType.LA_BUC_04.name()), any(Collection.class))).thenReturn(true);
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList())).thenReturn(Collections.emptyList());

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> hentMottakerinstitusjonerForkortetPeriode.utfør(p))
            .withMessageContaining("er EESSI-ready, men har ingen tidligere buc tilknyttet seg");
    }

    private Behandling lagBehandling() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setId(behandlingId);

        Fagsak fagsak = FagsakTestFactory.builder().medGsakSaksnummer().build();
        behandling.setFagsak(fagsak);
        return behandling;
    }
}
