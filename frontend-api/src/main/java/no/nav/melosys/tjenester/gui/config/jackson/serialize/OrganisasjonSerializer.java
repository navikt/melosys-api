package no.nav.melosys.tjenester.gui.config.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.AdresseDto;
import no.nav.melosys.tjenester.gui.dto.GateadresseDto;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.service.kodeverk.KodeverkService.UKJENT;


public class OrganisasjonSerializer extends StdSerializer<OrganisasjonDokument> {

    private final transient KodeverkService kodeverkService;

    public OrganisasjonSerializer(KodeverkService kodeverkService) {
        super(OrganisasjonDokument.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(OrganisasjonDokument organisasjon, JsonGenerator generator, SerializerProvider provider) throws IOException {
        OrganisasjonDto organisasjonDto = new OrganisasjonDto();

        organisasjonDto.orgnr = organisasjon.getOrgnummer();
        organisasjonDto.navn = organisasjon.getNavn();
        organisasjonDto.oppstartdato = organisasjon.getOppstartsdato();
        if (StringUtils.isNotEmpty(organisasjon.getEnhetstype())) {
            organisasjonDto.organisasjonsform = kodeverkService.dekod(FellesKodeverk.ENHETSTYPER_JURIDISK_ENHET, organisasjon.getEnhetstype());
        }

        organisasjonDto.forretningsadresse = tilAdresseDto(organisasjon.getForretningsadresse());
        organisasjonDto.postadresse = tilAdresseDto(organisasjon.getPostadresse());

        generator.writeObject(organisasjonDto);
    }

    private AdresseDto tilAdresseDto(StrukturertAdresse adresse) {
        AdresseDto dto = new AdresseDto();
        GateadresseDto gateadresse = new GateadresseDto();
        dto.gateadresse = gateadresse;

        if (adresse == null) {
            // Tomt objekt til frontend (ikke null)
            return dto;
        }

        gateadresse.gatenavn = adresse.getGatenavn();

        dto.postnr = adresse.getPostnummer();
        String poststed = StringUtils.isNotEmpty(adresse.getPoststed()) ? adresse.getPoststed()
            : kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnummer());
        dto.poststed = poststed;

        final String landISO2 = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, adresse.getLandkode());
        final String landkode = !UKJENT.equals(landISO2) ? landISO2
            :  kodeverkService.dekod(FellesKodeverk.LANDKODER, adresse.getLandkode());
        dto.land = landkode;

        return dto;
    }
}
