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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(anyLong())).thenReturn(lagMedlemAvFolketrygden(false));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> lagreMedlemsperiodeMedl.utfør(lagProsessInstans()))
            .withMessageContaining("Ingen medlemskapsperioder funnet for behandling");
    }

    @Test
    void utfør_erInnvilgelse() throws Exception {
        MedlemAvFolketrygden medlemAvFolketrygden = lagMedlemAvFolketrygden(true);
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(anyLong())).thenReturn(medlemAvFolketrygden);

        lagreMedlemsperiodeMedl.utfør(lagProsessInstans());

        verify(medlPeriodeService).opprettPeriodeEndeligFtrl(BEHANDLING_ID, medlemAvFolketrygden.getMedlemskapsperioder().iterator().next());
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

    private MedlemAvFolketrygden lagMedlemAvFolketrygden(boolean medPeriode) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(medPeriode ? lagMedlemskapsperiode() : emptyList());
        return medlemAvFolketrygden;
    }

    private Collection<Medlemskapsperiode> lagMedlemskapsperiode() {
        return List.of(new Medlemskapsperiode());
    }
}
