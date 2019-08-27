package no.nav.melosys.service.eessi;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

//A001
@Service
public class AnmodningUnntakMottakInitialiserer implements BehandleMottattSedInitialiserer {
    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakMottakInitialiserer.class);

    // TODO: Bruk av eksisterende behandling
    @Override
    public void initialiserProsessinstans(Prosessinstans prosessinstans) {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        log.info("Behandler mottatt EESSI-medling. Buc: {}, SED: {}", melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_FAGSAK_OG_BEH);
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A001;
    }
}
