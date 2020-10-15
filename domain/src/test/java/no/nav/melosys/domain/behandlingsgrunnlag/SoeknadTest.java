package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SoeknadTest {
    @Test
    public void hentAllePersonnumre() {
        String personnummer1 = "12345678910";
        String personnummer2 = "10987654321";

        Soeknad soeknad = new Soeknad();
        soeknad.personOpplysninger.medfolgendeAndre = personnummer1;
        soeknad.personOpplysninger.medfolgendeFamilie = Collections.singletonList(personnummer2);

        Set<String> personnumre = soeknad.hentAllePersonnumre();
        assertThat(personnumre.size()).isEqualTo(2);
        assertThat(personnumre).containsAll(Arrays.asList(personnummer1, personnummer2));
    }

    @Test
    public void hentAllePersonnumreKunFamilie() {
        String personnummer1 = "12345678910";

        Soeknad soeknad = new Soeknad();
        soeknad.personOpplysninger.medfolgendeFamilie = Collections.singletonList(personnummer1);

        Set<String> personnumre = soeknad.hentAllePersonnumre();
        assertThat(personnumre.size()).isEqualTo(1);
        assertThat(personnumre).contains(personnummer1);
    }

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