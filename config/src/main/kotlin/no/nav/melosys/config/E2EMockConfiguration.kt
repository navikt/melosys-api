package no.nav.melosys.config

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.util.*

/**
 * E2E Mock Configuration for backend service-layer mocking (Alternativ 3b)
 *
 * Mocker repository-laget med in-memory implementations.
 * Controllers og Services kjører som normalt, men uten database-aksess.
 *
 * Aktiveres med: -Dspring.profiles.active=e2e-mock
 */
@Configuration
@Profile("e2e-mock")
class E2EMockConfiguration {

    @Bean
    @Primary
    fun mockFagsakRepository(): FagsakRepository {
        return object : FagsakRepository {
            private val fagsaker = mutableMapOf<String, Fagsak>()

            init {
                // MEL-1016: FTRL/Utenfor avtaleland
                fagsaker["MEL-1016"] = Fagsak(
                    saksnummer = "MEL-1016",
                    type = Sakstyper.FTRL,
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                    status = Saksstatuser.OPPRETTET
                )
            }

            override fun findBySaksnummer(saksnummer: String): Optional<Fagsak> {
                return Optional.ofNullable(fagsaker[saksnummer])
            }

            override fun findByGsakSaksnummer(gsakSaksnummer: Long): Optional<Fagsak> {
                return fagsaker.values.find { it.gsakSaksnummer == gsakSaksnummer }
                    ?.let { Optional.of(it) } ?: Optional.empty()
            }

            override fun findByRolleAndAktør(rolle: Aktoersroller, aktørID: String): List<Fagsak> {
                return emptyList()
            }

            override fun findByRolleAndOrgnr(rolle: Aktoersroller, orgnr: String): List<Fagsak> {
                return emptyList()
            }

            override fun hentNesteSekvensVerdi(): Long {
                return 2000L
            }

            override fun findAllBySaksnummerIn(saksnumre: Collection<String>): List<Fagsak> {
                return saksnumre.mapNotNull { fagsaker[it] }
            }

            // CrudRepository methods
            override fun <S : Fagsak> save(entity: S): S {
                fagsaker[entity.saksnummer] = entity
                @Suppress("UNCHECKED_CAST")
                return entity as S
            }

            override fun <S : Fagsak> saveAll(entities: Iterable<S>): Iterable<S> {
                entities.forEach { save(it) }
                return entities
            }

            override fun findById(id: String): Optional<Fagsak> {
                return Optional.ofNullable(fagsaker[id])
            }

            override fun existsById(id: String): Boolean {
                return fagsaker.containsKey(id)
            }

            override fun findAll(): Iterable<Fagsak> {
                return fagsaker.values
            }

            override fun findAllById(ids: Iterable<String>): Iterable<Fagsak> {
                return ids.mapNotNull { fagsaker[it] }
            }

            override fun count(): Long {
                return fagsaker.size.toLong()
            }

            override fun deleteById(id: String) {
                fagsaker.remove(id)
            }

            override fun delete(entity: Fagsak) {
                fagsaker.remove(entity.saksnummer)
            }

            override fun deleteAllById(ids: Iterable<String>) {
                ids.forEach { fagsaker.remove(it) }
            }

            override fun deleteAll(entities: Iterable<Fagsak>) {
                entities.forEach { fagsaker.remove(it.saksnummer) }
            }

            override fun deleteAll() {
                fagsaker.clear()
            }
        }
    }

    @Bean
    @Primary
    fun mockBehandlingRepository(fagsakRepository: FagsakRepository): BehandlingRepository {
        return object : BehandlingRepository {
            private val behandlinger = mutableMapOf<Long, Behandling>()

            init {
                // Behandling for MEL-1016
                val fagsak = fagsakRepository.findBySaksnummer("MEL-1016").get()
                val behandling = Behandling(
                    id = 16L,
                    fagsak = fagsak,
                    status = Behandlingsstatus.UNDER_BEHANDLING,
                    type = Behandlingstyper.FØRSTEGANG,
                    tema = Behandlingstema.YRKESAKTIV,
                    behandlingsfrist = LocalDate.now().plusDays(30)
                )
                behandlinger[16L] = behandling
            }

            override fun findWithSaksopplysningerById(behandlingID: Long): Behandling? {
                return behandlinger[behandlingID]
            }

            override fun findIdsByStatus(behandlingsstatus: Behandlingsstatus): Collection<Long> {
                return behandlinger.values
                    .filter { it.status == behandlingsstatus }
                    .map { it.id }
            }

            override fun findById(id: Long): Optional<Behandling> {
                return Optional.ofNullable(behandlinger[id])
            }

            // CrudRepository methods
            override fun <S : Behandling> save(entity: S): S {
                behandlinger[entity.id] = entity
                return entity
            }

            override fun <S : Behandling> saveAll(entities: MutableIterable<S>): MutableIterable<S> {
                entities.forEach { save(it) }
                return entities
            }

            override fun existsById(id: Long): Boolean = behandlinger.containsKey(id)
            override fun findAll(): MutableIterable<Behandling> = behandlinger.values.toMutableList()
            override fun findAllById(ids: MutableIterable<Long>): MutableIterable<Behandling> {
                return ids.mapNotNull { behandlinger[it] }.toMutableList()
            }
            override fun count(): Long = behandlinger.size.toLong()
            override fun deleteById(id: Long) {}
            override fun delete(entity: Behandling) {}
            override fun deleteAllById(ids: MutableIterable<Long>) {}
            override fun deleteAll(entities: MutableIterable<Behandling>) {}
            override fun deleteAll() {}
        }
    }
}
