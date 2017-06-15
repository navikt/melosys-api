package no.nav.melosys.integrasjon.gsak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.integrasjon.felles.IntegrasjonException;
import no.nav.melosys.integrasjon.gsak.behandlesak.BehandleSakConsumer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Fagomraader;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Fagsystemer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Person;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Sak;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Sakstyper;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakResponse;

@Service
public class GsakService implements GsakFasade {

    private static final Logger log = LoggerFactory.getLogger(GsakService.class);

    private static final String FAGOMRÅDE_KODE = "MED"; // -> Medlemskap
    private static final String FAGSYSTEM_KODE = "FS22";// TODO (FA) endre når koden er opprettet i GSAK
    private static final String SAK_TYPE = "MFS"; // -> Med fagsak

    private BehandleSakConsumer behandleSakConsumer;

    @Autowired
    public GsakService(BehandleSakConsumer behandleSakConsumer) {
        this.behandleSakConsumer = behandleSakConsumer;
    }

    @Override
    public String opprettSak(Long fagsakId, String fnr) {
        Sak sak = new Sak();
        Fagomraader fagområde = new Fagomraader();
        fagområde.setValue(FAGOMRÅDE_KODE);
        sak.setFagomraade(fagområde);

        Sakstyper sakstype = new Sakstyper();
        sakstype.setValue(SAK_TYPE);
        sak.setSakstype(sakstype);

        Aktoer aktoer = new Person();
        aktoer.setIdent(fnr);
        sak.getGjelderBrukerListe().add(aktoer);

        Fagsystemer fagsystem = new Fagsystemer();
        fagsystem.setValue(FAGSYSTEM_KODE);
        sak.setFagsystem(fagsystem);

        String fagsystemSakId = FAGSYSTEM_KODE + fagsakId.toString();
        sak.setFagsystemSakId(fagsystemSakId);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest();
        opprettSakRequest.setSak(sak);

        try {
            OpprettSakResponse response = behandleSakConsumer.opprettSak(opprettSakRequest);
            log.info("Sak opprettet i GSAK med saksnummer: {}", response.getSakId());
            return response.getSakId();
        } catch (OpprettSakSakEksistererAllerede e) {
            throw new IntegrasjonException(e);
        } catch (OpprettSakUgyldigInput e) {// NOSONAR
            throw new IntegrasjonException(e);
        }
    }
}
