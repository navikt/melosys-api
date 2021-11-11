package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

public class InnvilgelseUK extends DokgenDto {

    private final Mottaker mottaker;
    private final Innvilgelse innvilgelse;
    private final Lovvalgbestemmelser_trygdeavtale_uk artikkel;
    private final Soknad soknad;
    private final Familie familie;
    private final boolean virksomhetArbeidsgiverSkalHaKopi;


    public InnvilgelseUK(InnvilgelseBrevbestilling brevbestilling,
                         Mottaker mottaker,
                         Lovvalgbestemmelser_trygdeavtale_uk artikkel,
                         Soknad soknad,
                         Familie familie,
                         boolean virksomhetArbeidsgiverSkalHaKopi) {
        super(brevbestilling);
        this.mottaker = mottaker;
        this.innvilgelse = Innvilgelse.av(brevbestilling);
        this.artikkel = artikkel;
        this.soknad = soknad;
        this.familie = familie;
        this.virksomhetArbeidsgiverSkalHaKopi = virksomhetArbeidsgiverSkalHaKopi;
    }

    @Override
    public Mottaker getMottaker() {
        return mottaker;
    }

    public Innvilgelse getInnvilgelse() {
        return innvilgelse;
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

    public boolean isVirksomhetArbeidsgiverSkalHaKopi() {
        return virksomhetArbeidsgiverSkalHaKopi;
    }
}
