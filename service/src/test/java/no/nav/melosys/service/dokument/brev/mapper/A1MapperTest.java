package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.YrkesgruppeType;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class A1MapperTest {

    private A1Mapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EnhancedRandom enhancedRandom;

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataDto brevDataDto;

    @Before
    public void setUp() {
        mapper = new A1Mapper();
        enhancedRandom = EnhancedRandomBuilder
            .aNewEnhancedRandomBuilder()
            .scanClasspathForConcreteTypes(true)
            .build();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART16_2);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());

        behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());
        when(behandlingsresultat.getLovvalgsperioder()).thenReturn(new HashSet<>(Arrays.asList(lovvalgsperiode)));

        Bostedsadresse boAdresse = new Bostedsadresse();
        boAdresse.getGateadresse().setGatenavn("Gatenavn");
        boAdresse.getGateadresse().setHusnummer(23);
        boAdresse.setPostnr("0165");
        boAdresse.setPoststed("Oslo");
        boAdresse.setLand(new Land(Land.NORGE));

        PersonDokument person = new PersonDokument();
        person.kjønn = new KjoennsType();
        person.kjønn.setKode("K");
        person.fornavn = "Ola";
        person.etternavn = "Nordmann";
        person.fødselsdato = LocalDate.now();
        person.statsborgerskap = new Land();
        person.statsborgerskap.setKode("NO");
        person.bostedsadresse = boAdresse;

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning.setDokument(person);

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(saksopplysning)));
        when(behandling.getFagsak()).thenReturn(new Fagsak());


        UstrukturertAdresse adresse = new UstrukturertAdresse();
        adresse.landKode = "Land";
        adresse.adresselinjer.add("Gatenavn");
        adresse.adresselinjer.add("25");
        adresse.adresselinjer.add("Postnummer");
        adresse.adresselinjer.add("Poststed");
        adresse.adresselinjer.add("Region");

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landKode = "Land";

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = strukturertAdresse;
        SoeknadDokument søknad = new SoeknadDokument();
        søknad.arbeidUtland = Arrays.asList(arbeidUtland);

        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.getForretningsadresseUstrukturert()).thenReturn(adresse);

        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setNavn(Arrays.asList("JARLSBERG", "INTERNATIONAL"));
        organisasjonDokument.setOrgnummer("123456789");
        organisasjonDokument.setOrganisasjonDetaljer(organisasjonsDetaljer);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.navn = "Jarlsberg";
        foretakUtland.orgnr = "1235234234";

        brevDataDto = new BrevDataDto();
        brevDataDto.yrkesgruppe = YrkesgruppeType.YRKESAKTIV;
        brevDataDto.norskeVirksomheter = new HashSet<>();
        brevDataDto.norskeVirksomheter.add(organisasjonDokument);   // Hovedvirksomhet
        brevDataDto.selvstendigeForetak = new HashSet<>();
        brevDataDto.utenlandskeVirksomheter = new ArrayList<>();
        brevDataDto.utenlandskeVirksomheter.add(foretakUtland);
        brevDataDto.søknad = søknad;
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = enhancedRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevDataDto);

        assertThat(xml).isNotNull();
    }
}