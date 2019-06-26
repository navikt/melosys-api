package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.MaritimtArbeidssted;
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

        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_2);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());

        behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());
        when(behandlingsresultat.getLovvalgsperioder()).thenReturn(new HashSet<>(Collections.singletonList(lovvalgsperiode)));

        StrukturertAdresse boAdresse = new StrukturertAdresse();
        boAdresse.gatenavn = "HjemmeGata";
        boAdresse.husnummer = "25";
        boAdresse.postnummer = "0165";
        boAdresse.poststed = "Poststed";
        boAdresse.region = "Region";
        boAdresse.landkode = Landkoder.NO.getKode();

        PersonDokument person = new PersonDokument();
        person.kjønn = new KjoennsType();
        person.kjønn.setKode("K");
        person.fornavn = "Ola";
        person.etternavn = "Nordmann";
        person.fødselsdato = LocalDate.now();
        person.statsborgerskap = new Land();
        person.statsborgerskap.setKode(Land.NORGE);

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landkode = Landkoder.NO.getKode();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = strukturertAdresse;

        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(strukturertAdresse);

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("JARLSBERG INTERNATIONAL",
                                                           "123456789",
                                                            strukturertAdresse,
                                                            Yrkesaktivitetstyper.LOENNET_ARBEID);

        AvklartVirksomhet utenlandksVirksomhet = new AvklartVirksomhet("Jarlsberg",
                                                                        "123456789",
                                                                        strukturertAdresse,
                                                                        Yrkesaktivitetstyper.LOENNET_ARBEID);

        Arbeidssted fysiskArbeidssted = new FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        AvklartMaritimtArbeid avklartMaritimtArbeid = mock(AvklartMaritimtArbeid.class);
        when(avklartMaritimtArbeid.getMaritimtype()).thenReturn(Maritimtyper.SKIP);
        when(avklartMaritimtArbeid.getLand()).thenReturn(Landkoder.GB.getKode());
        when(avklartMaritimtArbeid.getNavn()).thenReturn("Seven Kestrel");
        Arbeidssted ikkeFysiskArbeidssted = new MaritimtArbeidssted(avklartMaritimtArbeid);

        BrevDataA1 a1Data = new BrevDataA1();
        a1Data.yrkesgruppe = Yrkesgrupper.ORDINAER;
        a1Data.norskeVirksomheter = new ArrayList<>(Collections.singletonList(virksomhet));   // Hovedvirksomhet
        a1Data.selvstendigeForetak = new HashSet<>();
        a1Data.utenlandskeVirksomheter = new ArrayList<>(Collections.singletonList(utenlandksVirksomhet));
        a1Data.bostedsadresse = boAdresse;
        a1Data.arbeidssteder = Arrays.asList(fysiskArbeidssted, ikkeFysiskArbeidssted);
        a1Data.person = person;
        a1Data.hovedvirksomhet = virksomhet;

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