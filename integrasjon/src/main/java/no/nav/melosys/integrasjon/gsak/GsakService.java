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

    private static final String FAGOMRÅDE_KODE_MEDLEMSKAP = "MED"; // -> Medlemskap
    private static final String FAGSYSTEM_KODE_MELOSYS = "FS22";// TODO (FA) endre når koden er opprettet i GSAK
    private static final String SAK_TYPE_FAGSAK = "MFS"; // -> Med fagsak

    private BehandleSakConsumer behandleSakConsumer;

    @Autowired
    public GsakService(BehandleSakConsumer behandleSakConsumer) {
        this.behandleSakConsumer = behandleSakConsumer;
    }

    @Override
    public String opprettSak(Long fagsakId, String fnr) throws IntegrasjonException {
        Sak sak = new Sak();
        Fagomraader fagområde = new Fagomraader();
        fagområde.setValue(FAGOMRÅDE_KODE_MEDLEMSKAP);
        sak.setFagomraade(fagområde);

        Sakstyper sakstype = new Sakstyper();
        sakstype.setValue(SAK_TYPE_FAGSAK);
        sak.setSakstype(sakstype);

        Aktoer aktør = new Person();
        aktør.setIdent(fnr);
        sak.getGjelderBrukerListe().add(aktør);

        Fagsystemer fagsystem = new Fagsystemer();
        fagsystem.setValue(FAGSYSTEM_KODE_MELOSYS);
        sak.setFagsystem(fagsystem);

        String fagsystemSakId = FAGSYSTEM_KODE_MELOSYS + fagsakId.toString();
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
