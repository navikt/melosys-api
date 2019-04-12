package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AnmodningBegrunnelseKode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;


public class AnmodningUnntakMapper extends AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    @Override
    Fag mapArt161(Fag fag, Behandlingsresultat resultat, BrevData brevData) throws TekniskException {

        Vilkaarsresultat vilkaarsresultat = hentFørsteGyldigeVilkaarsresultatForArt16(resultat)
            .orElseThrow(() -> new TekniskException("Ingen begrunnelse funnet for brev om Artikkel 16.1"));

        VilkaarBegrunnelse vilkaarBegrunnelse = vilkaarsresultat.getBegrunnelser().iterator().next();
        fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.valueOf(vilkaarBegrunnelse.getKode()));

        return fag;
    }
}
