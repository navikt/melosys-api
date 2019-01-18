package no.nav.melosys.service.dokument.sed.bygger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigArbeid;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//For gjenbruk av AbstraktSedDatabygger implementasjonen i nåværende og fremtidige tester

public class DataByggerStubs {

    public static Behandling hentBehandlingStub() {
        Behandling behandling = new Behandling();

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
        saksopplysning.setDokument(søknadDokument);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysninger.add(saksopplysning);

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBEIDSFORHOLD);
        saksopplysning.setDokument(new ArbeidsforholdDokument());
        saksopplysninger.add(saksopplysning);

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning.setDokument(new PersonDokument());
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
