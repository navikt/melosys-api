package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AnmodningBegrunnelseKode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART16_1;


public class AnmodningUnntakMapper extends AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    @Override
    Fag mapArt161(Fag fag, Behandlingsresultat resultat, BrevData brevData) throws TekniskException {

        Vilkaarsresultat vilkaarsresultat = hentFørsteGyldigeVilkaarsresultat(resultat, FO_883_2004_ART16_1)
            .orElseThrow(() -> new TekniskException("Ingen begrunnelse funnet for brev om Artikkel 16.1"));

        VilkaarBegrunnelse vilkaarBegrunnelse = vilkaarsresultat.getBegrunnelser().stream()
            .filter(b -> Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN.value().equals(b.getKode()))
            .findFirst().orElseGet(() -> vilkaarsresultat.getBegrunnelser().iterator().next());

        if (vilkaarBegrunnelse.getKode().equals(Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN.value())) {
            validerFritekstbegrunnelse(vilkaarsresultat.getBegrunnelseFritekst());
            fag.setAnmodningFritekst(vilkaarsresultat.getBegrunnelseFritekst());
        }
        fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.valueOf(vilkaarBegrunnelse.getKode()));

        return fag;
    }
}
