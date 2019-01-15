package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;

import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AnmodningBegrunnelseKode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;


public class AnmodningUnntakMapper extends AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    @Override
    Fag mapArt161(Fag fag, Behandlingsresultat resultat, BrevData brevData) throws TekniskException {
        Set<VilkaarBegrunnelse> art161AnmodningBegrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART16_1);
        VilkaarBegrunnelse vilkaarBegrunnelse = art161AnmodningBegrunnelser.stream()
            .findFirst().orElseThrow(() -> new TekniskException("Ingen begrunnelse funnet for brev om Artikkel 16.1"));
        fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.valueOf(vilkaarBegrunnelse.getKode()));

        if (fag.getArt161AnmodningBegrunnelse() == Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN) {
            validerFritekstbegrunnelse(brevData.fritekst);
            fag.setAnmodningFritekst(brevData.fritekst);
        }
        return fag;
    }
}
