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
public class EktefelleEllerPartnerFamiliemedlemFilter {

    private final PDLConsumer pdlConsumer;

    public EktefelleEllerPartnerFamiliemedlemFilter(PDLConsumer pdlConsumer) {
        this.pdlConsumer = pdlConsumer;
    }

    @NotNull
    public Collection<Familiemedlem> hentEktefelleEllerPartnerFraSivilstander(Collection<Sivilstand> sivilstander) {
        List<Sivilstand> sivilstanderTilHovedperson = sivilstander.stream().toList();

        if (sivilstanderTilHovedperson.isEmpty()) {
            return Collections.emptySet();
        }

        if (sivilstanderTilHovedperson.size() > 1) {
            List<Sivilstand> aktuelleSivilstanderTilHovedperson =
                hentSivilstanderSomIkkeHarRedigertGiftOgSkiltEllerSeparertOgSkiltPåSammeTidspunkt(sivilstanderTilHovedperson);

            Optional<Sivilstand> sisteSivilstand = hentSisteSivilstand(aktuelleSivilstanderTilHovedperson);
            return hentEktefelleEllerPartnerFraSivilstander(sisteSivilstand.get());
        }

        return hentEktefelleEllerPartnerFraSivilstander(sivilstanderTilHovedperson.get(0));
    }

    @NotNull
    private Optional<Sivilstand> hentSisteSivilstand(List<Sivilstand> aktuelleSivilstanderTilHovedperson) {
        return aktuelleSivilstanderTilHovedperson.stream().min(this::sammenlignSisteDatoRegistrert);
    }

    private List<Sivilstand> hentSivilstanderSomIkkeHarRedigertGiftOgSkiltEllerSeparertOgSkiltPåSammeTidspunkt(List<Sivilstand> sivilstanderTilHovedperson) {
        return sivilstanderTilHovedperson.stream()
            .collect(Collectors.groupingBy(HarMetadata::hentDatoSistRegistrert)).values().stream()
            .map(this::lagSivilstanderSomIkkeHarRedigertGiftOgSkiltEllerSeparertOgSkiltSamtidig)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream).toList();
    }

    private List<Sivilstand> lagSivilstanderSomIkkeHarRedigertGiftOgSkiltEllerSeparertOgSkiltSamtidig(List<Sivilstand> sivilstandList) {
        List<Sivilstandstype> sivilstandstyper = sivilstandList.stream().map(Sivilstand::type).toList();

        if (erGiftSamtidigSomSeparertUtenÅVæreSkilt(sivilstandstyper)) {
            return hentSeparertSivilstand(sivilstandList);
        }

        return harIkkeRedigertGiftOgSkiltEllerSeparertOgSkiltSamtidig(sivilstandstyper) ? sivilstandList : null;
    }

    @NotNull
    private List<Sivilstand> hentSeparertSivilstand(List<Sivilstand> sivilstandList) {
        return sivilstandList.stream().filter(sivilstand -> sivilstand.type() == SEPARERT).toList();
    }

    private boolean erGiftSamtidigSomSeparertUtenÅVæreSkilt(List<Sivilstandstype> sivilstandstyper) {
        return sivilstandstyper.containsAll(List.of(GIFT, SEPARERT)) && !sivilstandstyper.contains(SKILT);
    }

    private boolean harIkkeRedigertGiftOgSkiltEllerSeparertOgSkiltSamtidig(List<Sivilstandstype> sivilstandstyper) {
        return !sivilstandstyper.containsAll(List.of(GIFT, SKILT)) && !sivilstandstyper.containsAll(List.of(SEPARERT, SKILT));
    }

    @NotNull
    private Set<Familiemedlem> hentEktefelleEllerPartnerFraSivilstander(Sivilstand sivilstand) {
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
