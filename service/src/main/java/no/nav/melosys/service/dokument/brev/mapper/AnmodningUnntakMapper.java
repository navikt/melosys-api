package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import no.nav.dok.melosysbrev._000084.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.InngangsvilkaarBegrunnelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
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

    static final BiMap<LovvalgBestemmelse, BestemmelseDetSoekesUnntakFraKode> BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP =
        HashBiMap.create(ImmutableMap.<LovvalgBestemmelse, BestemmelseDetSoekesUnntakFraKode>builder()
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_3_A)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_3_B)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_3_C)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_3_D)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_3_E)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_4)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_11_4_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_A)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_1)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_2)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_3)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_1_B_4)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_2_A)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_2_B)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_3)
            .put(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4, BestemmelseDetSoekesUnntakFraKode.FO_883_2004_ART_13_4)
            .build());

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat,
                                BrevData brevDataFelles, boolean enableBehandleAlleSaker) throws JAXBException, SAXException {
        BrevDataAnmodningUnntak brevdata = (BrevDataAnmodningUnntak) brevDataFelles;
        Fag fag = mapFag(behandling, resultat, brevdata);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntak brevData) {
        Fag fag = new Fag();
        if (behandling.getFagsak().getType() == Sakstyper.EU_EOS) {
            // Respons fra regelmodulen skiller ikke mellom begrunnelser for 883/2004 (MELOSYS-1863)
            fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        } else {
            throw new TekniskException(behandling.getFagsak().getType() + " støttes ikke.");
        }

        fag.setForetakNavn(brevData.hovedvirksomhet.navn);
        fag.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(brevData.yrkesaktivitet.getKode()));

        Anmodningsperiode anmodningsperiode = resultat.hentAnmodningsperiode();
        fag.setArbeidsland(brevData.arbeidsland);
        fag.setLovvalgsperiode(lagLovvalgsperiodeType(anmodningsperiode));

        if (resultat.manglerVilkår(FO_883_2004_ART12_1) && resultat.manglerVilkår(FO_883_2004_ART12_2)) {
            // bestemmelseDetSoekesUnntakFra støttes feilaktig bare i forkortet flyt, direkte anmodning om art.16
            Optional.ofNullable(anmodningsperiode.getUnntakFraBestemmelse())
                .map(BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP::get)
                .ifPresent(fag::setBestemmelseDetSoekesUnntakFra);
        }

        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_1)));
        fag.setArt121ForutgåendeBegrunnelse(
            mapArt121ForutgaaendeBegrunnelseType(resultat.hentVilkaarbegrunnelser(ART12_1_FORUTGAAENDE_MEDLEMSKAP)));

        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_2)));
        fag.setArt122NormalVirksomhetBegrunnelse(mapArt122NormalVirksomhetBegrunnelseType(
            resultat.hentVilkaarbegrunnelser(ART12_2_NORMALT_DRIVER_VIRKSOMHET)));

        mapAnmodningBegrunnelser(brevData.anmodningBegrunnelser).ifPresent(fag::setArt161AnmodningBegrunnelse);

        mapAnmodningUtenArt12Begrunnelser(brevData.anmodningUtenArt12Begrunnelser).ifPresent(fag::setArt161AnmodningUtenArt12Begrunnelse);

        fag.setAnmodningFritekst(brevData.anmodningFritekst);

        fag.setBegrunnelseFritekst(brevData.fritekst);

        return fag;
    }

    LovvalgsperiodeType lagLovvalgsperiodeType(Anmodningsperiode anmodningsperiode) {
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
