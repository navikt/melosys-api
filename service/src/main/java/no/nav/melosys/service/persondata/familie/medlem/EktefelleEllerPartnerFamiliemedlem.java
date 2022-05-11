package no.nav.melosys.service.persondata.familie.medlem;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstandstype;
import no.nav.melosys.service.persondata.mapping.FamiliemedlemOversetter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import static no.nav.melosys.integrasjon.pdl.dto.person.Sivilstandstype.*;

@Component
public class EktefelleEllerPartnerFamiliemedlem {

    private final PDLConsumer pdlConsumer;

    public EktefelleEllerPartnerFamiliemedlem(PDLConsumer pdlConsumer) {
        this.pdlConsumer = pdlConsumer;
    }

    @NotNull
    public Collection<Familiemedlem> hentEktefelleEllerPartner(Collection<Sivilstand> sivilstandCollection) {
        List<Sivilstand> sivilstanderTilHovedperson = sivilstandCollection.stream().toList();

        if (sivilstanderTilHovedperson.isEmpty()) {
            return Collections.emptySet();
        }

        if (sivilstanderTilHovedperson.size() > 1) {
            List<Sivilstand> aktuelleSivilstanderTilHovedperson =
                hentSivilstanderSomIkkeErGiftSamtidigSomSeparertEllerSkilt(sivilstanderTilHovedperson);

            Optional<Sivilstand> sisteSivilstand = aktuelleSivilstanderTilHovedperson.stream()
                .min(this::sammenlignSisteDatoRegistrert);
            return hentEktefelleEllerPartner(sisteSivilstand.get());
        }

        return hentEktefelleEllerPartner(sivilstanderTilHovedperson.get(0));
    }

    private List<Sivilstand> hentSivilstanderSomIkkeErGiftSamtidigSomSeparertEllerSkilt(List<Sivilstand> sivilstanderTilHovedperson) {
        return sivilstanderTilHovedperson.stream()
            .collect(Collectors.groupingBy(HarMetadata::hentDatoSistRegistrert)).values().stream()
            .map(this::lagSivilstanderSomIkkeErSeparertEllerSkilt)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream).toList();
    }

    private List<Sivilstand> lagSivilstanderSomIkkeErSeparertEllerSkilt(List<Sivilstand> sivilstandList) {
        List<Sivilstandstype> sivilstandstyper = sivilstandList.stream().map(Sivilstand::type).toList();
        return erIkkeGiftSamtidigSomSkiltEllerSeparert(sivilstandstyper) ? sivilstandList : null;
    }

    private boolean erIkkeGiftSamtidigSomSkiltEllerSeparert(List<Sivilstandstype> sivilstandstyper) {
        return !sivilstandstyper.containsAll(List.of(GIFT, SEPARERT)) &&
            !sivilstandstyper.containsAll(List.of(GIFT, SKILT));
    }

    @NotNull
    private Set<Familiemedlem> hentEktefelleEllerPartner(Sivilstand sivilstand) {
        if (sivilstand.erGyldigForEktefelleEllerPartner()) {
            return lagFamiliemedlemFraSivilstand(sivilstand);
        } else {
            return Collections.emptySet();
        }
    }

    @NotNull
    private Set<Familiemedlem> lagFamiliemedlemFraSivilstand(Sivilstand sivilstand) {
        String ident = sivilstand.relatertVedSivilstand();
        Person person = pdlConsumer.hentEktefelleEllerPartner(ident);
        return Set.of(FamiliemedlemOversetter.oversettEktefelleEllerPartner(person, sivilstand));
    }

    private int sammenlignSisteDatoRegistrert(Sivilstand sivilstand1, Sivilstand sivilstand2) {
        return sivilstand2.hentDatoSistRegistrert().compareTo(sivilstand1.hentDatoSistRegistrert());
    }
}
