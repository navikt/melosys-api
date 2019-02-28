package no.nav.melosys.service.dokument;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvklarteVirksomheterTest {

    AvklarteVirksomheter avklarteVirksomheter;
    SoeknadDokument søknad;

    String avklartOrgnr = "123456789";

    @Before
    public void setUp() throws TekniskException {
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);
        when(avklartefaktaService.hentAvklarteOrganisasjoner(anyLong())).thenReturn(new HashSet<>(Arrays.asList(avklartOrgnr)));

        søknad = new SoeknadDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(søknad);
        saksopplysning.setType(SaksopplysningType.SØKNAD);

        Behandling behandling = mock(Behandling.class);
        when(behandling.getSaksopplysninger()).thenReturn(Collections.singleton(saksopplysning));

        avklarteVirksomheter = new AvklarteVirksomheter(avklartefaktaService, registerOppslagService, behandling);
    }

    @Test
    public void hentAvklarteSelvstendigeForetakOrgnumre_girListeMedKunAvklarteOrgnumre() throws TekniskException {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = avklartOrgnr;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        SelvstendigForetak foretak1 = new SelvstendigForetak();
        foretak1.orgnr = "10987654321";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak1);

        Set<String> avklarteSelvstendigeOrgnumre = avklarteVirksomheter.hentAvklarteSelvstendigeForetakOrgnumre();
        assertThat(avklarteSelvstendigeOrgnumre).containsOnly(avklartOrgnr);
    }
}
