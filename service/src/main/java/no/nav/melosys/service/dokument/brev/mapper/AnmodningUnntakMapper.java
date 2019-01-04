package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000081.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AnmodningBegrunnelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.begrunnelse.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.VilkaarType.*;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class AnmodningUnntakMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000081.xsd";

    private static final String JA = "true";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag(behandling, resultat, (BrevDataAnmodningUnntak) brevData);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntak brevData) throws TekniskException {
        Fag fag = new Fag();

        if (brevData.hovedvirksomhet == null) {
            throw new TekniskException("Trenger minst en norsk virksomhet for ART16.1");
        }
        fag.setForetakNavn(brevData.hovedvirksomhet.navn);
        SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
        // TODO: Frilansaktivitet håndteres ikke i Lev 1
        if (soeknadDokument.selvstendigArbeid.erSelvstendig) {
            fag.setYrkesaktivitet(YrkesaktivitetsKode.SELVSTENDIG);
        } else {
            fag.setYrkesaktivitet(YrkesaktivitetsKode.LOENNET_ARBEID);
        }

        if (behandling.getFagsak().getType() == Fagsakstype.EU_EØS) {
            // TODO: Respons fra regelmodulen skiller ikke mellom begrunnelser for 883/2004 (MELOSYS-1863)
            fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        } else {
            throw new TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004");
        }
        fag.setLovvalgsperiode(lagLovvalgsperiodeType(resultat));

        Set<VilkaarBegrunnelse> art121Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART12_1);
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        Set<VilkaarBegrunnelse> art121ForutgåendeBegrunnelser = hentVilkaarbegrunnelser(resultat, ART12_1_FORUTGÅENDE_MEDLEMSKAP);
        fag.setArt121ForutgåendeBegrunnelse(mapArt121ForutgaaendeBegrunnelseType(art121ForutgåendeBegrunnelser));

        Set<VilkaarBegrunnelse> art122Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART12_2);
        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(art122Begrunnelser));

        Set<VilkaarBegrunnelse> art122NormalVirksomhetBegrunnelse = hentVilkaarbegrunnelser(resultat, ART12_2_NORMALT_DRIVER_VIRKSOMHET);
        fag.setArt122NormalVirksomhetBegrunnelse(mapArt122NormalVirksomhetBegrunnelseType(art122NormalVirksomhetBegrunnelse));

        Set<VilkaarBegrunnelse> art161AnmodningBegrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART16_1);
        VilkaarBegrunnelse vilkaarBegrunnelse = art161AnmodningBegrunnelser.stream()
                        .findFirst().orElseThrow(() -> new TekniskException("Ingen begrunnelse funnet for Artikkel 16.1"));
        fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.valueOf(vilkaarBegrunnelse.getKode()));

        if (fag.getArt161AnmodningBegrunnelse() == Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN) {
            if (StringUtils.isEmpty(brevData.fritekst)) {
                throw new TekniskException("Ingen fritekstbegrunnelse satt for Artikkel 16.1");
            }
            fag.setAnmodningFritekst(brevData.fritekst);
        }
        return fag;
    }

    private Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Behandlingsresultat resultat, VilkaarType vilkaarType) {
        return resultat.getVilkaarsresultater().stream()
                .filter(vr -> vr.getVilkaar() == vilkaarType)
                .flatMap(vr -> vr.getBegrunnelser().stream())
                .collect(Collectors.toSet());
    }

    private LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = resultat.getLovvalgsperioder()
            .stream().findFirst().orElseThrow(() -> new TekniskException("Ingen lovvalgsperiode funnet for behandlingsresultat"));

        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        lovvalgsperiodeType.setUnntakFraLovvalgsland(lovvalgsperiode.getLovvalgsland().getKode());
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return lovvalgsperiodeType;
    }

    private Art121BegrunnelseType lagArt121BegrunnelseType() {
        Art121BegrunnelseType art121BegrunnelseType = new Art121BegrunnelseType();
        art121BegrunnelseType.setUtsendelseOver24Mn("");
        art121BegrunnelseType.setErstatterAnnen("");
        art121BegrunnelseType.setIkkeUtsendtPåOppdragForAg("");
        art121BegrunnelseType.setIkkeOmfattetLengeNokINorgeFør("");
        art121BegrunnelseType.setUnder2MnSidenForrigeUtsendingTilSammeLand("");
        art121BegrunnelseType.setIkkeVesentligVirksomhet("");
        return art121BegrunnelseType;
    }

    private Art121BegrunnelseType mapArt121BegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) {
        Art121BegrunnelseType art121BegrunnelseType = lagArt121BegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Artikkel12_1 artikkel12_1 = Artikkel12_1.valueOf(vilkaarBegrunnelse.getKode());
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
            }
        }
        return art121BegrunnelseType;
    }

    private Art121ForutgaaendeBegrunnelseType lagArt121ForutgaaendeBegrunnelseType() {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = new Art121ForutgaaendeBegrunnelseType();
        art121ForutgaaendeBegrunnelseType.setUntattMedlemskap("");
        art121ForutgaaendeBegrunnelseType.setFolkeregistrertIkkeArbeidetINorge("");
        art121ForutgaaendeBegrunnelseType.setIkkeFolkeregistrertEllerArbeidetINorge("");
        return art121ForutgaaendeBegrunnelseType;
    }

    private Art121ForutgaaendeBegrunnelseType mapArt121ForutgaaendeBegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = lagArt121ForutgaaendeBegrunnelseType();

        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            ForutgaaendeMedlemskap forutgaaendeMedlemskap = ForutgaaendeMedlemskap.valueOf(vilkaarBegrunnelse.getKode());
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
            }
        }
        return art121ForutgaaendeBegrunnelseType;
    }

    private Art122BegrunnelseType lagArt122BegrunnelseType() {
        Art122BegrunnelseType art122BegrunnelseType = new Art122BegrunnelseType();
        art122BegrunnelseType.setUtsendelseOver24Mn("");
        art122BegrunnelseType.setIkkeLignendeVirksomhet("");
        art122BegrunnelseType.setNormaltIkkeDriftINorge("");
        return art122BegrunnelseType;
    }

    private Art122BegrunnelseType mapArt122BegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) {
        Art122BegrunnelseType art122BegrunnelseType = lagArt122BegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Artikkel12_2 artikkel12_2 = Artikkel12_2.valueOf(vilkaarBegrunnelse.getKode());
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
            }
        }
        return art122BegrunnelseType;
    }

    private Art122NormalVirksomhetBegrunnelseType lagArt122NormalVirksomhetBegrunnelseType() {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = new Art122NormalVirksomhetBegrunnelseType();
        art122NormalVirksomhetBegrunnelseType.setIkkeForutgåendeDrift("");
        art122NormalVirksomhetBegrunnelseType.setHarIkkeNødvendigInfrastruktur("");
        art122NormalVirksomhetBegrunnelseType.setOpprettholderIkkeLisenserAutorisasjon("");
        return art122NormalVirksomhetBegrunnelseType;
    }

    private Art122NormalVirksomhetBegrunnelseType mapArt122NormalVirksomhetBegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = lagArt122NormalVirksomhetBegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            NormaltDriverVirksomhet normaltDriverVirksomhet = NormaltDriverVirksomhet.valueOf(vilkaarBegrunnelse.getKode());
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
