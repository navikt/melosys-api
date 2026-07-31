package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.PersonAdresse
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

/**
 * Registeret over placeholdere til brev. Registeret er kode, ikke database: én definisjon per felt.
 */
@Component
class PlaceholderRegister {

    val definisjoner: List<PlaceholderDefinisjon> = listOf(
        PlaceholderDefinisjon(
            nokkel = "saksnummer",
            visningsnavn = "Saksnummer",
            beskrivelse = "Sakens saksnummer i Melosys",
            eksempel = { "MEL-12345" },
            resolver = { verdi(it.saksnummer) },
        ),
        PlaceholderDefinisjon(
            nokkel = "dagens-dato",
            visningsnavn = "Dagens dato",
            beskrivelse = "Dagens dato på formatet dd.MM.yyyy",
            eksempel = { iDag() },
            resolver = { verdi(iDag()) },
        ),
        PlaceholderDefinisjon(
            nokkel = "fornavn",
            visningsnavn = "Fornavn",
            beskrivelse = "Brukerens fornavn",
            eksempel = { "Ola" },
            resolver = { verdi(it.persondata.fornavn) },
        ),
        PlaceholderDefinisjon(
            nokkel = "etternavn",
            visningsnavn = "Etternavn",
            beskrivelse = "Brukerens etternavn",
            eksempel = { "Nordmann" },
            resolver = { verdi(it.persondata.etternavn) },
        ),
        PlaceholderDefinisjon(
            nokkel = "fodselsdato",
            visningsnavn = "Fødselsdato",
            beskrivelse = "Brukerens fødselsdato på formatet dd.MM.yyyy",
            eksempel = { "15.03.2024" },
            resolver = { dato(it.persondata.fødselsdato) },
        ),
        PlaceholderDefinisjon(
            nokkel = "fodselsnummer",
            visningsnavn = "Fødselsnummer",
            beskrivelse = "Brukerens fødselsnummer",
            eksempel = { "12345678901" },
            resolver = { verdi(it.persondata.hentFolkeregisterident()) },
        ),
        PlaceholderDefinisjon(
            nokkel = "lovvalgsperiode-fra",
            visningsnavn = "Lovvalgsperiode fra",
            beskrivelse = "Startdatoen for lovvalgsperioden i behandlingsresultatet",
            eksempel = { "01.03.2024" },
            sakstyper = LOVVALGSSAKER,
            // Domenet tillater bare én lovvalgsperiode, så feltet har aldri kandidater
            resolver = { dato(it.sakskontekst.lovvalgsperiode?.fom) },
        ),
        PlaceholderDefinisjon(
            nokkel = "lovvalgsperiode-til",
            visningsnavn = "Lovvalgsperiode til",
            beskrivelse = "Sluttdatoen for lovvalgsperioden i behandlingsresultatet. Utelates ved åpen sluttdato",
            eksempel = { "28.02.2027" },
            sakstyper = LOVVALGSSAKER,
            resolver = { dato(it.sakskontekst.lovvalgsperiode?.tom) },
        ),
        PlaceholderDefinisjon(
            nokkel = "medlemskapsperiode-fra",
            visningsnavn = "Medlemskapsperiode fra",
            beskrivelse = "Startdatoen for de innvilgede periodene i behandlingsresultatet. Enkeltperiodene er alternativer",
            eksempel = { "01.03.2024" },
            resolver = { kontekst ->
                kontekst.sakskontekst.medlemskapsperioder()?.let { perioder ->
                    dato(kontekst.sakskontekst.medlemskapsperiodeFom, perioder.mapNotNull { it.fom })
                }
            },
        ),
        PlaceholderDefinisjon(
            nokkel = "medlemskapsperiode-til",
            visningsnavn = "Medlemskapsperiode til",
            beskrivelse = "Sluttdatoen for de innvilgede periodene i behandlingsresultatet. Utelates ved åpen sluttdato",
            eksempel = { "28.02.2027" },
            resolver = { kontekst ->
                kontekst.sakskontekst.medlemskapsperioder()?.let { perioder ->
                    dato(kontekst.sakskontekst.medlemskapsperiodeTom, perioder.mapNotNull { it.tom })
                }
            },
        ),
        PlaceholderDefinisjon(
            nokkel = "soknadsperiode-fra",
            visningsnavn = "Søknadsperiode fra",
            beskrivelse = "Startdatoen for perioden det er søkt om. Behandlingens periode er forhåndsvalgt",
            eksempel = { "01.03.2024" },
            resolver = { kontekst -> periodefelt(kontekst.sakskontekst.soknadsperioder) { it.fom } },
        ),
        PlaceholderDefinisjon(
            nokkel = "soknadsperiode-til",
            visningsnavn = "Søknadsperiode til",
            beskrivelse = "Sluttdatoen for perioden det er søkt om. Utelates når den forhåndsvalgte perioden er åpen",
            eksempel = { "28.02.2027" },
            resolver = { kontekst -> periodefelt(kontekst.sakskontekst.soknadsperioder) { it.tom } },
        ),
        PlaceholderDefinisjon(
            nokkel = "bostedsadresse",
            visningsnavn = "Bostedsadresse",
            beskrivelse = "Brukerens gjeldende bostedsadresse på én linje",
            eksempel = { "Storgata 1, 0155, Oslo, Norge" },
            resolver = { verdi(it.bostedsadresse()?.tilAdresselinje(it.landnavn)) },
        ),
        PlaceholderDefinisjon(
            nokkel = "bosted-postnummer",
            visningsnavn = "Postnummer i bostedsadressen",
            beskrivelse = "Postnummeret i brukerens gjeldende bostedsadresse",
            eksempel = { "0155" },
            resolver = { verdi(it.bostedsadresse()?.strukturertAdresse?.postnummer) },
        ),
        PlaceholderDefinisjon(
            nokkel = "bosted-poststed",
            visningsnavn = "Poststed i bostedsadressen",
            beskrivelse = "Poststedet i brukerens gjeldende bostedsadresse (settes kun for norske adresser)",
            eksempel = { "Oslo" },
            resolver = { verdi(it.bostedsadresse()?.strukturertAdresse?.poststed) },
        ),
        PlaceholderDefinisjon(
            nokkel = "oppholdsadresse",
            visningsnavn = "Oppholdsadresse",
            beskrivelse = "Brukerens oppholdsadresse på én linje. Alle registrerte oppholdsadresser er alternativer",
            eksempel = { "Strandveien 3, 5003, Bergen, Norge" },
            resolver = { kontekst -> adresser(kontekst.persondata.oppholdsadresseKandidater(), kontekst.landnavn) },
        ),
        PlaceholderDefinisjon(
            nokkel = "kontaktadresse",
            visningsnavn = "Kontaktadresse",
            beskrivelse = "Brukerens kontaktadresse på én linje. Alle registrerte kontaktadresser er alternativer",
            eksempel = { "Postboks 5, 0028, Oslo, Norge" },
            resolver = { kontekst -> adresser(kontekst.persondata.kontaktadresseKandidater(), kontekst.landnavn) },
        ),
        PlaceholderDefinisjon(
            nokkel = "arbeidsgiver-norge",
            visningsnavn = "Arbeidsgiver i Norge",
            beskrivelse = "Navnet på den norske arbeidsgiveren i saken. Avklarte arbeidsgivere brukes når de finnes, " +
                "ellers de oppgitte fra søknaden. Alle er alternativer",
            eksempel = { "Nordisk Verksted AS" },
            resolver = { tekster(it.norskeArbeidsgivernavn) },
        ),
        PlaceholderDefinisjon(
            nokkel = "arbeidsgiver-utland",
            visningsnavn = "Arbeidsgiver i utlandet",
            beskrivelse = "Navnet på den utenlandske arbeidsgiveren i saken. Avklarte arbeidsgivere brukes når de finnes, " +
                "ellers de oppgitte fra søknaden. Alle er alternativer",
            eksempel = { "Nordwerk GmbH" },
            resolver = { tekster(it.sakskontekst.utenlandskeArbeidsgivere) },
        ),
        PlaceholderDefinisjon(
            nokkel = "arbeidsland",
            visningsnavn = "Arbeidsland",
            beskrivelse = "Landet arbeidet utføres i. Alle arbeidsland uten marginalt arbeid er alternativer",
            eksempel = { "Tyskland" },
            resolver = { tekster(it.sakskontekst.arbeidsland) },
        ),
    )

    private companion object {
        private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val LOVVALGSSAKER = listOf(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE)

        private fun iDag(): String = LocalDate.now().format(DATOFORMAT)

        private fun verdi(verdi: String?): PlaceholderResultat? = verdi?.let { PlaceholderResultat(it) }

        /** Konvensjonen: forhåndsvalget er første kandidat, resten følger i rekkefølgen kilden ga dem. */
        private fun resultat(verdi: String, kandidater: List<String>): PlaceholderResultat =
            PlaceholderResultat(verdi, listOf(verdi) + kandidater.filterNot { it == verdi })

        private fun dato(dato: LocalDate?, kandidater: List<LocalDate> = emptyList()): PlaceholderResultat? =
            dato?.let { valgt -> resultat(valgt.format(DATOFORMAT), kandidater.map { it.format(DATOFORMAT) }) }

        /** Verdien leses fra den forhåndsvalgte perioden og kandidatene fra hele perioder, så fra og til aldri krysser kilder. */
        private fun periodefelt(perioder: List<PeriodeData>, felt: (PeriodeData) -> LocalDate?): PlaceholderResultat? =
            dato(perioder.firstOrNull()?.let(felt), perioder.mapNotNull(felt))

        private fun tekster(tekster: List<String>): PlaceholderResultat? =
            tekster.firstOrNull()?.let { resultat(it, tekster) }

        private fun adresser(adresser: List<PersonAdresse>, landnavn: (String?) -> String): PlaceholderResultat? =
            tekster(adresser.mapNotNull { it.tilAdresselinje(landnavn) })

        private fun PlaceholderKontekst.bostedsadresse(): Bostedsadresse? = persondata.finnBostedsadresse().getOrNull()
    }
}
