package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AnmodningBegrunnelseKode;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART16_1;


public class AnmodningUnntakMapper extends AbstraktAnmodningUnntakOgAvslagMapper implements BrevDataMapper {

    @Override
    Fag mapArt161(Fag fag, Behandlingsresultat resultat, BrevData brevData) throws TekniskException {

        Set<Vilkaarsresultat> vilkaarsresultater = resultat.getVilkaarsresultater().stream()
            .filter(v -> v.getVilkaar().equals(FO_883_2004_ART16_1)).collect(Collectors.toSet());

        if (vilkaarsresultater.isEmpty() || vilkaarsresultater.iterator().next().getBegrunnelser().isEmpty()) {
            throw new TekniskException("Ingen begrunnelse funnet for brev om Artikkel 16.1");
        }

        Vilkaarsresultat vilkaarsresultat = vilkaarsresultater.iterator().next();
        boolean erSærligGrunn = vilkaarsresultat.getBegrunnelser().stream()
            .anyMatch(v -> Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN.value().equals(v.getKode()));

        if (erSærligGrunn) {
            validerFritekstbegrunnelse(vilkaarsresultat.getBegrunnelseFritekst());
            fag.setAnmodningFritekst(vilkaarsresultat.getBegrunnelseFritekst());
            fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN);
        } else {
            fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.valueOf(
                vilkaarsresultat.getBegrunnelser().iterator().next().getKode()));
        }

        return fag;
    }
}
