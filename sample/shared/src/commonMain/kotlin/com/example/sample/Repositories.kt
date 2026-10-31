package com.example.sample

import io.github.yysskk.kmpswiftmockable.SwiftMockable

data class User(val id: Int, val name: String)

/** A parent that is not annotated: its requirements are restated in each mirror. */
interface Closeable {
    fun close()
}

@SwiftMockable
interface Repository {
    val name: String
    fun clear()
}

@SwiftMockable
interface UserRepository : Repository, Closeable {
    suspend fun fetchUser(id: Int): User
    suspend fun findUser(name: String): User?
    var cachedUser: User?
    fun save(user: User)
    fun save(users: List<User>)
}

@SwiftMockable
interface Analytics {
    fun track(event: String, properties: Map<String, String>)
}
