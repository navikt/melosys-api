package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000081.BrevdataType;
import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev._000081.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.AvslagOgAnmodningTypeFactory.*;

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
        SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
        // Frilansaktivitet håndteres ikke i Lev 1
        if (soeknadDokument.selvstendigArbeid.erSelvstendig) {
            fag.setYrkesaktivitet(YrkesaktivitetsKode.SELVSTENDIG);
        } else {
            fag.setYrkesaktivitet(YrkesaktivitetsKode.LOENNET_ARBEID);
        }

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

    private Art121BegrunnelseType mapArt121BegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art121BegrunnelseType art121BegrunnelseType = lagArt121BegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_1_Begrunnelser artikkel12_1 = Art12_1_Begrunnelser.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel12_1) {
                case UTSENDELSE_OVER_24_MN:
                    art121BegrunnelseType.setUtsendelseOver24Mn(JA);
                    break;
                case ERSTATTER_ANNEN:
                    art121BegrunnelseType.setErstatterAnnen(JA);
                    break;
                case IKKE_UTSENDT_PAA_OPPDRAG_FOR_AG:
                    art121BegrunnelseType.setIkkeUtsendtPåOppdragForAg(JA);
                    break;
                case IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER:
                    art121BegrunnelseType.setIkkeOmfattetLengeNokINorgeFør(JA);
                    break;
                case UNDER_2_MN_SIDEN_FORRIGE_UTSENDING_TIL_SAMME_LAND:
                    art121BegrunnelseType.setUnder2MnSidenForrigeUtsendingTilSammeLand(JA);
                    break;
                case IKKE_VESENTLIG_VIRKSOMHET:
                    art121BegrunnelseType.setIkkeVesentligVirksomhet(JA);
                    break;
                default:
                    throw new TekniskException(artikkel12_1 + " støttes ikke.");
            }
        }
        return art121BegrunnelseType;
    }

    private Art121ForutgaaendeBegrunnelseType mapArt121ForutgaaendeBegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = lagArt121ForutgaaendeBegrunnelseType();

        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_1_Forutgaaende_Medl_Begrunnelse forutgaaendeMedlemskap = Art12_1_Forutgaaende_Medl_Begrunnelse.valueOf(vilkaarBegrunnelse.getKode());
            switch (forutgaaendeMedlemskap) {
                case UNNTATT_MEDLEMSKAP:
                    art121ForutgaaendeBegrunnelseType.setUntattMedlemskap(JA);
                    break;
                case FOLKEREGISTRERT_IKKE_ARBEIDET_I_NORGE:
                    art121ForutgaaendeBegrunnelseType.setFolkeregistrertIkkeArbeidetINorge(JA);
                    break;
                case IKKE_FOLKEREGISTRERT_ELLER_ARBEIDET_I_NORGE:
                    art121ForutgaaendeBegrunnelseType.setIkkeFolkeregistrertEllerArbeidetINorge(JA);
                    break;
                default:
                    throw new TekniskException(forutgaaendeMedlemskap + " støttes ikke.");
            }
        }
        return art121ForutgaaendeBegrunnelseType;
    }

    private Art122BegrunnelseType mapArt122BegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art122BegrunnelseType art122BegrunnelseType = lagArt122BegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_2_Begrunnelser artikkel12_2 = Art12_2_Begrunnelser.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel12_2) {
                case UTSENDELSE_OVER_24_MN:
                    art122BegrunnelseType.setUtsendelseOver24Mn(JA);
                    break;
                case IKKE_LIGNENDE_VIRKSOMHET:
                    art122BegrunnelseType.setIkkeLignendeVirksomhet(JA);
                    break;
                case NORMALT_IKKE_DRIFT_NORGE:
                    art122BegrunnelseType.setNormaltIkkeDriftINorge(JA);
                    break;
                default:
                    throw new TekniskException(artikkel12_2 + "  støttes ikke.");
            }
        }
        return art122BegrunnelseType;
    }

    private Art122NormalVirksomhetBegrunnelseType mapArt122NormalVirksomhetBegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = lagArt122NormalVirksomhetBegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Normaltdrivervirksomhet normaltDriverVirksomhet = Normaltdrivervirksomhet.valueOf(vilkaarBegrunnelse.getKode());
            switch (normaltDriverVirksomhet) {
                case IKKE_FORUTGAAENDE_DRIFT:
                    art122NormalVirksomhetBegrunnelseType.setIkkeForutgåendeDrift(JA);
                    break;
                case HAR_IKKE_NØDVENDIG_INFRASTRUKTUR:
                    art122NormalVirksomhetBegrunnelseType.setHarIkkeNødvendigInfrastruktur(JA);
                    break;
                case OPPRETTHOLDER_IKKE_LISENSER_AUTORISASJON:
                    art122NormalVirksomhetBegrunnelseType.setOpprettholderIkkeLisenserAutorisasjon(JA);
                    break;
                default:
                    throw new TekniskException(normaltDriverVirksomhet + "  støttes ikke.");
            }
        }
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
