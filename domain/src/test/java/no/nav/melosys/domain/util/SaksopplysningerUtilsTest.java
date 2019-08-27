package no.nav.melosys.domain.util;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SaksopplysningerUtilsTest {

    @Test
    public void hentDokument() {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        Saksopplysning saksopplysning_1 = new Saksopplysning();
        saksopplysning_1.setType(SaksopplysningType.ARBFORH);
        saksopplysninger.add(saksopplysning_1);
        Saksopplysning saksopplysning_2 = new Saksopplysning();
        saksopplysning_2.setType(SaksopplysningType.SØKNAD);
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        saksopplysning_2.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysning_2);
        Saksopplysning saksopplysning_3 = new Saksopplysning();
        saksopplysning_3.setType(SaksopplysningType.MEDL);
        saksopplysninger.add(saksopplysning_3);

        behandling.setSaksopplysninger(saksopplysninger);

        Optional<SaksopplysningDokument> saksopplysningDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.SØKNAD);
        assertThat(saksopplysningDokument).isNotEmpty();
        assertThat(saksopplysningDokument.get()).isEqualTo(soeknadDokument);
    }

    @Test
    public void hentArbeidsforholdDokument() throws TekniskException {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        ArbeidsforholdDokument arbDok = new ArbeidsforholdDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(arbDok);
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysninger.add(saksopplysning);

        SaksopplysningDokument saksopplysningdokument = SaksopplysningerUtils.hentArbeidsforholdDokument(behandling);
        assertThat(saksopplysningdokument).isEqualTo(arbDok);
    }

    @Test(expected = TekniskException.class)
    public void hentArbeidsforholdDokumentMangler() throws TekniskException {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysninger.add(saksopplysning);

        SaksopplysningerUtils.hentArbeidsforholdDokument(behandling);
    }

    @Test
    public void hentPersonDokument() throws TekniskException {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        PersonDokument personDok = new PersonDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDok);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysninger.add(saksopplysning);

        SaksopplysningDokument saksopplysningdokument = SaksopplysningerUtils.hentPersonDokument(behandling);
        assertThat(saksopplysningdokument).isEqualTo(personDok);
    }

    @Test
    public void hentSammensattNavn() {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        PersonDokument personDok = new PersonDokument();
        personDok.sammensattNavn = "FØRST SISTE";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(personDok);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysninger.add(saksopplysning);

        assertThat(SaksopplysningerUtils.hentSammensattNavn(behandling)).isEqualTo("FØRST SISTE");
    }

    @Test
    public void hentMedlemskapsDokument() throws TekniskException {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        MedlemskapDokument medlDok = new MedlemskapDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(medlDok);
        saksopplysning.setType(SaksopplysningType.MEDL);
        saksopplysninger.add(saksopplysning);

        SaksopplysningDokument saksopplysningdokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        assertThat(saksopplysningdokument).isEqualTo(medlDok);
    }

    @Test
    public void hentSøknadDokument() throws TekniskException {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        SoeknadDokument søknadDok = new SoeknadDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(søknadDok);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysninger.add(saksopplysning);

        SaksopplysningDokument saksopplysningdokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
        assertThat(saksopplysningdokument).isEqualTo(søknadDok);
    }

    @Test(expected = TekniskException.class)
    public void hentSøknadDokumentSkalIkkeLevereMedlemskapsDok() throws TekniskException {
        Behandling behandling = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        MedlemskapDokument medlDok = new MedlemskapDokument();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(medlDok);
        saksopplysning.setType(SaksopplysningType.MEDL);
        saksopplysninger.add(saksopplysning);

        SaksopplysningDokument saksopplysningdokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
        assertThat(saksopplysningdokument).isNotEqualTo(medlDok);
    }
}