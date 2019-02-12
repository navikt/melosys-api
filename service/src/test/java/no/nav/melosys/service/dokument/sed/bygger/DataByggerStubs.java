package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.soeknad.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//For gjenbruk av AbstraktSedDatabygger implementasjonen i nåværende og fremtidige tester

public class DataByggerStubs {

    public static Behandling hentBehandlingStub() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.adresse = hentStrukturertAddresseStub();
        foretakUtland.orgnr = "orgnr";
        foretakUtland.navn = "navn foretak";

        Saksopplysning saksopplysning = new Saksopplysning();
        SoeknadDokument søknadDokument = new SoeknadDokument();
        søknadDokument.selvstendigArbeid = new SelvstendigArbeid();
        søknadDokument.foretakUtland = Lists.newArrayList(foretakUtland);
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = "12312312";
        søknadDokument.selvstendigArbeid.selvstendigForetak = Collections.singletonList(selvstendigForetak);
        søknadDokument.selvstendigArbeid.erSelvstendig = true;
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = hentStrukturertAddresseStub();
        arbeidUtland.foretakNavn = "foretaknavn";
        arbeidUtland.foretakOrgnr = "32132133";
        søknadDokument.arbeidUtland = Collections.singletonList(arbeidUtland);
        saksopplysning.setDokument(søknadDokument);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysninger.add(saksopplysning);

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBEIDSFORHOLD);
        saksopplysning.setDokument(new ArbeidsforholdDokument());
        saksopplysninger.add(saksopplysning);

        PersonDokument personDokument = new PersonDokument();
        personDokument.erEgenAnsatt = true;
        personDokument.fødselsdato = LocalDate.now();
        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setLand(new Land(Land.NORGE));
        bostedsadresse.setPoststed("1212");
        bostedsadresse.setGateadresse(new Gateadresse());
        personDokument.bostedsadresse = bostedsadresse;

        Familiemedlem familiemedlem = new Familiemedlem();
        familiemedlem.navn = "farnavn";
        familiemedlem.fnr = "111111111";
        familiemedlem.familierelasjon = Familierelasjon.FARA;
        personDokument.familiemedlemmer = Collections.singletonList(familiemedlem);

        KjoennsType kjønn = new KjoennsType();
        kjønn.setKode("M");
        personDokument.kjønn = kjønn;

        personDokument.fornavn = "Mrfornavn";
        personDokument.etternavn = "Spock";
        personDokument.statsborgerskap = new Land(Land.NORGE);

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning.setDokument(personDokument);
        saksopplysninger.add(saksopplysning);

        return behandling;
    }

    public static StrukturertAdresse hentStrukturertAddresseStub() {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landKode = "Land";
        return strukturertAdresse;
    }

    public static Set hentOrganisasjonDokumentSetStub() {
        HashSet<OrganisasjonDokument> orgDokumentHashSet = new HashSet<>();
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.organisasjonDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonDokument.organisasjonDetaljer.hentStrukturertForretningsadresse()).thenReturn(hentStrukturertAddresseStub());
        orgDokumentHashSet.add(organisasjonDokument);

        return orgDokumentHashSet;
    }
}
