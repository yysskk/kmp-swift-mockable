package com.example.sample

/** Kotlin code that Swift tests drive with generated mocks. */
class UserService(
    private val repository: UserRepository,
    private val analytics: Analytics,
) {
    suspend fun displayName(id: Int): String {
        val user = repository.cachedUser?.takeIf { it.id == id } ?: repository.fetchUser(id)
        repository.cachedUser = user
        analytics.track("user_loaded", mapOf("id" to id.toString()))
        return user.name
    }

    suspend fun exists(name: String): Boolean = repository.findUser(name) != null

    fun importUsers(users: List<User>) {
        if (users.size == 1) repository.save(users.single()) else repository.save(users)
    }

    fun shutdown() {
        repository.clear()
        repository.close()
    }

    fun repositoryName(): String = repository.name
}
