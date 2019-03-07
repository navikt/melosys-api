package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_987_2009;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.stereotype.Service;

@Service
public class SedDataByggerVelger {
    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagSystemService registerOppslagService;
    private final KodeverkService kodeverkService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final AvklarteVirksomheterService avklarteVirksomheterService;

    public SedDataByggerVelger(AvklartefaktaService avklartefaktaService, RegisterOppslagSystemService registerOppslagService,
                               KodeverkService kodeverkService, LovvalgsperiodeService lovvalgsperiodeService,
                               UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                               VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
    }

    public SedDataBygger hent(LovvalgBestemmelse lovvalgBestemmelse) {

        if (lovvalgBestemmelse instanceof LovvalgsBestemmelser_883_2004) {
            LovvalgsBestemmelser_883_2004 lovvalgBestemmelse_883_2004 = (LovvalgsBestemmelser_883_2004) lovvalgBestemmelse;
            switch ((LovvalgsBestemmelser_883_2004) lovvalgBestemmelse) {

                case FO_883_2004_ART12_1:
                case FO_883_2004_ART12_2:
                case FO_883_2004_ART16_1:
                case FO_883_2004_ART16_2:
                    return new SedDataBygger(kodeverkService,
                        lovvalgsperiodeService, avklartefaktaService, avklarteVirksomheterService);
                //art 12 og 16 trenger samme data
                //SedDatabygger settes for nå til flere SED'er er implementert og mer informasjon trengs
                case FO_883_2004_ART11_1:
                case FO_883_2004_ART11_3A:
                case FO_883_2004_ART11_3B:
                case FO_883_2004_ART11_3C:
                case FO_883_2004_ART11_3E:
                case FO_883_2004_ART11_4_2:
                case FO_883_2004_ART13_1A:
                case FO_883_2004_ART13_1B1:
                case FO_883_2004_ART13_1_B2:
                case FO_883_2004_ART13_1_B3:
                case FO_883_2004_ART13_1_B4:
                case FO_883_2004_ART13_2A:
                case FO_883_2004_ART13_2B:
                case FO_883_2004_ART13_3:
                case FO_883_2004_ART13_4:
            }
        } else if (lovvalgBestemmelse instanceof LovvalgsBestemmelser_987_2009) {
            switch ((LovvalgsBestemmelser_987_2009) lovvalgBestemmelse) {
                case FO_987_2009_ART14_11:
            }
        }
        //Kaster runtime exception til resten av SED'er er implementert
        throw new RuntimeException("Støtte for å sende sed for lovvalgsbestemmelse "
            + lovvalgBestemmelse.name() + " er ikke implementert enda");
    }
}
