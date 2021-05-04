package no.nav.melosys.saksflyt.steg.medl;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LagreMedlemsperiodeMedlTest {

    public static final long BEHANDLING_ID = 123L;
    @Mock
    private MedlemAvFolketrygdenService medlemAvFolketrygdenService;

    @Mock
    private MedlPeriodeService medlPeriodeService;

    private LagreMedlemsperiodeMedl lagreMedlemsperiodeMedl;

    @BeforeEach
    void init() {
        lagreMedlemsperiodeMedl = new LagreMedlemsperiodeMedl(medlemAvFolketrygdenService, medlPeriodeService);
    }

    @Test
    void utfør_feilerUtenMedlemskapsperiode() throws Exception {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(anyLong())).thenReturn(lagMedlemAvFolketrygden());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> lagreMedlemsperiodeMedl.utfør(lagProsessInstans()))
            .withMessageContaining("Ingen medlemskapsperioder funnet for behandling");
    }

    @Test
    void utfør_erInnvilgelse_oppretterMedlPerioder() throws Exception {
        MedlemAvFolketrygden medlemAvFolketrygden = lagMedlemAvFolketrygden(new Medlemskapsperiode(), new Medlemskapsperiode());
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(anyLong())).thenReturn(medlemAvFolketrygden);

        lagreMedlemsperiodeMedl.utfør(lagProsessInstans());

        verify(medlPeriodeService, times(2)).opprettPeriodeEndelig(BEHANDLING_ID, medlemAvFolketrygden.getMedlemskapsperioder().iterator().next());
    }

    @Test
    void utfør_erInnvilgelse_opprettPerioder_Idempotent() throws Exception {
        Medlemskapsperiode lagretPeriode = new Medlemskapsperiode();
        lagretPeriode.setMedlPeriodeID(123L);
        MedlemAvFolketrygden medlemAvFolketrygden = lagMedlemAvFolketrygden(lagretPeriode, new Medlemskapsperiode());

        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(anyLong())).thenReturn(medlemAvFolketrygden);

        lagreMedlemsperiodeMedl.utfør(lagProsessInstans());

        verify(medlPeriodeService, times(1)).opprettPeriodeEndelig(eq(BEHANDLING_ID), any(Medlemskapsperiode.class));
    }

    private Prosessinstans lagProsessInstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(lagBehandling());
        return prosessinstans;
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        return behandling;
    }

    private MedlemAvFolketrygden lagMedlemAvFolketrygden(Medlemskapsperiode... medlemskapsperioder) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(medlemskapsperioder.length > 0 ? List.of(medlemskapsperioder) : emptyList());
        return medlemAvFolketrygden;
    }
}
