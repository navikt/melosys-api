package no.nav.melosys.service.brev.bestilling;

import java.util.List;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV;

@Component
public class HentBrevmottakereNorskMyndighetService {

    private final EregFasade eregFasade;

    public HentBrevmottakereNorskMyndighetService(EregFasade eregFasade) {
        this.eregFasade = eregFasade;
    }

    @Transactional
    public List<Brevmottaker> hentMuligeBrevmottakereNorskMyndighet(List<String> orgnrNorskeMyndigheter) {
        return orgnrNorskeMyndigheter.stream().map(this::mapTilBrevmottaker).toList();
    }

    @NotNull
    private Brevmottaker mapTilBrevmottaker(String orgnr) {
        return new Brevmottaker.Builder()
            .medRolle(Mottakerroller.NORSK_MYNDIGHET)
            .medDokumentNavn(FRITEKSTBREV.getBeskrivelse())
            .medOrgnr(orgnr)
            .medMottakerNavn(eregFasade.hentOrganisasjonNavn(orgnr))
            .build();
    }
}
