package no.nav.melosys.saksflyt.agent.ufm;

import java.time.Instant;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokument extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private final SaksopplysningRepository saksopplysningRepository;
    private final DokumentFactory dokumentFactory;

    @Autowired
    public OpprettSedDokument(SaksopplysningRepository saksopplysningRepository, DokumentFactory dokumentFactory) {
        this.saksopplysningRepository = saksopplysningRepository;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        SedDokument sedDokument = prosessinstans.getData(ProsessDataKey.SED_DOKUMENT, SedDokument.class);

        if (sedDokument == null) {
            throw new TekniskException("SedDokument finnes ikke!");
        }

        Instant nå = Instant.now();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);
        saksopplysning.setType(SaksopplysningType.SED_OPPLYSNINGER);
        saksopplysning.setBehandling(prosessinstans.getBehandling());
        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
        saksopplysning.setVersjon("0.2-SNAPSHOT");
        saksopplysning.setEndretDato(nå);
        saksopplysning.setRegistrertDato(nå);

        String xml = dokumentFactory.lagInternXml(saksopplysning);
        saksopplysning.setDokumentXml(xml);

        saksopplysningRepository.save(saksopplysning);
        log.info("Saksopplysning: SedDokument opprettet for behandling {}", prosessinstans.getBehandling().getId());

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_AVSLUTT_TIDLIGERE_PERIODE);
    }
}
