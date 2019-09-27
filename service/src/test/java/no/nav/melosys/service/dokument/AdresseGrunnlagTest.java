package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.Bosted;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AdresseGrunnlagTest {
    private SoeknadDokument soeknadDokument = new SoeknadDokument();
    private PersonDokument personDokument = new PersonDokument();
    private AdresseGrunnlag adresseGrunnlag;

    @Before
    public void setup() {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        adresseGrunnlag = new AdresseGrunnlag(soeknadDokument, personDokument, kodeverkService);
    }

    @Test
    public void hentAdresseHvisFinnes_harBostedsadresse_forventBostedsadresse() throws TekniskException {
        soeknadDokument.bosted = new Bosted();
        soeknadDokument.bosted.oppgittAdresse = new StrukturertAdresse();
        soeknadDokument.bosted.oppgittAdresse.landkode = "SE";
        soeknadDokument.bosted.oppgittAdresse.gatenavn = "gate";

        StrukturertAdresse strukturertAdresse = adresseGrunnlag.hentAdresseHvisFinnes();

        assertThat(strukturertAdresse.gatenavn).isEqualTo("gate");
        assertThat(strukturertAdresse.landkode).isEqualTo("SE");
    }

    @Test
    public void hentAdresseHvisFinnes_harBostedsadresseIRegister_forventBostedsadresse() throws TekniskException {
        personDokument.bostedsadresse = new no.nav.melosys.domain.dokument.person.Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land("SWE"));
        personDokument.bostedsadresse.setGateadresse(new Gateadresse());
        personDokument.bostedsadresse.getGateadresse().setGatenavn("gate");

        StrukturertAdresse strukturertAdresse = adresseGrunnlag.hentAdresseHvisFinnes();

        assertThat(strukturertAdresse.gatenavn).isEqualTo("gate");
        assertThat(strukturertAdresse.landkode).isEqualTo("SE");
    }

    @Test
    public void hentAdresseHvisFinnes_harPostadresse_forventPostadresse() throws TekniskException {
        personDokument.postadresse = new UstrukturertAdresse();
        personDokument.postadresse.land = new Land("SWE");
        personDokument.postadresse.adresselinje1 = "gate";

        StrukturertAdresse strukturertAdresse = adresseGrunnlag.hentAdresseHvisFinnes();

        assertThat(strukturertAdresse.gatenavn).isEqualTo("gate");
        assertThat(strukturertAdresse.landkode).isEqualTo("SE");
    }

    @Test(expected = TekniskException.class)
    public void hentAdresseHvisFinnes_ingenAdresse_forventException() throws TekniskException {
        adresseGrunnlag.hentAdresseHvisFinnes();
    }
}