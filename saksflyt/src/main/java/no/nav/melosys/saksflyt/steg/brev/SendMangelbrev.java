package no.nav.melosys.saksflyt.steg.brev;

import java.time.Instant;
import java.time.Period;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.MANGELBREV;

/**
 * Sender mangelbrev til søker/arbeidsgiver
 *
 * Transisjoner:
 * MANGELBREV -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendMangelbrev implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendMangelbrev.class);

    private final BehandlingService behandlingService;

    private static final int DOKUMENTASJON_SVARFRIST_UKER = 4;

    private final BrevBestiller brevBestiller;
    @Autowired
    public SendMangelbrev(BehandlingService behandlingService, BrevBestiller brevBestiller) {
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return MANGELBREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        Aktoersroller mottaker = prosessinstans.getData(ProsessDataKey.MOTTAKER, Aktoersroller.class);
        BrevData brevData = prosessinstans.getData(ProsessDataKey.BREVDATA, BrevData.class);

        String saksbehandler = brevData.saksbehandler;
        String fritekst = brevData.fritekst;

        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(MELDING_MANGLENDE_OPPLYSNINGER)
            .medAvsender(saksbehandler)
            .medMottakere(Mottaker.av(mottaker))
            .medBehandling(behandling)
            .medFritekst(fritekst).build();
        brevBestiller.bestill(brevbestilling);

        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER)));
        behandlingService.lagre(behandling);

        prosessinstans.setSteg(ProsessSteg.FERDIG);
        log.info("Sendt mangelbrev for prosessinstans {}", prosessinstans.getId());
    }
}
