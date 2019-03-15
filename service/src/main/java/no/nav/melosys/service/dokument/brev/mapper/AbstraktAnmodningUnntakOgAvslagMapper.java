package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000081.BrevdataType;
import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev._000081.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000081.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.InngangsvilkaarBegrunnelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*;

/**
 * Anmodning om unntak og avslag deler samme mal.
 */
abstract class AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000081.xsd";

    static final String JA = "true";

    abstract Fag mapArt161(Fag fag, Behandlingsresultat resultat, BrevData brevData) throws TekniskException;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag(behandling, resultat, (BrevDataAnmodningUnntakOgAvslag) brevData);
        fag = mapArt161(fag, resultat, brevData);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntakOgAvslag brevData) throws TekniskException {
        Fag fag = new Fag();

        if (behandling.getFagsak().getType() == Sakstyper.EU_EOS) {
            // Respons fra regelmodulen skiller ikke mellom begrunnelser for 883/2004 (MELOSYS-1863)
            fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        } else {
            throw new TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004");
        }

        fag.setForetakNavn(brevData.hovedvirksomhet.navn);
        // Frilansaktivitet håndteres ikke i Lev 1
        if (brevData.hovedvirksomhet.isSelvstendigForetak()) {
            fag.setYrkesaktivitet(YrkesaktivitetsKode.SELVSTENDIG);
        } else {
            fag.setYrkesaktivitet(YrkesaktivitetsKode.LOENNET_ARBEID);
        }

        fag.setArbeidsland(brevData.arbeidsland.getBeskrivelse());
        fag.setLovvalgsperiode(lagLovvalgsperiodeType(resultat));

        Set<VilkaarBegrunnelse> art121Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART12_1);
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        Set<VilkaarBegrunnelse> art121ForutgåendeBegrunnelser = hentVilkaarbegrunnelser(resultat, ART12_1_FORUTGAAENDE_MEDLEMSKAP);
        fag.setArt121ForutgåendeBegrunnelse(mapArt121ForutgaaendeBegrunnelseType(art121ForutgåendeBegrunnelser));

        Set<VilkaarBegrunnelse> art122Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART12_2);
        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(art122Begrunnelser));

        Set<VilkaarBegrunnelse> art122NormalVirksomhetBegrunnelse = hentVilkaarbegrunnelser(resultat, ART12_2_NORMALT_DRIVER_VIRKSOMHET);
        fag.setArt122NormalVirksomhetBegrunnelse(mapArt122NormalVirksomhetBegrunnelseType(art122NormalVirksomhetBegrunnelse));

        return fag;
    }

    Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Behandlingsresultat resultat, Vilkaar vilkaarType) {
        return resultat.getVilkaarsresultater().stream()
            .filter(vr -> vr.getVilkaar() == vilkaarType)
            .flatMap(vr -> vr.getBegrunnelser().stream())
            .collect(Collectors.toSet());
    }

    void validerFritekstbegrunnelse(String fritekst) throws TekniskException {
        if (StringUtils.isEmpty(fritekst)) {
            throw new TekniskException("Ingen fritekstbegrunnelse satt for Artikkel 16.1");
        }
    }

    private LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = resultat.getLovvalgsperioder()
            .stream().findFirst().orElseThrow(() -> new TekniskException("Ingen lovvalgsperiode funnet for behandlingsresultat"));

        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();

        Landkoder unntakFraLovvalgsland = lovvalgsperiode.getUnntakFraLovvalgsland();
        if (unntakFraLovvalgsland != null) {
            lovvalgsperiodeType.setUnntakFraLovvalgsland(unntakFraLovvalgsland.getKode());
        }
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return lovvalgsperiodeType;
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
