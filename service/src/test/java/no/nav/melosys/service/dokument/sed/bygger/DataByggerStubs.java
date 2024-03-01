package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigArbeid;
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak;
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//For gjenbruk av AbstraktSedDatabygger implementasjonen i nåværende og fremtidige tester

class DataByggerStubs {

    static Behandling hentBehandlingStub() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());

        Fagsak fagsak = new Fagsak();
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        myndighet.setInstitusjonID("SE:123321");
        fagsak.setAktører(Collections.singleton(myndighet));
        behandling.setFagsak(fagsak);

        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        behandling.setSaksopplysninger(saksopplysninger);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.setAdresse(hentStrukturertAddresseStub());
        foretakUtland.setNavn("navn foretak");
        foretakUtland.setUuid("uuid");

        Soeknad søknadDokument = new Soeknad();
        søknadDokument.selvstendigArbeid = new SelvstendigArbeid();
        søknadDokument.foretakUtland = Lists.newArrayList(foretakUtland);
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.setOrgnr("12312312");
        søknadDokument.selvstendigArbeid.setSelvstendigForetak(Collections.singletonList(selvstendigForetak));
        søknadDokument.selvstendigArbeid.setErSelvstendig(true);
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted("foretaknavn", hentStrukturertAddresseStub());
        søknadDokument.arbeidPaaLand.setFysiskeArbeidssteder(Lists.newArrayList(fysiskArbeidssted));
        UtenlandskIdent utenlandskIdent = new UtenlandskIdent();
        utenlandskIdent.setIdent("439205843");
        utenlandskIdent.setLandkode("SE");
        søknadDokument.personOpplysninger.getUtenlandskIdent().add(utenlandskIdent);
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(søknadDokument);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setDokument(new ArbeidsforholdDokument());
        saksopplysninger.add(saksopplysning);

        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.setEnhetNavn("enhet");
        søknadDokument.maritimtArbeid = Collections.singletonList(maritimtArbeid);

        PersonDokument personDokument = new PersonDokument();
        personDokument.setErEgenAnsatt(true);
        personDokument.setFødselsdato(LocalDate.now());
        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setLand(new Land(Land.NORGE));
        bostedsadresse.setPoststed("1212");
        bostedsadresse.setGateadresse(new Gateadresse());
        personDokument.setBostedsadresse(bostedsadresse);

        Familiemedlem familiemedlem = new Familiemedlem();
        familiemedlem.setNavn("farnavn");
        familiemedlem.setFnr("111111111");
        familiemedlem.setFamilierelasjon(Familierelasjon.FARA);
        personDokument.setFamiliemedlemmer(Collections.singletonList(familiemedlem));

        personDokument.setKjønn(new KjoennsType("M"));

        personDokument.setFornavn("Mrfornavn");
        personDokument.setEtternavn("Spock");
        personDokument.setStatsborgerskap(new Land(Land.NORGE));

        saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(personDokument);
        saksopplysninger.add(saksopplysning);

        return behandling;
    }

    static Behandling hentBehandlingMedManglendeAdressefelterStub(boolean fysiskArbeidsstedManglerLandkode,
                                                                  boolean arbeidsgivendeForetakUtlandManglerLandkode,
                                                                  boolean selvstendigForetakUtlandManglerLandkode) {
        Behandling behandling = hentBehandlingStub();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        MottatteOpplysningerData mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();

        FysiskArbeidssted fysiskArbeidssted = mottatteOpplysningerData.arbeidPaaLand.getFysiskeArbeidssteder().remove(0);
        fysiskArbeidssted.getAdresse().setPoststed(null);
        if (fysiskArbeidsstedManglerLandkode) {
            fysiskArbeidssted.getAdresse().setLandkode(null);
        }
        mottatteOpplysningerData.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);

        ForetakUtland foretakUtland = mottatteOpplysningerData.foretakUtland.remove(0);
        foretakUtland.getAdresse().setPostnummer(null);
        foretakUtland.getAdresse().setPoststed(null);
        if (arbeidsgivendeForetakUtlandManglerLandkode || selvstendigForetakUtlandManglerLandkode) {
            foretakUtland.getAdresse().setLandkode(null);
        }
        foretakUtland.setSelvstendigNæringsvirksomhet(selvstendigForetakUtlandManglerLandkode);
        mottatteOpplysningerData.foretakUtland.add(foretakUtland);

        return behandling;
    }

    private static StrukturertAdresse hentStrukturertAddresseStub() {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setHusnummerEtasjeLeilighet("25");
        strukturertAdresse.setGatenavn("Gatenavn");
        strukturertAdresse.setPostnummer("0165");
        strukturertAdresse.setRegion("Region");
        strukturertAdresse.setLandkode(Landkoder.NO.getKode());
        return strukturertAdresse;
    }

    static Set hentOrganisasjonDokumentSetStub() {
        HashSet<OrganisasjonDokument> orgDokumentHashSet = new HashSet<>();
        OrganisasjonDokument organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .organisasjonsDetaljer(mock(OrganisasjonsDetaljer.class))
            .orgnummer("orgnr")
            .build();
        when(organisasjonDokument.getOrganisasjonDetaljer().hentStrukturertForretningsadresse()).thenReturn(hentStrukturertAddresseStub());
        orgDokumentHashSet.add(organisasjonDokument);

        return orgDokumentHashSet;
    }
}
