package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev._000081.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.InngangsvilkaarBegrunnelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*;

/**
 * Anmodning om unntak og avslag har to forskjellige maler med samme innhold
 */
abstract class AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    private static final Logger log = LoggerFactory.getLogger(AbstraktAnmodningUnntakOgAvslagMapper.class);

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

        fag.setArbeidsland(brevData.arbeidsland);
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

    private static Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Behandlingsresultat resultat, Vilkaar vilkaarType) {
        return resultat.getVilkaarsresultater().stream()
            .filter(vr -> vr.getVilkaar() == vilkaarType)
            .flatMap(vr -> vr.getBegrunnelser().stream())
            .collect(Collectors.toSet());
    }

    static String validerFritekstbegrunnelse(String begrunnelse) throws TekniskException {
        if (!StringUtils.isEmpty(begrunnelse)) {
            return begrunnelse;
        } else {
            throw new TekniskException("Ingen fritekstbegrunnelse satt for Artikkel 16.1");
        }
    }

    Optional<Vilkaarsresultat> hentFørsteGyldigeVilkaarsresultatForArt16(Behandlingsresultat resultat) {
        return resultat.getVilkaarsresultater().stream()
            .filter(v -> v.getVilkaar().equals(FO_883_2004_ART16_1) && !v.getBegrunnelser().isEmpty())
            .findFirst();
    }

    private static LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = resultat.getLovvalgsperioder()
            .stream().findFirst().orElseThrow(() -> new TekniskException("Ingen lovvalgsperiode funnet for behandlingsresultat" + resultat.getId()));

        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();

        Landkoder unntakFraLovvalgsland = lovvalgsperiode.getUnntakFraLovvalgsland();
        if (unntakFraLovvalgsland != null) {
            lovvalgsperiodeType.setUnntakFraLovvalgsland(unntakFraLovvalgsland.getBeskrivelse());
        }
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            log.error("", e);
        }
        return lovvalgsperiodeType;
    }
}
