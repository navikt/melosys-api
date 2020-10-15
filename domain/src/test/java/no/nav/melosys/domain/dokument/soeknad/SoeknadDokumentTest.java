package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadDokument;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SoeknadDokumentTest {

    @Test
    public void hentAllePersonnumre() {
        String personnummer1 = "12345678910";
        String personnummer2 = "10987654321";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.personOpplysninger.medfolgendeAndre = personnummer1;
        soeknadDokument.personOpplysninger.medfolgendeFamilie = Collections.singletonList(personnummer2);

        Set<String> personnumre = soeknadDokument.hentAllePersonnumre();
        assertThat(personnumre.size()).isEqualTo(2);
        assertThat(personnumre.containsAll(Arrays.asList(personnummer1, personnummer2)));
    }

    @Test
    public void hentAllePersonnumreKunFamilie() {
        String personnummer1 = "12345678910";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.personOpplysninger.medfolgendeFamilie = Collections.singletonList(personnummer1);

        Set<String> personnumre = soeknadDokument.hentAllePersonnumre();
        assertThat(personnumre.size()).isEqualTo(1);
        assertThat(personnumre.contains(personnummer1));
    }

    @Test
    public void hentAlleOrganisasjonsnumre() {
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = "12345678910";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.selvstendigArbeid.selvstendigForetak = Collections.singletonList(selvstendigForetak);

        String orgNr2 = "10987654321";
        soeknadDokument.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add("10987654321");

        Set<String> organisasjonsnumre = soeknadDokument.hentAlleOrganisasjonsnumre();
        assertThat(organisasjonsnumre.size()).isEqualTo(2);
        assertThat(organisasjonsnumre.contains(selvstendigForetak.orgnr));
        assertThat(organisasjonsnumre.contains(orgNr2));
    }
}