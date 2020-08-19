package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000084.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.InngangsvilkaarBegrunnelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*;

public class AnmodningUnntakMapper implements BrevDataMapper {

    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakMapper.class);

    private static final String XSD_LOCATION = "melosysbrev/melosys_000084.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat,
                                BrevData brevDataFelles) throws JAXBException, SAXException, TekniskException {
        BrevDataAnmodningUnntak brevdata = (BrevDataAnmodningUnntak) brevDataFelles;
        Fag fag = mapFag(behandling, resultat, brevdata);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntak brevData) throws TekniskException {
        Fag fag = new Fag();
        if (behandling.getFagsak().getType() == Sakstyper.EU_EOS) {
            // Respons fra regelmodulen skiller ikke mellom begrunnelser for 883/2004 (MELOSYS-1863)
            fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        } else {
            throw new TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004");
        }

        fag.setForetakNavn(brevData.hovedvirksomhet.navn);
        fag.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(brevData.yrkesaktivitet.getKode()));

        fag.setArbeidsland(brevData.arbeidsland);
        fag.setLovvalgsperiode(lagLovvalgsperiodeType(resultat));

        Set<VilkaarBegrunnelse> art121Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_1);
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        Set<VilkaarBegrunnelse> art121ForutgåendeBegrunnelser = resultat.hentVilkaarbegrunnelser(ART12_1_FORUTGAAENDE_MEDLEMSKAP);
        fag.setArt121ForutgåendeBegrunnelse(mapArt121ForutgaaendeBegrunnelseType(art121ForutgåendeBegrunnelser));

        Set<VilkaarBegrunnelse> art122Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_2);
        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(art122Begrunnelser));

        Set<VilkaarBegrunnelse> art122NormalVirksomhetBegrunnelse = resultat.hentVilkaarbegrunnelser(ART12_2_NORMALT_DRIVER_VIRKSOMHET);
        fag.setArt122NormalVirksomhetBegrunnelse(mapArt122NormalVirksomhetBegrunnelseType(art122NormalVirksomhetBegrunnelse));

        mapAnmodningBegrunnelser(brevData.anmodningBegrunnelser).ifPresent(fag::setArt161AnmodningBegrunnelse);

        mapAnmodningUtenArt12Begrunnelser(brevData.anmodningUtenArt12Begrunnelser).ifPresent(fag::setArt161AnmodningUtenArt12Begrunnelse);

        fag.setAnmodningFritekst(brevData.anmodningFritekst);

        fag.setBegrunnelseFritekst(brevData.fritekst);

        Anmodningsperiode anmodningsperiode = resultat.hentValidertAnmodningsperiode();
        if (anmodningsperiode.erUnntakFraArtikkel13()) {
            fag.setBestemmelseDetSoekesUnntakFra(tilBestemmelseBrev(anmodningsperiode.getUnntakFraBestemmelse()));
        }

        return fag;
    }

    private static BestemmelseDetSoekesUnntakFraKode tilBestemmelseBrev(LovvalgBestemmelse bestemmelse) {
        if (bestemmelse instanceof Lovvalgbestemmelser_883_2004) {
            switch ((Lovvalgbestemmelser_883_2004) bestemmelse) {
                case FO_883_2004_ART13_1A:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_A;
                case FO_883_2004_ART13_1B1:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_1;
                case FO_883_2004_ART13_1B2:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_2;
                case FO_883_2004_ART13_1B3:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_3;
                case FO_883_2004_ART13_1B4:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_4;
                case FO_883_2004_ART13_2A:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_2_A;
                case FO_883_2004_ART13_2B:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_2_B;
                case FO_883_2004_ART13_3:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_3;
                case FO_883_2004_ART13_4:
                    return BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_4;
                default:
                    throw new UnsupportedOperationException(
                        String.format("BestemmelseDetSoekesUnntakFraKode støtter ikke %s", bestemmelse)
                    );
            }
        }

        throw new UnsupportedOperationException(
            String.format("Kan ikke konvertere bestemmelse av type %s til BestemmelseDetSoekesUnntakFraKode", bestemmelse)
        );
    }

    LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) {
        Anmodningsperiode anmodningsperiode = resultat.hentValidertAnmodningsperiode();
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();

        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(anmodningsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(anmodningsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            log.error("", e);
        }
        return lovvalgsperiodeType;
    }

    private static JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        return factory.createBrevdata(brevdataType);
    }
}