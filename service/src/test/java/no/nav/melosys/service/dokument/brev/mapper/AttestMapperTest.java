package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AttestMapperTest {

    private AttestMapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EnhancedRandom enhancedRandom;

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataVedlegg brevData;

    @Before
    public void setUp() {
        mapper = new AttestMapper();
        enhancedRandom = EnhancedRandomConfigurer.randomForDokProd();

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

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landKode = "Land";

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = strukturertAdresse;

        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(strukturertAdresse);

        Virksomhet virksomhet = new Virksomhet("JARLSBERG INTERNATIONAL",
                                               "123456789",
                                                strukturertAdresse);

        Virksomhet utenlandksVirksomhet = new Virksomhet("Jarlsberg",
                                                         "123456789",
                                                         strukturertAdresse);

        Arbeidssted fysiskArbeidssted = new Arbeidssted("JARLSBERG INTERNATIONAL", strukturertAdresse);
        Arbeidssted ikkeFysiskArbeidssted = new Arbeidssted("Seven Kestrel", "GB", YrkesgruppeType.SOKKEL_ELLER_SKIP);

        BrevDataA1 a1Data = new BrevDataA1();
        a1Data.yrkesgruppe = YrkesgruppeType.ORDINAER;
        a1Data.norskeVirksomheter = new ArrayList<>(Arrays.asList(virksomhet));   // Hovedvirksomhet
        a1Data.selvstendigeForetak = new HashSet<>();
        a1Data.utenlandskeVirksomheter = new ArrayList<>(Arrays.asList(utenlandksVirksomhet));
        a1Data.bostedsadresse = boAdresse;
        a1Data.arbeidssteder = Arrays.asList(fysiskArbeidssted, ikkeFysiskArbeidssted);
        a1Data.person = person;

        brevData = new BrevDataVedlegg("Z1234567");
        brevData.brevDataA1 = a1Data;
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = enhancedRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }
}