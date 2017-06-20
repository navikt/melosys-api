package no.nav.melosys.integrasjon.tps.aktoer;

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentListeRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentListeResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdListeRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdListeResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;

public class AktorConsumerImpl implements AktorConsumer {
    private AktoerV2 port;

    public AktorConsumerImpl(AktoerV2 port) {
        this.port = port;
    }

    @Override
    public HentAktoerIdForIdentResponse hentAktørIdForIdent(HentAktoerIdForIdentRequest request)
            throws HentAktoerIdForIdentPersonIkkeFunnet {
        return port.hentAktoerIdForIdent(request);
    }

    @Override
    public HentAktoerIdForIdentListeResponse hentAktoerIdForIdentListeRequest(HentAktoerIdForIdentListeRequest request) {
        return port.hentAktoerIdForIdentListe(request);
    }

    @Override
    public HentIdentForAktoerIdResponse hentIdentForAktoerId(HentIdentForAktoerIdRequest request)
            throws HentIdentForAktoerIdPersonIkkeFunnet {
        return port.hentIdentForAktoerId(request);
    }

    @Override
    public HentIdentForAktoerIdListeResponse hentIdentForAktoerIdListeRequest(HentIdentForAktoerIdListeRequest request) {
        return port.hentIdentForAktoerIdListe(request);
    }
}
