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
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.FysiskArbeidssted;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagPersonnavn;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class A001Mapper {

    public SEDA001 mapSEDA001(BrevDataA001 brevData) throws TekniskException {
        SEDA001 seda001 = new SEDA001();

        seda001.setAntallVedlegg("0");

        seda001.setDatoSendt(convertToXMLGregorianCalendarRemoveTimezone(Instant.now()));

        seda001.setLandkodeAvsender(hentIso3Landkode(Landkoder.NO.getKode()));

        seda001.setTrygdemyndighet(mapTrygdemyndighet(brevData.utenlandskMyndighet));

        seda001.setPerson(mapPerson(brevData.personDokument, brevData.bostedsadresse, brevData.utenlandskIdent));

        List<AvklartVirksomhet> selvstendigeVirksomheter = brevData.selvstendigeVirksomheter;

        //A001 Skal ikke inneholde både Identifikasjon av arbeidsgiveren og Identifikasjon av selvstendig næringsvirksomhet
        if (!selvstendigeVirksomheter.isEmpty()) {
            seda001.setSelvstendigNæringsvirksomhetListe(mapSelvstendigvirksometliste(selvstendigeVirksomheter));
        } else {
            // Foretakliste = Identifikasjon av arbeidsgiver (Kun arbeidsgivere)
            List<AvklartVirksomhet> arbeidsgivendeVirksomheter = brevData.arbeidsgivendeVirkomsheter;
            seda001.setForetakListe(mapForetakliste(arbeidsgivendeVirksomheter));
        }

        seda001.setArbeidsstedListe(mapArbeidsstedliste(brevData.arbeidssteder));

        seda001.setLovvalgsPeriodeListe(mapLovvalgsperioder(brevData.lovvalgsperioder));

        // Alle lovvalgsperiodene må ha samme landkode
        Lovvalgsperiode lovvalgsperiode = brevData.lovvalgsperioder.iterator().next();
        seda001.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(lovvalgsperiode.getUnntakFraBestemmelse().getKode()));
        seda001.setLovvalgsLand(hentIso3Landkode(lovvalgsperiode.getLovvalgsland().getKode()));  // Alltid Norge

        if (lovvalgsperiode.getTilleggsbestemmelse() != null) {
            seda001.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(lovvalgsperiode.getTilleggsbestemmelse().getKode()));
        }

        // Mangler implementasjon i oppgavene. Lev1 støtter ikke purring
        seda001.setForespørselType(ForespoerselTypeKode.FOERSTEGANG);

        seda001.setTidligereAnmodningListe(mapTidligereAnmodningListe(brevData.tidligereAnmodninger));

        seda001.setTidligereLovvalgsperiodeListe(mapTidligereLovvalgsperioder(brevData.tidligereLovvalgsperioder));

        seda001.setVilkårBegrunnelse(mapVilkårBegrunnelse(brevData.vilkårsresultat161));

        brevData.ansettelsesperiode.ifPresent(periode -> seda001.setAnsettelsesPeriode(mapAnsettelsesperiode(periode)));

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
                periode.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
                periode.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
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
                tidligereAnmodningType.setTidligereAnmodningsDato(convertToXMLGregorianCalendarRemoveTimezone(dato));
            } catch (DatatypeConfigurationException e) {
                throw new TekniskException("Feil ved konvertering av dato for tidligere anmodning");
            }
            tidligereAnmodningListeType.getTidligereAnmodning().add(tidligereAnmodningType);
        }

        return new TidligereAnmodningListeType();
    }

    private TrygdemyndighetType mapTrygdemyndighet(UtenlandskMyndighet utenlandskMyndighet) throws TekniskException {
        TrygdemyndighetType trygdemyndighet = new TrygdemyndighetType();
        trygdemyndighet.setTrygdemyndighetsinstitusjon(utenlandskMyndighet.institusjonskode);
        trygdemyndighet.setInstitusjonsnavn(utenlandskMyndighet.navn);
        trygdemyndighet.setTrygdemyndighetsland(hentIso3Landkode(utenlandskMyndighet.landkode.getKode()));

        TrygdemyndighetsadresseType adresseBrev = new TrygdemyndighetsadresseType();
        adresseBrev.setGatenavn(utenlandskMyndighet.gateadresse);
        adresseBrev.setPostnummer(utenlandskMyndighet.postnummer);
        adresseBrev.setPoststed(utenlandskMyndighet.poststed);
        trygdemyndighet.setTrygdemyndighetsadresse(adresseBrev);

        return trygdemyndighet;
    }

    private PersonType mapPerson(PersonDokument personDok, StrukturertAdresse bostedsadresse, Optional<String> utenlandskIdent) throws TekniskException {
        PersonType person = new PersonType();
        person.setPersonnavn(lagPersonnavn(personDok));
        person.setStatsborgerskapListe(mapStatsborgerskapListe(personDok));
        person.setKjønn(KjoennKode.fromValue(personDok.kjønn.getKode()));
        person.setBostedsadresse(mapBostedAdresse(bostedsadresse));
        person.setFødselsnummer(personDok.fnr);
        //Fødeland og Fødested skal ikke fylles ut
        utenlandskIdent.ifPresent(person::setUtenlandskID);
        try {
            person.setFødselsdato(convertToXMLGregorianCalendarRemoveTimezone(personDok.fødselsdato));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil ved konvertering av fødselsdato", e);
        }

        return person;
    }

    private StatsborgerskapListeType mapStatsborgerskapListe(PersonDokument personDok) throws TekniskException {
        StatsborgerskapType statsborgerskap = new StatsborgerskapType();
        statsborgerskap.setStatsborgerskap(hentIso3Landkode(personDok.statsborgerskap.getKode()));

        StatsborgerskapListeType statsborgerskapListe = new StatsborgerskapListeType();
        statsborgerskapListe.getStatsborgerskap().add(statsborgerskap);

        return statsborgerskapListe;
    }

    private VilkaarBegrunnelseType mapVilkårBegrunnelse(Vilkaarsresultat resultat) {
        VilkaarBegrunnelseType vilkårbegrunnelse = new VilkaarBegrunnelseType();

        VilkaarBegrunnelse begrunnelse = resultat.getBegrunnelser().stream()
            .filter(v -> Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN.value().equals(v.getKode()))
            .findFirst().orElse(resultat.getBegrunnelser().iterator().next());

        Art161AnmodningBegrunnelseKode begrunnelseKode = Art161AnmodningBegrunnelseKode.fromValue(begrunnelse.getKode());
        vilkårbegrunnelse.setStandardBegrunnelse(begrunnelseKode);
        return vilkårbegrunnelse;
    }

    private ArbeidsstedListeType mapArbeidsstedliste(List<Arbeidssted> arbeidssteder) throws TekniskException {
        ArbeidsstedListeType arbeidsstedListe = new ArbeidsstedListeType();
        for (Arbeidssted arbeidssted : arbeidssteder) {

            ArbeidsstedType arbeidsstedBrev;
            if (arbeidssted.erFysisk()) {
                arbeidsstedBrev = mapFysiskArbeidssted((FysiskArbeidssted)arbeidssted);
            }
            else {
                arbeidsstedBrev = mapIkkeFysiskArbeidssted(arbeidssted);
            }
            arbeidsstedListe.getArbeidssted().add(arbeidsstedBrev);
        }
        return arbeidsstedListe;
    }

    private ArbeidsstedType mapFysiskArbeidssted(FysiskArbeidssted arbeidssted) throws TekniskException {
        ArbeidsstedType arbeidsstedBrev = new ArbeidsstedType();
        arbeidsstedBrev.setNavn(arbeidssted.getNavn());
        arbeidsstedBrev.setIkkeFysiskArbeidssted("false");
        arbeidsstedBrev.setYrkesgruppe(YrkesgruppeKode.ORDINAER);

        StrukturertAdresse adresse = arbeidssted.adresse;
        AdresseType3 adresseBrev = new AdresseType3();
        adresseBrev.setGatenavn(adresse.gatenavn);
        adresseBrev.setHusnummer(adresse.husnummer);
        adresseBrev.setPostnummer(adresse.postnummer);
        adresseBrev.setPossted(adresse.poststed);
        adresseBrev.setRegion(adresse.region);
        adresseBrev.setLand(hentIso3Landkode(adresse.landkode));
        arbeidsstedBrev.setAdresse(adresseBrev);

        return arbeidsstedBrev;
    }

    private ArbeidsstedType mapIkkeFysiskArbeidssted(Arbeidssted arbeidssted) throws TekniskException {
        ArbeidsstedType arbeidsstedBrev = new ArbeidsstedType();
        arbeidsstedBrev.setNavn(arbeidssted.getNavn());
        arbeidsstedBrev.setIkkeFysiskArbeidssted("true");
        arbeidsstedBrev.setYrkesgruppe(YrkesgruppeKode.fromValue(arbeidssted.getYrkesgruppe().getKode()));

        AdresseType3 adresseBrev = new AdresseType3();
        adresseBrev.setLand(hentIso3Landkode(arbeidssted.getLandkode()));
        arbeidsstedBrev.setAdresse(adresseBrev);
        return arbeidsstedBrev;
    }

    private BostedsadresseType mapBostedAdresse(StrukturertAdresse bosted) throws TekniskException {
        BostedsadresseType bostedsadresse = new BostedsadresseType();
        bostedsadresse.setGatenavn(bosted.gatenavn);
        bostedsadresse.setHusnummer(bosted.husnummer);
        bostedsadresse.setPostnummer(bosted.postnummer);
        bostedsadresse.setPoststed(bosted.poststed);
        bostedsadresse.setRegion(bosted.region);
        bostedsadresse.setLand(hentIso3Landkode(bosted.landkode));
        bostedsadresse.setAdresseType(BostedsadresseTypeKode.BOSTEDSLAND); // Lev1 kun bostedsland
        return bostedsadresse;
    }

    private ForetakListeType mapForetakliste(List<AvklartVirksomhet> arbeidsgivendeVirksomheter) throws TekniskException {
        ForetakListeType foretakListe = new ForetakListeType();
        for (AvklartVirksomhet virksomhet : arbeidsgivendeVirksomheter) {
            ForetakType foretak = new ForetakType();
            foretak.setNavn(virksomhet.navn);
            foretak.setOrgnummer(virksomhet.orgnr);
            foretak.setYrkesaktivitet(YrkesaktivitetsKode.valueOf(virksomhet.yrkesaktivitet.getKode()));
            foretak.setHovedvirksomhet("true");  // Kun et foretak i Lev1

            StrukturertAdresse adresse = (StrukturertAdresse) virksomhet.adresse;
            AdresseType adresseBrev = new AdresseType();

            adresseBrev.setLand(hentIso3Landkode(adresse.landkode));
            adresseBrev.setAdresselinje1(adresse.gatenavn);
            adresseBrev.setAdresselinje2(adresse.poststed);
            adresseBrev.setAdresselinje3(adresse.postnummer);

            foretak.setAdresse(adresseBrev);
            foretakListe.getForetak().add(foretak);
        }

        return foretakListe;
    }

    private SelvstendigNaeringsvirksomhetListeType mapSelvstendigvirksometliste(List<AvklartVirksomhet> virksomheter) throws TekniskException {
        SelvstendigNaeringsvirksomhetListeType selvstendigeVirksomheter = new SelvstendigNaeringsvirksomhetListeType();
        for (AvklartVirksomhet virksomhet : virksomheter) {
            SelvstendigNaeringsvirksomhetType selvstendigVirksomhet = new SelvstendigNaeringsvirksomhetType();
            selvstendigVirksomhet.setNavn(virksomhet.navn);
            selvstendigVirksomhet.setOrgnummer(virksomhet.orgnr);

            AdresseType2 adresseBrev = new AdresseType2();
            StrukturertAdresse adresse = (StrukturertAdresse) virksomhet.adresse;

            adresseBrev.setLand(hentIso3Landkode(adresse.landkode));
            adresseBrev.setAdresselinje1(adresse.gatenavn);
            adresseBrev.setAdresselinje2(adresse.poststed);
            adresseBrev.setAdresselinje3(adresse.postnummer);

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
            lovvalgsperiodeBrev.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(periode.getFom()));
            lovvalgsperiodeBrev.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(periode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Feil ved konvertering");
        }
        return lovvalgsperiodeBrev;
    }

    private UnntakFraLovvalgslandType mapUnntaksland(Lovvalgsperiode periode) throws TekniskException {
        UnntakFraLovvalgslandType unntakFraLovvalgslandType = new UnntakFraLovvalgslandType();
        String land = periode.getUnntakFraLovvalgsland().getKode();
        unntakFraLovvalgslandType.getUnntakFraLovvalgsland().add(hentIso3Landkode(land));
        return unntakFraLovvalgslandType;
    }

    //A001 krever ISO-3
    private static String hentIso3Landkode(String landkode) throws TekniskException {
        return landkode.length() == 2 ? LandkoderUtils.tilIso3(landkode) : landkode;
    }
}
