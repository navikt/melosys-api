package no.nav.melosys.service.persondata.familie;

import java.util.Collection;

import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.service.persondata.familie.medlem.EktefelleEllerPartnerFamiliemedlem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.persondata.familie.FamiliemedlemObjectFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EktefelleEllerPartnerFamiliemedlemTest {

    @Mock
    private PDLConsumer pdlConsumer;

    @InjectMocks
    private EktefelleEllerPartnerFamiliemedlem ektefelleEllerPartnerFamiliemedlem;

    @Test
    void test() {
        when(pdlConsumer.hentEktefelleEllerPartner(IDENT_PERSON_GIFT)).thenReturn(lagPersonGift());
        Collection<Sivilstand> sivilstandTilHovedperson = lagSivilstandForHovedperson();


        var result = ektefelleEllerPartnerFamiliemedlem.hentEktefelleEllerPartner(sivilstandTilHovedperson);


        assertThat(result).hasSize(1);
        var sivilstand = result.stream().findFirst().get();
        assertThat(sivilstand).matches(Familiemedlem::erRelatertVedSivilstand);
        assertThat(sivilstand.navn().fornavn()).isEqualTo(PERSON_GIFT_FORNAVN);
    }
}
