package no.nav.melosys.service.dokument;

import javax.transaction.Transactional;

import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.Familie;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.InnvilgelseUK;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.Kopi;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.Soknad;
import org.springframework.stereotype.Component;

@Component
public class InnvilgelseUKMapper {

    // Nok lurt å se på InnvilgelseFtrlMapper. Burde være lignende
    public InnvilgelseUKMapper() {
    }

    @Transactional
    public InnvilgelseUK map(InnvilgelseBrevbestilling brevbestilling) {
        // TODO: map brevbestilling
        Mottaker mottaker = null;
        Lovvalgbestemmelser_trygdeavtale_uk artikkel = null;
        Soknad soknad = null;
        Familie familie = null;
        Kopi kopi = null;
        return new InnvilgelseUK(
            brevbestilling,
            mottaker,
            artikkel,
            soknad,
            familie,
            kopi
        );
    }
}
