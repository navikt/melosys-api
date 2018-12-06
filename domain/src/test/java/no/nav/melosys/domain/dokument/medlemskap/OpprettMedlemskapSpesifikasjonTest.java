package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatType;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OpprettMedlemskapSpesifikasjonTest {

    @Test
    public void erPeriodeEndelig() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(BehandlingsresultatType.FASTSATT_LOVVALGSLAND);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        assertThat(OpprettMedlemskapSpesifikasjon.erPeriodeEndelig(behandlingsresultat, lovvalgsperiode)).isTrue();
    }

    @Test
    public void erPeriodeUnderAvklaring() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        assertThat(OpprettMedlemskapSpesifikasjon.erPeriodeUnderAvklaring(behandlingsresultat)).isTrue();
    }

}