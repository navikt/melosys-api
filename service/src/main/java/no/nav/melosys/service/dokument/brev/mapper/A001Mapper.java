package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000115.*;
import no.nav.dok.melosysbrev._000115.BostedsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.BrevDataUtils;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagPersonnavn;

public class A001Mapper {

    public SEDA001 mapSEDA001(BrevDataA001 brevData) throws TekniskException {
        SEDA001 seda001 = new SEDA001();

        seda001.setAntallVedlegg("0");

        try {
            seda001.setDatoSendt(BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(Instant.now()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Feil ved konvertering");
        }

        seda001.setLandkodeAvsender(Landkoder.NO.getKode());

        seda001.setTrygdemyndighet(mapTrygdemyndighet(brevData.utenlandskMyndighet));

        seda001.setPerson(mapPerson(brevData.personDokument, brevData.bostedsadresse, brevData.utenlandskIdent));

        // Foretakliste = Identifikasjon av arbeidsgiver (Kun arbeidsgivere)
        List<Virksomhet> arbeidsgivendeVirksomheter = brevData.arbeidsgivendeVirkomsheter;
        seda001.setForetakListe(mapForetakliste(arbeidsgivendeVirksomheter));

        List<Virksomhet> selvstendigeVirksomheter = brevData.selvstendigeVirksomheter;
        seda001.setSelvstendigNæringsvirksomhetListe(mapSelvstendigvirksometliste(selvstendigeVirksomheter));

        seda001.setArbeidsstedListe(mapArbeidsstedliste(brevData.arbeidssteder));

        seda001.setLovvalgsPeriodeListe(mapLovvalgsperioder(brevData.lovvalgsperioder));

        // Alle lovvalgsperiodene må ha samme landKode
        Lovvalgsperiode lovvalgsperiode = brevData.lovvalgsperioder.iterator().next();
        seda001.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(lovvalgsperiode.getUnntakFraBestemmelse().getKode()));
        seda001.setLovvalgsLand(lovvalgsperiode.getLovvalgsland().getKode());  // Alltid Norge

        // TODO: Implementasjon mangler i lovvalgsperiode
        //seda001.setTilleggsbestemmelse();

        // Mangler implementasjon i oppgavene. Lev1 støtter ikke purring
        seda001.setForespørselType(ForespoerselTypeKode.FOERSTEGANG);

        seda001.setTidligereAnmodningListe(mapTidligereAnmodningListe(brevData.tidligereAnmodninger));

        seda001.setTidligereLovvalgsperiodeListe(mapTidligereLovvalgsperioder(brevData.tidligereLovvalgsperioder));

        seda001.setVilkårBegrunnelse(mapVilkårBegrunnelse(brevData.vilkårsresultat161));

        if (brevData.ansettelsesperiode.isPresent()) {
            seda001.setAnsettelsesPeriode(mapAnsettelsesperiode(brevData.ansettelsesperiode.get()));
        }

        seda001.setFritekst(brevData.vilkårsresultat161.getBegrunnelseFritekst());

        return seda001;
    }

    public AnsettelsesPeriodeType mapAnsettelsesperiode(Periode ansettelsesperiode) {
        AnsettelsesPeriodeType ansettelsesperiodeType = new AnsettelsesPeriodeType();
        ansettelsesperiodeType.setFomDato(ansettelsesperiode.getFom().toString());
        return ansettelsesperiodeType;
    }

    private TidligereLovvalgsperiodeListeType mapTidligereLovvalgsperioder(Collection<Lovvalgsperiode> tidligerePerioder) throws TekniskException {
        TidligereLovvalgsperiodeListeType tidligereLovvalgsperiodeListeType = new TidligereLovvalgsperiodeListeType();
        for (Lovvalgsperiode lovvalgsperiode : tidligerePerioder) {
            PeriodeType periode = new PeriodeType();
            try {
                periode.setFomDato(BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
                periode.setTomDato(BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
            } catch (DatatypeConfigurationException e) {
                throw new TekniskException("Feil ved konvertering av dato for tidligere lovvalgsperiode");
            }

            LovvalgsbestemmelseKode bestemmelse = LovvalgsbestemmelseKode.fromValue(lovvalgsperiode.getBestemmelse().getKode());
            periode.setLovvalgsbestemmelse(bestemmelse);
            tidligereLovvalgsperiodeListeType.getTidligereLovvalgsperiode().add(periode);
        }
        return tidligereLovvalgsperiodeListeType;
    }

    private TidligereAnmodningListeType mapTidligereAnmodningListe(List<LocalDate> tidligereAnmodningdatoer) throws TekniskException {
        TidligereAnmodningListeType tidligereAnmodningListeType = new TidligereAnmodningListeType();
        for (LocalDate dato : tidligereAnmodningdatoer) {
            TidligereAnmodningType tidligereAnmodningType = new TidligereAnmodningType();
            try {
                tidligereAnmodningType.setTidligereAnmodningsDato(BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(dato));
            } catch (DatatypeConfigurationException e) {
                throw new TekniskException("Feil ved konvertering av dato for tidligere anmodning");
            }
            tidligereAnmodningListeType.getTidligereAnmodning().add(tidligereAnmodningType);
        }

        return new TidligereAnmodningListeType();
    }

    private TrygdemyndighetType mapTrygdemyndighet(UtenlandskMyndighet utenlandskMyndighet) {
        TrygdemyndighetType trygdemyndighet = new TrygdemyndighetType();
        trygdemyndighet.setTrygdemyndighetsinstitusjon(utenlandskMyndighet.institusjonskode);
        trygdemyndighet.setInstitusjonsnavn(utenlandskMyndighet.navn);
        trygdemyndighet.setTrygdemyndighetsland(utenlandskMyndighet.landkode.getKode());

        TrygdemyndighetsadresseType adresseBrev = new TrygdemyndighetsadresseType();
        adresseBrev.setGatenavn(utenlandskMyndighet.gateadresse);
        adresseBrev.setPostnummer(utenlandskMyndighet.postnummer);
        adresseBrev.setPoststed(utenlandskMyndighet.poststed);
        trygdemyndighet.setTrygdemyndighetsadresse(adresseBrev);

        return trygdemyndighet;
    }

    private PersonType mapPerson(PersonDokument personDok, Bostedsadresse adresse, Optional<String> utenlandskIdent) throws TekniskException {
        PersonType person = new PersonType();
        person.setPersonnavn(lagPersonnavn(personDok));
        person.setStatsborgerskapListe(mapStatsborgerskapListe(personDok));
        person.setKjønn(KjoennKode.fromValue(personDok.kjønn.getKode()));
        person.setBostedsadresse(mapBostedAdresse(adresse));
        person.setFødselsnummer(personDok.fnr);
        //Fødeland og Fødested skal ikke fylles ut
        if (utenlandskIdent.isPresent()) {
            person.setUtenlandskID(utenlandskIdent.get());
        }
        try {
            person.setFødselsdato(convertToXMLGregorianCalendarRemoveTimezone(personDok.fødselsdato));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e);
        }

        return person;
    }

    private StatsborgerskapListeType mapStatsborgerskapListe(PersonDokument personDok) {
        StatsborgerskapType statsborgerskap = new StatsborgerskapType();
        statsborgerskap.setStatsborgerskap(personDok.statsborgerskap.getKode());

        StatsborgerskapListeType statsborgerskapListe = new StatsborgerskapListeType();
        statsborgerskapListe.getStatsborgerskap().add(statsborgerskap);

        return statsborgerskapListe;
    }

    private VilkaarBegrunnelseType mapVilkårBegrunnelse(Vilkaarsresultat resultat) {
        VilkaarBegrunnelseType vilkårbegrunnelse = new VilkaarBegrunnelseType();
        VilkaarBegrunnelse begrunnelse = resultat.getBegrunnelser().iterator().next();
        Art161AnmodningBegrunnelseKode begrunnelseKode = Art161AnmodningBegrunnelseKode.fromValue(begrunnelse.getKode());
        vilkårbegrunnelse.setStandardBegrunnelse(begrunnelseKode);
        return vilkårbegrunnelse;
    }

    private ArbeidsstedListeType mapArbeidsstedliste(List<Arbeidssted> arbeidssteder) {
        ArbeidsstedListeType arbeidsstedListe = new ArbeidsstedListeType();
        for (Arbeidssted arbeidssted : arbeidssteder) {

            ArbeidsstedType arbeidsstedBrev;
            if (arbeidssted.adresse != null) {
                arbeidsstedBrev = mapFysiskArbeidssted(arbeidssted);
            }
            else {
                arbeidsstedBrev = mapIkkeFysiskArbeidssted(arbeidssted);
            }
            arbeidsstedListe.getArbeidssted().add(arbeidsstedBrev);
        }
        return arbeidsstedListe;
    }

    private ArbeidsstedType mapFysiskArbeidssted(Arbeidssted arbeidssted) {
        ArbeidsstedType arbeidsstedBrev = new ArbeidsstedType();
        arbeidsstedBrev.setNavn(arbeidssted.navn);
        arbeidsstedBrev.setIkkeFysiskArbeidssted("false");
        arbeidsstedBrev.setYrkesgruppe(YrkesgruppeKode.ORDINAER);

        StrukturertAdresse adresse = arbeidssted.adresse;
        AdresseType3 adresseBrev = new AdresseType3();
        adresseBrev.setGatenavn(adresse.gatenavn);
        adresseBrev.setHusnummer(adresse.husnummer);
        adresseBrev.setPostnummer(adresse.postnummer);
        adresseBrev.setPossted(adresse.poststed);
        adresseBrev.setRegion(adresse.region);
        adresseBrev.setLand(adresse.landKode);
        arbeidsstedBrev.setAdresse(adresseBrev);

        return arbeidsstedBrev;
    }

    private ArbeidsstedType mapIkkeFysiskArbeidssted(Arbeidssted arbeidssted) {
        ArbeidsstedType arbeidsstedBrev = new ArbeidsstedType();
        arbeidsstedBrev.setNavn(arbeidssted.navn);
        arbeidsstedBrev.setIkkeFysiskArbeidssted("true");
        arbeidsstedBrev.setYrkesgruppe(YrkesgruppeKode.fromValue(arbeidssted.yrkesgruppe.getKode()));

        AdresseType3 adresseBrev = new AdresseType3();
        adresseBrev.setLand(arbeidssted.landKode);
        arbeidsstedBrev.setAdresse(adresseBrev);
        return arbeidsstedBrev;
    }

    private BostedsadresseType mapBostedAdresse(Bostedsadresse bosted) {
        Gateadresse gateadresse = bosted.getGateadresse();

        BostedsadresseType bostedsadresse = new BostedsadresseType();
        bostedsadresse.setGatenavn(gateadresse.getGatenavn());
        bostedsadresse.setHusnummer(gateadresse.getGatenummer() + " " + gateadresse.getHusbokstav());
        bostedsadresse.setPostnummer(bosted.getPostnr());
        bostedsadresse.setPoststed(bosted.getPoststed());
        bostedsadresse.setLand(bosted.getLand().getKode());
        bostedsadresse.setAdresseType(BostedsadresseTypeKode.BOSTEDSLAND); // Lev1 kun bostedsland
        return bostedsadresse;
    }

    private ForetakListeType mapForetakliste(List<Virksomhet> arbeidsgivendeVirksomheter) {
        ForetakListeType foretakListe = new ForetakListeType();
        for (Virksomhet virksomhet : arbeidsgivendeVirksomheter) {
            ForetakType foretak = new ForetakType();
            foretak.setNavn(virksomhet.navn);
            foretak.setOrgnummer(virksomhet.orgnr);
            foretak.setYrkesaktivitet(YrkesaktivitetsKode.LOENNET_ARBEID); // TODO: Frilanser ikke implementert
            foretak.setHovedvirksomhet("true");  // Kun et foretak i Lev1

            UstrukturertAdresse adresse = (UstrukturertAdresse) virksomhet.adresse;
            AdresseType adresseBrev = new AdresseType();
            adresseBrev.setLand(adresse.landKode);

            List<String> adresselinjer = adresse.adresselinjer;
            adresseBrev.setAdresselinje1(adresselinjer.get(0));
            adresseBrev.setAdresselinje2(adresselinjer.get(1));
            if (adresselinjer.size() > 2) {
                adresseBrev.setAdresselinje3(adresselinjer.get(2));
            }
            if (adresselinjer.size() > 3) {
                adresseBrev.setAdresselinje4(adresselinjer.get(3));
            }
            foretak.setAdresse(adresseBrev);
            foretakListe.getForetak().add(foretak);
        }

        return foretakListe;
    }

    private SelvstendigNaeringsvirksomhetListeType mapSelvstendigvirksometliste(List<Virksomhet> virksomheter) {
        SelvstendigNaeringsvirksomhetListeType selvstendigeVirksomheter = new SelvstendigNaeringsvirksomhetListeType();
        for (Virksomhet virksomhet : virksomheter) {
            SelvstendigNaeringsvirksomhetType selvstendigVirksomhet = new SelvstendigNaeringsvirksomhetType();
            selvstendigVirksomhet.setNavn(virksomhet.navn);
            selvstendigVirksomhet.setOrgnummer(virksomhet.orgnr);

            AdresseType2 adresseBrev = new AdresseType2();
            UstrukturertAdresse adresse = (UstrukturertAdresse) virksomhet.adresse;
            adresseBrev.setLand(adresse.landKode);

            List<String> adresselinjer = adresse.adresselinjer;
            adresseBrev.setAdresselinje1(adresselinjer.get(0));
            adresseBrev.setAdresselinje2(adresselinjer.get(1));
            if (adresselinjer.size() > 2) {
                adresseBrev.setAdresselinje3(adresselinjer.get(2));
            }
            if (adresselinjer.size() > 3) {
                adresseBrev.setAdresselinje4(adresselinjer.get(3));
            }
            selvstendigVirksomhet.setAdresse(adresseBrev);
            selvstendigeVirksomheter.getSelvstendigNæringsvirksomhet().add(selvstendigVirksomhet);
        }
        return selvstendigeVirksomheter;
    }

    private LovvalgsPeriodeListeType mapLovvalgsperioder(Collection<Lovvalgsperiode> lovvalgsperioder) throws TekniskException {
        LovvalgsPeriodeListeType lovvalgsperioderBrev = new LovvalgsPeriodeListeType();
        for (Lovvalgsperiode periode : lovvalgsperioder) {
            LovvalgsPeriodeType lovvalgsperiodeBrev = mapLovvalgsperiode(periode);
            lovvalgsperioderBrev.getLovvalgsPeriode().add(lovvalgsperiodeBrev);

            UnntakFraLovvalgslandType unntakFraLovvalgslandType = mapUnntaksland(periode);
            lovvalgsperioderBrev.getUnntakFraLovvalgsland().add(unntakFraLovvalgslandType);
        }
        return lovvalgsperioderBrev;
    }

    private LovvalgsPeriodeType mapLovvalgsperiode(Lovvalgsperiode periode) throws TekniskException {
        LovvalgsPeriodeType lovvalgsperiodeBrev = new LovvalgsPeriodeType();
        try {
            lovvalgsperiodeBrev.setFomDato(BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(periode.getFom()));
            lovvalgsperiodeBrev.setTomDato(BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(periode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Feil ved konvertering");
        }
        return lovvalgsperiodeBrev;
    }

    private UnntakFraLovvalgslandType mapUnntaksland(Lovvalgsperiode periode) {
        UnntakFraLovvalgslandType unntakFraLovvalgslandType = new UnntakFraLovvalgslandType();
        String land = periode.getUnntakFraLovvalgsland().getKode();
        unntakFraLovvalgslandType.getUnntakFraLovvalgsland().add(land);
        return unntakFraLovvalgslandType;
    }
}
