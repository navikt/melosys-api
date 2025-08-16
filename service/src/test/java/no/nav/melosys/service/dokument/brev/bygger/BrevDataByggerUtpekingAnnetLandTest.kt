package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerUtpekingAnnetLandTest {
    @Mock
    UtpekingService utpekingService;
    @Mock
    BrevDataGrunnlag brevDataGrunnlag;

    private BrevDataByggerUtpekingAnnetLand brevDataByggerUtpekingAnnetLand;

    @BeforeEach
    public void setUp() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .build();
        when(brevDataGrunnlag.getBehandling()).thenReturn(behandling);
        brevDataByggerUtpekingAnnetLand = new BrevDataByggerUtpekingAnnetLand(utpekingService, new BrevbestillingDto());
    }

    @Test
    void lag_medUtpekingPeriode_girBrevdata() {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode(LocalDate.now(), null, Land_iso2.CY,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null);
        when(utpekingService.hentUtpekingsperioder(eq(1L))).thenReturn(List.of(utpekingsperiode));
        final BrevData brevData = brevDataByggerUtpekingAnnetLand.lag(brevDataGrunnlag, "sb");
        assertThat(brevData).isInstanceOf(BrevDataUtpekingAnnetLand.class);
        assertThat(((BrevDataUtpekingAnnetLand) brevData).getUtpekingsperiode()).isEqualTo(utpekingsperiode);
    }

    @Test
    void lag_utenUtpekingPeriode_kasterException() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevDataByggerUtpekingAnnetLand.lag(brevDataGrunnlag, "sb"))
            .withMessageContaining("uten utpekingsperiode");
    }
}
