package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.behandlingsgrunnlag.data.SelvstendigForetak;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SoeknadTest {
    @Test
    public void hentAlleOrganisasjonsnumre() {
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = "12345678910";

        Soeknad soeknad = new Soeknad();
        soeknad.selvstendigArbeid.selvstendigForetak = Collections.singletonList(selvstendigForetak);

        String orgNr2 = "10987654321";
        soeknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add("10987654321");

        Set<String> organisasjonsnumre = soeknad.hentAlleOrganisasjonsnumre();
        assertThat(organisasjonsnumre.size()).isEqualTo(2);
        assertThat(organisasjonsnumre).contains(selvstendigForetak.orgnr)
            .contains(orgNr2);
    }
}
