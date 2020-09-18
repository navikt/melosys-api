package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;

@Component
public class OpprettFagsakOgBehandlingFraSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandlingFraSed.class);

    private final FagsakService fagsakService;

    @Autowired
    public OpprettFagsakOgBehandlingFraSed(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(EESSI_MELDING, MelosysEessiMelding.class);

        if (prosessinstans.getType() == ProsessType.MOTTAK_SED || prosessinstans.getType() == ProsessType.MOTTAK_SED_JOURNALFØRING) {
            throw new TekniskException("Prosessinstans er av type " + prosessinstans.getType());
        }

        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);
        Sakstyper sakstype = behandlingstema == Behandlingstema.BESLUTNING_LOVVALG_NORGE ? Sakstyper.UKJENT : Sakstyper.EU_EOS;

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(prosessinstans.getData(AKTØR_ID))
            .medBehandlingstype(Behandlingstyper.SED)
            .medBehandlingstema(behandlingstema)
            .medInitierendeJournalpostId(prosessinstans.getData(JOURNALPOST_ID))
            .medInitierendeDokumentId(prosessinstans.getData(DOKUMENT_ID))
            .medSakstype(sakstype)
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());

        Behandling behandling = fagsak.hentAktivBehandling();
        prosessinstans.setBehandling(behandling);

        log.info("Fagsak {} opprettet med behandling {} for RINA-sak {}",
            fagsak.getSaksnummer(), behandling.getId(), melosysEessiMelding.getRinaSaksnummer());

        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_SAK);
    }
}
