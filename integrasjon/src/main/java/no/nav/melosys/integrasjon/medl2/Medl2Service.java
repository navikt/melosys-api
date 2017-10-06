package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapConsumer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class Medl2Service implements Medl2Fasade {

    private static final Logger log = LoggerFactory.getLogger(Medl2Service.class);

    private MedlemskapConsumer medlemskapConsumer;

    @Autowired
    public Medl2Service(MedlemskapConsumer medlemskapConsumer) {
        this.medlemskapConsumer = medlemskapConsumer;
    }

    @Override
    public List<Medlemsperiode> hentPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        Foedselsnummer ident = new Foedselsnummer();
        ident.setValue(fnr);

        HentPeriodeListeRequest req = new HentPeriodeListeRequest();
        req.setIdent(ident);

        HentPeriodeListeResponse res = medlemskapConsumer.hentPeriodeListe(req);

        /* Medlemsperiode:
        * ["id", "versjon", "datoRegistrert", "datoBesluttet", "status", "statusaarsak", "trygdedekning", "helsedel",
        * "type", "land", "lovvalg", "kilde", "kildedokumenttype", "grunnlagstype", "studieinformasjon"]
        */

        // TODO: Integrate with this domain model instead
        return res.getPeriodeListe();
    }
}
