package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000081.*;
import no.nav.dok.melosysbrev._000081.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class AnmodningUnntakMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000081.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag();
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag() {
        Fag fag = new Fag();
        fag.setForetakNavn("TODO");
        fag.setYrkesaktivitet(YrkesaktivitetsKode.FRILANSER); // FIXME
        fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER); // FIXME
        fag.setAnmodningFritekst("TODO"); // FIXME Hvis Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN
        fag.setLovvalgsperiode(lagLovvalgsperiodeType());
        fag.setArt121Begrunnelse(lagArt121BegrunnelseType());
        fag.setArt121ForutgåendeBegrunnelse(lagArt121ForutgaaendeBegrunnelseType());
        fag.setArt122Begrunnelse(lagArt122BegrunnelseType());
        fag.setArt122NormalVirksomhetBegrunnelse(lagArt122NormalVirksomhetBegrunnelseType());
        fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.ERSTATTER_EN_ANNEN_UNDER_5_AAR); // FIXME
        return fag;
    }

    private LovvalgsperiodeType lagLovvalgsperiodeType() {
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        lovvalgsperiodeType.setUnntakFraLovvalgsland("TODO");
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return lovvalgsperiodeType;
    }

    private Art121BegrunnelseType lagArt121BegrunnelseType() {
        Art121BegrunnelseType art121BegrunnelseType = new Art121BegrunnelseType();
        // FIXME EmptyBoolean "true", "false", ""
        art121BegrunnelseType.setUtsendelseOver24Mn("");
        art121BegrunnelseType.setErstatterAnnen("");
        art121BegrunnelseType.setIkkeUtsendtPåOppdragForAg("");
        art121BegrunnelseType.setIkkeOmfattetLengeNokINorgeFør("");
        art121BegrunnelseType.setUnder2MnSidenForrigeUtsendingTilSammeLand("");
        art121BegrunnelseType.setIkkeVesentligVirksomhet("");
        return art121BegrunnelseType;
    }

    private Art121ForutgaaendeBegrunnelseType lagArt121ForutgaaendeBegrunnelseType() {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = new Art121ForutgaaendeBegrunnelseType();
        // FIXME EmptyBoolean "true", "false", ""
        art121ForutgaaendeBegrunnelseType.setUntattMedlemskap("");
        art121ForutgaaendeBegrunnelseType.setFolkeregistrertIkkeArbeidetINorge("");
        art121ForutgaaendeBegrunnelseType.setIkkeFolkeregistrertEllerArbeidetINorge("");
        return art121ForutgaaendeBegrunnelseType;
    }

    private Art122BegrunnelseType lagArt122BegrunnelseType() {
        Art122BegrunnelseType art122BegrunnelseType = new Art122BegrunnelseType();
        // FIXME EmptyBoolean "true", "false", ""
        art122BegrunnelseType.setUtsendelseOver24Mn("");
        art122BegrunnelseType.setIkkeLignendeVirksomhet("");
        art122BegrunnelseType.setNormaltIkkeDriftINorge("");
        return art122BegrunnelseType;
    }

    private Art122NormalVirksomhetBegrunnelseType lagArt122NormalVirksomhetBegrunnelseType() {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = new Art122NormalVirksomhetBegrunnelseType();
        // FIXME EmptyBoolean "true", "false", ""
        art122NormalVirksomhetBegrunnelseType.setIkkeForutgåendeDrift("");
        art122NormalVirksomhetBegrunnelseType.setHarIkkeNødvendigInfrastruktur("");
        art122NormalVirksomhetBegrunnelseType.setOpprettholderIkkeLisenserAutorisasjon("");
        return art122NormalVirksomhetBegrunnelseType;
    }

    @SuppressWarnings("Duplicates")
    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        return factory.createBrevdata(brevdataType);
    }

}
