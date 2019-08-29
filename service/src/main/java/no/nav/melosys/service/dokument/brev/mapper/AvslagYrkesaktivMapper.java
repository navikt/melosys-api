package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000081.BrevdataType;
import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev._000081.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AvslagBegrunnelse;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.xml.sax.SAXException;

public class AvslagYrkesaktivMapper extends AbstraktAnmodningUnntakOgAvslagMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000081.xsd";

    private static final String JA = "true";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat,
                                BrevData brevData) throws JAXBException, SAXException, TekniskException {
        BrevDataAnmodningUnntakOgAvslag brevDataAnmodningUnntakOgAvslag = (BrevDataAnmodningUnntakOgAvslag) brevData;
        Fag fag = mapFag(behandling, resultat, brevDataAnmodningUnntakOgAvslag);

        if (brevDataAnmodningUnntakOgAvslag.anmodningsperiodeSvar.isPresent()) {
            brevDataAnmodningUnntakOgAvslag.anmodningsperiodeSvar.ifPresent(aps ->
                mapArt161AvslagFraAnmodningsperiode(fag, aps));
        } else {
            mapArt161Avslag(fag, resultat);
        }

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private void mapArt161AvslagFraAnmodningsperiode(Fag fag, AnmodningsperiodeSvar svar) {
        fag.setBegrunnelseFritekst(svar.getBegrunnelseFritekst());

        Art161AvslagBegrunnelse avslagBegrunnelse = lagTomArt161AvslagBegrunnelse();
        avslagBegrunnelse.setUtlAvslaarAvtale(JA);
        fag.setArt161AvslagBegrunnelse(avslagBegrunnelse);
    }

    @Override
    Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntakOgAvslag brevData) throws TekniskException {
        Fag fag = super.mapFag(behandling, resultat, brevData);
        fag.setAvslag(JA);
        return fag;
    }

    private void mapArt161Avslag(Fag fag, Behandlingsresultat resultat) throws TekniskException {
        Optional<Vilkaarsresultat> vilkaarsresultat = hentFørsteGyldigeVilkaarsresultatForArt16(resultat);
        Set<VilkaarBegrunnelse> art161Begrunnelser = vilkaarsresultat.map(Vilkaarsresultat::getBegrunnelser).orElse(Collections.emptySet());
        Art161AvslagBegrunnelse art161AvslagBegrunnelser = lagTomArt161AvslagBegrunnelse();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : art161Begrunnelser) {
            Art16_1_avslag artikkel161AvslagKode = Art16_1_avslag.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel161AvslagKode) {
                case OVER_12_MD_UTL_ARBEIDSGIVER:
                case OVER_5_AAR:
                    art161AvslagBegrunnelser.setOver5Aar(JA);
                    break;
                case INGEN_SPESIELLE_FORHOLD:
                    art161AvslagBegrunnelser.setIngenSpesielleForhold(JA);
                    break;
                case SAERLIG_AVSLAGSGRUNN:
                    art161AvslagBegrunnelser.setSaerligAvslagsgrunn(JA);
                    Vilkaarsresultat v = vilkaarsresultat.orElseThrow(IllegalStateException::new);
                    fag.setBegrunnelseFritekst(validerFritekstbegrunnelse(v.getBegrunnelseFritekst()));
                    break;
                case SOEKT_FOR_SENT:
                    art161AvslagBegrunnelser.setSoektForSent(JA);
                    break;
                default:
                    throw new TekniskException(artikkel161AvslagKode + " støttes ikke.");
            }
        }

        fag.setArt161AvslagBegrunnelse(art161AvslagBegrunnelser);
    }

    private static Art161AvslagBegrunnelse lagTomArt161AvslagBegrunnelse() {
        return Art161AvslagBegrunnelse.builder().withIngenSpesielleForhold("")
            .withOver5Aar("")
            .withSaerligAvslagsgrunn("")
            .withUtlAvslaarAvtale("")
            .withSoektForSent("").build();
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
