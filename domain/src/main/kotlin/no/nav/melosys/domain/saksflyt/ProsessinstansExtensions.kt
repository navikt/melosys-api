package no.nav.melosys.domain.saksflyt

import com.fasterxml.jackson.core.type.TypeReference

inline fun <reified T> Prosessinstans.getData(key: ProsessDataKey, default: T? = null): T? {
    return this.getData(key,  object : TypeReference<T>() {}, default)
}
