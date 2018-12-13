package no.nav.melosys.saksflyt.agent.brev;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.DokumenttypeId.MELDING_MANGLENDE_OPPLYSNINGER;
import static no.nav.melosys.domain.ProsessSteg.MANGELBREV;

/**
 * Sender mangelbrev til søker/arbeidsgiver
 *
 * Transisjoner:
 * MANGELBREV -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendMangelbrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendMangelbrev.class);

    private final BehandlingRepository behandlingRepo;

    private final DokumentSystemService dokumentService;

    private static final int DOKUMENTASJON_SVARFRIST_UKER = 4;

    @Autowired
    public SendMangelbrev(BehandlingRepository behandlingRepo, DokumentSystemService dokumentService) {
        this.behandlingRepo = behandlingRepo;
        this.dokumentService = dokumentService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return MANGELBREV;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        BrevData brevData = prosessinstans.getData(ProsessDataKey.BREVDATA, BrevData.class);

        dokumentService.produserDokument(behandling.getId(), MELDING_MANGLENDE_OPPLYSNINGER, brevData);

        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(LocalDateTime.now().plusWeeks(DOKUMENTASJON_SVARFRIST_UKER).toInstant(ZoneOffset.UTC));
        behandlingRepo.save(behandling);

        prosessinstans.setSteg(null);
        log.info("Sendt mangelbrev for prosessinstans {}", prosessinstans.getId());
    }
}
