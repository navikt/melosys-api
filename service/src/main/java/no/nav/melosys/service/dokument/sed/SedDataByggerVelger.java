package no.nav.melosys.service.dokument.sed;

import javax.validation.constraints.NotNull;

import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.bygger.A009DataBygger;
import no.nav.melosys.service.dokument.sed.bygger.AbstraktSedDataBygger;
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
    }

    public AbstraktSedDataBygger hent(@NotNull SedType sedType) {
        switch (sedType) {
            case A009:
                return new A009DataBygger(kodeverkService,registerOppslagService,
                    lovvalgsperiodeService,avklartefaktaService);
        }

        //Kaster runtime exception til resten av
        throw new RuntimeException("Sed-type: " + sedType.name() + " er ikke implementert enda");
    }
}
