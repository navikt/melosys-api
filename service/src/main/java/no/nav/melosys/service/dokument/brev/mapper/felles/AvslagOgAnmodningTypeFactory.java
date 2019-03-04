package no.nav.melosys.service.dokument.brev.mapper.felles;

import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000081.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art121BegrunnelseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art121ForutgaaendeBegrunnelseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art122BegrunnelseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art122NormalVirksomhetBegrunnelseType;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public final class AvslagOgAnmodningTypeFactory {

    private AvslagOgAnmodningTypeFactory() {
    }

    public static LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) throws TekniskException {
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

    public static Art121BegrunnelseType lagArt121BegrunnelseType() {
        Art121BegrunnelseType art121BegrunnelseType = new Art121BegrunnelseType();
        art121BegrunnelseType.setUtsendelseOver24Mn("");
        art121BegrunnelseType.setErstatterAnnen("");
        art121BegrunnelseType.setIkkeUtsendtPåOppdragForAg("");
        art121BegrunnelseType.setIkkeOmfattetLengeNokINorgeFør("");
        art121BegrunnelseType.setUnder2MnSidenForrigeUtsendingTilSammeLand("");
        art121BegrunnelseType.setIkkeVesentligVirksomhet("");
        return art121BegrunnelseType;
    }

    public static Art121ForutgaaendeBegrunnelseType lagArt121ForutgaaendeBegrunnelseType() {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = new Art121ForutgaaendeBegrunnelseType();
        art121ForutgaaendeBegrunnelseType.setUntattMedlemskap("");
        art121ForutgaaendeBegrunnelseType.setFolkeregistrertIkkeArbeidetINorge("");
        art121ForutgaaendeBegrunnelseType.setIkkeFolkeregistrertEllerArbeidetINorge("");
        return art121ForutgaaendeBegrunnelseType;
    }

    public static Art122BegrunnelseType lagArt122BegrunnelseType() {
        Art122BegrunnelseType art122BegrunnelseType = new Art122BegrunnelseType();
        art122BegrunnelseType.setUtsendelseOver24Mn("");
        art122BegrunnelseType.setIkkeLignendeVirksomhet("");
        art122BegrunnelseType.setNormaltIkkeDriftINorge("");
        return art122BegrunnelseType;
    }

    public static Art122NormalVirksomhetBegrunnelseType lagArt122NormalVirksomhetBegrunnelseType() {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = new Art122NormalVirksomhetBegrunnelseType();
        art122NormalVirksomhetBegrunnelseType.setIkkeForutgåendeDrift("");
        art122NormalVirksomhetBegrunnelseType.setHarIkkeNødvendigInfrastruktur("");
        art122NormalVirksomhetBegrunnelseType.setOpprettholderIkkeLisenserAutorisasjon("");
        return art122NormalVirksomhetBegrunnelseType;
    }
}
