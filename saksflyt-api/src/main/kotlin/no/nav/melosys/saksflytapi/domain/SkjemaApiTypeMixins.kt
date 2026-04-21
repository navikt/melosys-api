package no.nav.melosys.saksflytapi.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "metadatatype", include = JsonTypeInfo.As.PROPERTY, visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = DegSelvMetadata::class, name = "UTSENDT_ARBEIDSTAKER_DEG_SELV"),
    JsonSubTypes.Type(value = ArbeidsgiverMetadata::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER"),
    JsonSubTypes.Type(value = AnnenPersonMetadata::class, name = "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON"),
    JsonSubTypes.Type(value = ArbeidsgiverMedFullmaktMetadata::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_MED_FULLMAKT"),
    JsonSubTypes.Type(value = RadgiverMedFullmaktMetadata::class, name = "UTSENDT_ARBEIDSTAKER_RADGIVER_MED_FULLMAKT"),
)
abstract class UtsendtArbeidstakerMetadataMixin

abstract class DegSelvMetadataMixin @JsonCreator constructor(
    @JsonProperty("skjemadel") skjemadel: Skjemadel,
    @JsonProperty("arbeidsgiverNavn") arbeidsgiverNavn: String?,
    @JsonProperty("juridiskEnhetOrgnr") juridiskEnhetOrgnr: String?,
    @JsonProperty("kobletSkjemaId") kobletSkjemaId: UUID?,
    @JsonProperty("erstatterSkjemaId") erstatterSkjemaId: UUID?,
)

abstract class ArbeidsgiverMetadataMixin @JsonCreator constructor(
    @JsonProperty("skjemadel") skjemadel: Skjemadel,
    @JsonProperty("arbeidsgiverNavn") arbeidsgiverNavn: String?,
    @JsonProperty("juridiskEnhetOrgnr") juridiskEnhetOrgnr: String?,
    @JsonProperty("kobletSkjemaId") kobletSkjemaId: UUID?,
    @JsonProperty("erstatterSkjemaId") erstatterSkjemaId: UUID?,
)

abstract class AnnenPersonMetadataMixin @JsonCreator constructor(
    @JsonProperty("skjemadel") skjemadel: Skjemadel,
    @JsonProperty("arbeidsgiverNavn") arbeidsgiverNavn: String?,
    @JsonProperty("juridiskEnhetOrgnr") juridiskEnhetOrgnr: String?,
    @JsonProperty("fullmektigFnr") fullmektigFnr: String?,
    @JsonProperty("kobletSkjemaId") kobletSkjemaId: UUID?,
    @JsonProperty("erstatterSkjemaId") erstatterSkjemaId: UUID?,
)

abstract class ArbeidsgiverMedFullmaktMetadataMixin @JsonCreator constructor(
    @JsonProperty("skjemadel") skjemadel: Skjemadel,
    @JsonProperty("arbeidsgiverNavn") arbeidsgiverNavn: String?,
    @JsonProperty("juridiskEnhetOrgnr") juridiskEnhetOrgnr: String?,
    @JsonProperty("fullmektigFnr") fullmektigFnr: String?,
    @JsonProperty("kobletSkjemaId") kobletSkjemaId: UUID?,
    @JsonProperty("erstatterSkjemaId") erstatterSkjemaId: UUID?,
)

abstract class RadgiverMedFullmaktMetadataMixin @JsonCreator constructor(
    @JsonProperty("skjemadel") skjemadel: Skjemadel,
    @JsonProperty("arbeidsgiverNavn") arbeidsgiverNavn: String?,
    @JsonProperty("juridiskEnhetOrgnr") juridiskEnhetOrgnr: String?,
    @JsonProperty("fullmektigFnr") fullmektigFnr: String?,
    @JsonProperty("kobletSkjemaId") kobletSkjemaId: UUID?,
    @JsonProperty("erstatterSkjemaId") erstatterSkjemaId: UUID?,
    @JsonProperty("radgiverfirma") radgiverfirma: RadgiverfirmaInfo?,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY, visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = UtsendtArbeidstakerArbeidstakersSkjemaDataDto::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"),
    JsonSubTypes.Type(value = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL"),
)
interface UtsendtArbeidstakerSkjemaDataMixin

abstract class UtsendtArbeidstakerSkjemaDtoMixin @JsonCreator constructor(
    @JsonProperty("id") id: UUID?,
    @JsonProperty("status") status: SkjemaStatus?,
    @JsonProperty("type") type: SkjemaType?,
    @JsonProperty("fnr") fnr: String?,
    @JsonProperty("orgnr") orgnr: String?,
    @JsonProperty("metadata") metadata: UtsendtArbeidstakerMetadata?,
    @JsonProperty("data") data: UtsendtArbeidstakerSkjemaData?,
)

abstract class UtsendtArbeidstakerSkjemaM2MDtoMixin @JsonCreator constructor(
    @JsonProperty("skjema") skjema: UtsendtArbeidstakerSkjemaDto?,
    @JsonProperty("kobletSkjema") kobletSkjema: UtsendtArbeidstakerSkjemaDto?,
    @JsonProperty("tidligereInnsendteSkjema") tidligereInnsendteSkjema: List<UtsendtArbeidstakerSkjemaDto>?,
    @JsonProperty("referanseId") referanseId: String?,
    @JsonProperty("innsendtTidspunkt") innsendtTidspunkt: LocalDateTime?,
    @JsonProperty("innsenderFnr") innsenderFnr: String?,
)

abstract class SkjemaMottattMeldingMixin @JsonCreator constructor(
    @JsonProperty("skjemaId") skjemaId: UUID,
    @JsonProperty("relaterteSkjemaIder") relaterteSkjemaIder: List<UUID>,
)
