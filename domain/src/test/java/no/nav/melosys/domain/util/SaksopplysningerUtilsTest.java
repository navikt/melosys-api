package no.nav.melosys.domain.util;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class SaksopplysningerUtilsTest {

    @Test
    public void hentDokument() {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        Saksopplysning saksopplysning_1 = new Saksopplysning();
        saksopplysning_1.setType(SaksopplysningType.ARBEIDSFORHOLD);
        saksopplysninger.add(saksopplysning_1);
        Saksopplysning saksopplysning_2 = new Saksopplysning();
        saksopplysning_2.setType(SaksopplysningType.SØKNAD);
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        saksopplysning_2.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysning_2);
        Saksopplysning saksopplysning_3 = new Saksopplysning();
        saksopplysning_3.setType(SaksopplysningType.MEDLEMSKAP);
        saksopplysninger.add(saksopplysning_3);

        behandling.setSaksopplysninger(saksopplysninger);

        Optional<SaksopplysningDokument> saksopplysningDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.SØKNAD);
        assertThat(saksopplysningDokument).isNotEmpty();
        assertThat(saksopplysningDokument.get()).isEqualTo(soeknadDokument);
    }
}