package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;

public class BehandleMedlemskapConsumerImpl implements BehandleMedlemskapConsumer {

    private BehandleMedlemskapV2 port;

    public BehandleMedlemskapConsumerImpl(BehandleMedlemskapV2 port) {
        this.port = port;
    }

    @Override
    public OpprettPeriodeResponse opprettPeriode(OpprettPeriodeRequest request) throws PersonIkkeFunnet, Sikkerhetsbegrensning, UgyldigInput {
        return port.opprettPeriode(request);
    }

    @Override
    public void oppdaterPeriode(OppdaterPeriodeRequest request) throws Sikkerhetsbegrensning, UgyldigInput, PeriodeUtdatert, PeriodeIkkeFunnet {
        port.oppdaterPeriode(request);
    }
}
