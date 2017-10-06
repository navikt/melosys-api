package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.MedlemskapV2;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;

public class MedlemskapConsumerImpl implements MedlemskapConsumer {

    private MedlemskapV2 port;

    public MedlemskapConsumerImpl(MedlemskapV2 port) {
        this.port = port;
    }

    @Override
    public HentPeriodeListeResponse hentPeriodeListe(HentPeriodeListeRequest req) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        return port.hentPeriodeListe(req);
    }
}
