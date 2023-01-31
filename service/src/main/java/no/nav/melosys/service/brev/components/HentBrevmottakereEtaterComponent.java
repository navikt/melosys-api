package no.nav.melosys.service.brev.components;

import java.util.List;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ETAT;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV;

@Component
public class HentBrevmottakereEtaterComponent {

    private final EregFasade eregFasade;

    public HentBrevmottakereEtaterComponent(EregFasade eregFasade) {
        this.eregFasade = eregFasade;
    }

    @Transactional
    public List<Brevmottaker> hentMuligeBrevmottakereEtater(List<String> orgnrEtater) {
        return orgnrEtater.stream().map(this::mapTilBrevmottaker).toList();
    }

    @NotNull
    private Brevmottaker mapTilBrevmottaker(String orgnr) {
        return new Brevmottaker.Builder()
            .medRolle(ETAT)
            .medDokumentNavn(FRITEKSTBREV.getBeskrivelse())
            .medOrgnr(orgnr)
            .medMottakerNavn(eregFasade.hentOrganisasjonNavn(orgnr))
            .build();
    }
}
