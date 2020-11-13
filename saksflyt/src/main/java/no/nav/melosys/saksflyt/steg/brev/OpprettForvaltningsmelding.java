package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.DokgenService;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidKlage;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_FORVALTNINGSMELDING;

@Component
public class OpprettForvaltningsmelding implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettForvaltningsmelding.class);

    private final BehandlingService behandlingService;
    private final DokgenService dokgenService;
    private final JoarkFasade joarkFasade;

    @Autowired
    public OpprettForvaltningsmelding(BehandlingService behandlingService, DokgenService dokgenService, JoarkFasade joarkFasade, DoksysFasade doksysFasade) {
        this.behandlingService = behandlingService;
        this.dokgenService = dokgenService;
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_FORVALTNINGSMELDING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        boolean skalSendesForvaltningsmelding = prosessinstans.getData(SKAL_SENDES_FORVALTNINGSMELDING, Boolean.class, Boolean.FALSE);

        if (prosessinstans.getBehandling().erBehandlingAvSøknad() && skalSendesForvaltningsmelding) {
            Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
            Behandlingstyper behandlingType = behandling.getType();

            byte[] pdf;

            switch (behandlingType) {
                case SOEKNAD:
                    pdf = dokgenService.lagPdf("saksbehandlingstid_soknad", SaksbehandlingstidSoknad.map(behandling));
                    break;
                case KLAGE:
                    pdf = dokgenService.lagPdf("saksbehandlingstid_klage", SaksbehandlingstidKlage.map(behandling));
                    break;
                default:
                    throw new FunksjonellException(format("Behandlingsstype %s er ikke støttet for forvaltningsmelding", behandlingType.getKode()));
            }

            String journalpostId = joarkFasade.opprettJournalpost(
                OpprettJournalpost.lagJournalpostForPdf("Melding om forventet saksbehandlingstid",
                    behandling.hentPersonDokument().fnr, pdf), true);

            prosessinstans.setData(DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);

            log.info("Forvaltningsmelding er opprettet og journalført for behandling {}. JournalpostId: {}", prosessinstans.getBehandling().getId(), journalpostId);
        } else {
            log.info("Ikke opprettet forvaltningsmelding for behandling {}", prosessinstans.getBehandling().getId());
        }
    }
}
