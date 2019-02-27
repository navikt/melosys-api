package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;

import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AvslagBegrunnelse;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.kodeverk.Art16_1_Avslag__Begrunnelser;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART16_1;

public class AvslagBrukerMapper extends AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    @Override
    Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntakOgAvslag brevData) throws TekniskException {
        Fag fag = super.mapFag(behandling, resultat, brevData);
        fag.setAvslag(JA);
        return fag;
    }

    @Override
    Fag mapArt161(Fag fag, Behandlingsresultat resultat, BrevData brevData) throws TekniskException {
        Set<VilkaarBegrunnelse> art161Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART16_1);
        Art161AvslagBegrunnelse art161AvslagBegrunnelser = lagArt161AvslagBegrunnelse();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : art161Begrunnelser) {
            Art16_1_Avslag__Begrunnelser artikkel161AvslagKode = Art16_1_Avslag__Begrunnelser.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel161AvslagKode) {
                case ERSTATTER_EN_ANNEN_SAMLET_OVER_5_AAR:
                    art161AvslagBegrunnelser.setOver5Aar(JA);
                    break;
                case FORLENGELSE_SAMLET_OVER_5_AAR:
                    art161AvslagBegrunnelser.setOver5Aar(JA);
                    break;
                case INGEN_SPESIELLE_FORHOLD:
                    art161AvslagBegrunnelser.setIngenSpesielleForhold(JA);
                    break;
                case SAERLIG_AVSLAGSGRUNN:
                    art161AvslagBegrunnelser.setSaerligAvslagsgrunn(JA);
                    validerFritekstbegrunnelse(brevData.fritekst);
                    fag.setBegrunnelseFritekst(brevData.fritekst);
                    break;
                case SOEKT_FOR_SENT:
                    art161AvslagBegrunnelser.setSoektForSent(JA);
                    break;
                default:
                    throw new TekniskException(artikkel161AvslagKode + " støttes ikke.");
            }
        }

        fag.setArt161AvslagBegrunnelse(art161AvslagBegrunnelser);
        return fag;
    }

    private static Art161AvslagBegrunnelse lagArt161AvslagBegrunnelse() {
        return Art161AvslagBegrunnelse.builder().withIngenSpesielleForhold("")
            .withOver5Aar("")
            .withSaerligAvslagsgrunn("")
            .withSoektForSent("").build();
    }
}
