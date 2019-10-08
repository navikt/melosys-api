package no.nav.melosys.saksflyt.steg.sed.jfr.brev;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("JournalførAouBrevOpprettSedDokument")
public class OpprettSedDokument extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);
    private static final String SED_DOKUMENT_VERSJON = "0.2-SNAPSHOT";

    private final SaksopplysningRepository saksopplysningRepository;
    private final DokumentFactory dokumentFactory;

    @Autowired
    public OpprettSedDokument(SaksopplysningRepository saksopplysningRepository, DokumentFactory dokumentFactory) {
        this.saksopplysningRepository = saksopplysningRepository;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Instant nå = Instant.now();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setEndretDato(nå);
        saksopplysning.setRegistrertDato(nå);
        saksopplysning.setDokument(opprettSedDokument(prosessinstans));
        saksopplysning.setBehandling(prosessinstans.getBehandling());
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
        saksopplysning.setVersjon(SED_DOKUMENT_VERSJON);

        saksopplysning.setDokumentXml(dokumentFactory.lagInternXml(saksopplysning));
        saksopplysning.setBehandling(prosessinstans.getBehandling());

        saksopplysningRepository.save(saksopplysning);
        log.info("Saksopplysning: SedDokument opprettet for behandling {}", prosessinstans.getBehandling().getId());

        prosessinstans.setSteg(ProsessSteg.JFR_AOU_BREV_OPPRETT_GSAK_SAK);
    }

    private static SaksopplysningDokument opprettSedDokument(Prosessinstans prosessinstans) {
        final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

        String brukerId = prosessinstans.getData(ProsessDataKey.BRUKER_ID);
        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);

        LovvalgBestemmelse lovvalgsbestemmelse = konverterer.convertToEntityAttribute(
            prosessinstans.getData(ProsessDataKey.LOVVALGSBESTEMMELSE));
        LovvalgBestemmelse unntakFraLovvalgBestemmelse = konverterer.convertToEntityAttribute(
            prosessinstans.getData(ProsessDataKey.UNNTAK_FRA_LOVVALGSBESTEMMELSE));

        SedDokument sedDokument = new SedDokument();
        sedDokument.setErElektronisk(false);
        sedDokument.setBucType(BucType.LA_BUC_01);
        sedDokument.setSedType(SedType.A001);
        sedDokument.setFnr(brukerId);
        sedDokument.setLovvalgsperiode(periode);
        sedDokument.setLovvalgslandKode(hentFørste(ProsessDataKey.LOVVALGSLAND, prosessinstans));
        sedDokument.setUnntakFraLovvalgslandKode(hentFørste(ProsessDataKey.UNNTAK_FRA_LOVVALGSLAND, prosessinstans));
        sedDokument.setLovvalgBestemmelse(lovvalgsbestemmelse);
        sedDokument.setUnntakFraLovvalgBestemmelse(unntakFraLovvalgBestemmelse);

        return sedDokument;
    }

    private static Landkoder hentFørste(ProsessDataKey land, Prosessinstans prosessinstans) {
        List landliste = prosessinstans.getData(land, List.class);
        if (landliste.isEmpty() || landliste.iterator().next() == null) {
            return null;
        }

        return Landkoder.valueOf(landliste.iterator().next().toString());
    }
}
