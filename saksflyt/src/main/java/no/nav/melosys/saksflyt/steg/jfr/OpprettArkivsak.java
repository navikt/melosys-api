package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.ArkivsakService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_ARKIVSAK;

@Component
public class OpprettArkivsak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettArkivsak.class);

    private final FagsakService fagsakService;
    private final ArkivsakService arkivsakService;

    public OpprettArkivsak(FagsakService fagsakService, ArkivsakService arkivsakService) {
        this.fagsakService = fagsakService;
        this.arkivsakService = arkivsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_ARKIVSAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();

        if (fagsak.getGsakSaksnummer() != null) {
            throw new FunksjonellException("Kan ikke knytte fagsak " + fagsak.getSaksnummer() + " til ny arkivsak: allerede knyttet til " + fagsak.getGsakSaksnummer());
        }

        Optional<String> aktørId = fagsak.finnBrukersAktørID();
        String saksnummer = fagsak.getSaksnummer();
        Behandlingstema behandlingstema = behandling.getTema();
        Sakstemaer sakstemaer = fagsak.getTema();

        Long arkivsakID;
        if (aktørId.isPresent()) {
            arkivsakID = arkivsakService.opprettSakForBruker(saksnummer, behandlingstema, sakstemaer, aktørId.get());
        } else {
            arkivsakID = arkivsakService.opprettSakForVirksomhet(saksnummer, behandlingstema, sakstemaer, prosessinstans.getData(ProsessDataKey.VIRKSOMHET_ORGNR));
        }
        fagsak.setGsakSaksnummer(arkivsakID);
        fagsakService.lagre(fagsak);

        log.info("Opprettet arkivsak {} for fagsak {}", arkivsakID, saksnummer);
    }
}
