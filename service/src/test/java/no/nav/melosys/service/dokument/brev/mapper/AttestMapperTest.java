package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagMaritimtArbeidssted;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttestMapperTest {

    private AttestMapper mapper;

    private EasyRandom easyRandom;

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataVedlegg brevData;

    @BeforeEach
    public void setUp() {
        mapper = new AttestMapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);

        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());

        behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());
        when(behandlingsresultat.getLovvalgsperioder()).thenReturn(new HashSet<>(Collections.singletonList(lovvalgsperiode)));
        when(behandlingsresultat.hentLovvalgsperiode()).thenReturn(lovvalgsperiode);

        StrukturertAdresse boAdresse = new StrukturertAdresse();
        boAdresse.setGatenavn("HjemmeGata");
        boAdresse.setHusnummerEtasjeLeilighet("25");
        boAdresse.setPostnummer("0165");
        boAdresse.setPoststed("Poststed");
        boAdresse.setRegion("Region");
        boAdresse.setLandkode(Landkoder.NO.getKode());

        PersonDokument person = new PersonDokument();
        person.setKjønn(new KjoennsType("K"));
        person.setFornavn("Ola");
        person.setEtternavn("Nordmann");
        person.setFødselsdato(LocalDate.now());
        person.setStatsborgerskap(new Land(Land.NORGE));

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setHusnummerEtasjeLeilighet("25");
        strukturertAdresse.setGatenavn("Gatenavn");
        strukturertAdresse.setPostnummer("0165");
        strukturertAdresse.setPoststed("Poststed");
        strukturertAdresse.setRegion("Region");
        strukturertAdresse.setLandkode(Landkoder.NO.getKode());

        FysiskArbeidssted arbeidssted = new FysiskArbeidssted(null, strukturertAdresse);

        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(strukturertAdresse);

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("JARLSBERG INTERNATIONAL",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID);

        AvklartVirksomhet utenlandskVirksomhet = new AvklartVirksomhet("Jarlsberg",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID);

        no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted fysiskArbeidssted = new no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted ikkeFysiskArbeidssted = lagMaritimtArbeidssted();

        BrevDataA1 a1Data = new BrevDataA1();
        a1Data.yrkesgruppe = Yrkesgrupper.ORDINAER;
        a1Data.bostedsadresse = boAdresse;
        a1Data.arbeidssteder = Arrays.asList(fysiskArbeidssted, ikkeFysiskArbeidssted);
        a1Data.arbeidsland = Arrays.asList(Land_iso2.NO, Land_iso2.BG, Land_iso2.AT, Land_iso2.AX);
        a1Data.person = lagPersonopplysninger();
        a1Data.hovedvirksomhet = virksomhet;
        a1Data.bivirksomheter = new ArrayList<>(Collections.singletonList(utenlandskVirksomhet));

        brevData = new BrevDataVedlegg("Z1234567");
        brevData.brevDataA1 = a1Data;
    }

    @Test
    void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }
}
