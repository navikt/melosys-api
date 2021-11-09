package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

public class InnvilgelseUK extends DokgenDto {

    private final Mottaker mottaker;
    private final Lovvalgbestemmelser_trygdeavtale_uk artikkel;
    private final Soknad soknad;
    private final Familie familie;
    private final Kopi kopi;


    public InnvilgelseUK(InnvilgelseBrevbestilling brevbestilling,
                         Mottaker mottaker,
                         Lovvalgbestemmelser_trygdeavtale_uk artikkel,
                         Soknad soknad,
                         Familie familie,
                         Kopi kopi) {
        super(brevbestilling);
        this.mottaker = mottaker;
        this.artikkel = artikkel;
        this.soknad = soknad;
        this.familie = familie;
        this.kopi = kopi;
    }

    @Override
    public Mottaker getMottaker() {
        return mottaker;
    }

    public Lovvalgbestemmelser_trygdeavtale_uk getArtikkel() {
        return artikkel;
    }

    public Soknad getSoknad() {
        return soknad;
    }

    public Familie getFamilie() {
        return familie;
    }

    public Kopi getKopi() {
        return kopi;
    }
}
