package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
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
    public void setUp() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        when(brevDataGrunnlag.getBehandling()).thenReturn(behandling);
        brevDataByggerUtpekingAnnetLand = new BrevDataByggerUtpekingAnnetLand(utpekingService, new BrevbestillingDto());
    }

    @Test
    void lag_medUtpekingPeriode_girBrevdata() throws FunksjonellException {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode(LocalDate.now(), null, Landkoder.CY,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null);
        when(utpekingService.hentUtpekingsperioder(eq(1L))).thenReturn(List.of(utpekingsperiode));
        final BrevData brevData = brevDataByggerUtpekingAnnetLand.lag(brevDataGrunnlag, "sb");
        assertThat(brevData).isInstanceOf(BrevDataUtpekingAnnetLand.class);
        assertThat(((BrevDataUtpekingAnnetLand)brevData).utpekingsperiode).isEqualTo(utpekingsperiode);
    }

    @Test
    void lag_utenUtpekingPeriode_kasterException() throws FunksjonellException {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevDataByggerUtpekingAnnetLand.lag(brevDataGrunnlag, "sb"))
            .withMessageContaining("uten utpekingsperiode");
    }
}
