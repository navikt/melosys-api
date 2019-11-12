package no.nav.melosys.integrasjonstest.felles.verifisering;

import java.util.Arrays;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.service.dokument.brev.DokumenttypeIdMapper.hentID;

public class ForventetDokumentBestilling {
    private static final Logger logger = LoggerFactory.getLogger(ForventetDokumentBestilling.class);

    public Produserbaredokumenter produserbaredokumenter;
    public Aktoersroller aktoersroller;
    public String mottakerID;

    public static ForventetDokumentBestilling forventDokument(Produserbaredokumenter dok, Aktoersroller rolle) {
        return forventDokument(dok, rolle, null);
    }

    public static ForventetDokumentBestilling forventDokument(Produserbaredokumenter dok, Aktoersroller rolle, String mottakerId) {
        ForventetDokumentBestilling dokumentBestilling = new ForventetDokumentBestilling();
        dokumentBestilling.produserbaredokumenter = dok;
        dokumentBestilling.aktoersroller = rolle;
        dokumentBestilling.mottakerID = mottakerId;
        return dokumentBestilling;
    }

    public boolean erOppfylt(DokumentbestillingMetadata metadata) {
        try {
            if (produserbaredokumenter == null || hentID(produserbaredokumenter).equals(metadata.dokumenttypeID)) {
                if (aktoersroller == null || aktoersroller == metadata.mottaker.getRolle()) {
                    if (StringUtils.isBlank(mottakerID) || mottakerID.equals(metadata.mottakerID)) {
                        return true;
                    }
                }
            }
        } catch (TekniskException e) { logger.info(Arrays.toString(e.getStackTrace())); }
        return false;
    }

    @Override
    public String toString() {
        return produserbaredokumenter + "-" + aktoersroller.getKode() + "-" + mottakerID;
    }

}
