package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.AbstraktOrganisasjon;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.GateadresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.service.kodeverk.KodeverkService.UKJENT;

public class OrganisasjonSerializer extends StdSerializer<AbstraktOrganisasjon> {

    private final transient KodeverkService kodeverkService;

    public OrganisasjonSerializer(KodeverkService kodeverkService) {
        super(AbstraktOrganisasjon.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(AbstraktOrganisasjon organisasjon, JsonGenerator generator, SerializerProvider provider) throws IOException {
        OrganisasjonDto organisasjonDto = new OrganisasjonDto();

        organisasjonDto.setOrgnr(organisasjon.getOrgnummer());
        organisasjonDto.setNavn(organisasjon.getNavn());
        organisasjonDto.setOppstartdato(organisasjon.getOppstartsdato());
        if (StringUtils.isNotEmpty(organisasjon.getEnhetstype())) {
            organisasjonDto.setOrganisasjonsform(kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, organisasjon.getEnhetstype()));
        }

        organisasjonDto.setForretningsadresse(tilAdresseDto(organisasjon.getForretningsadresse()));
        organisasjonDto.setPostadresse(tilAdresseDto(organisasjon.getPostadresse()));

        generator.writeObject(organisasjonDto);
    }

    private AdresseDto tilAdresseDto(StrukturertAdresse adresse) {
        AdresseDto dto = new AdresseDto();
        GateadresseDto gateadresse = new GateadresseDto();
        dto.setGateadresse(gateadresse);

        if (adresse == null) {
            // Tomt objekt til frontend (ikke null)
            return dto;
        }

        gateadresse.setGatenavn(adresse.getGatenavn());

        dto.setPostnr(adresse.getPostnummer());
        String poststed = StringUtils.isNotEmpty(adresse.getPoststed()) ? adresse.getPoststed()
            : kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnummer());
        dto.setPoststed(poststed);

        final String landISO2 = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, adresse.getLandkode());
        final String landkode = !UKJENT.equals(landISO2) ? landISO2
            :  kodeverkService.dekod(FellesKodeverk.LANDKODER, adresse.getLandkode());
        dto.setLand(landkode);

        return dto;
    }
}
