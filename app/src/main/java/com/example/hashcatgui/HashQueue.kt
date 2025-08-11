package com.example.hashcatgui

data class Hash(
    val hash: String,
    var possibleHashTypes: List<HashcatMode> = emptyList(),
    var verifiedHashType: HashcatMode? = null,
    var password: String? = null
)

class HashQueue(private val pythonCracker: PythonCracker) {
    val hashes = mutableListOf<Hash>()

    fun addHash(hash: String) {
        hashes.add(Hash(hash))
    }

    fun removeHash(hash: Hash) {
        hashes.remove(hash)
    }

    fun attack(hash: Hash, wordlistPath: String, hashAlgorithm: String) {
        val password = pythonCracker.dictionaryAttack(hash.hash, wordlistPath, hashAlgorithm)
        if (password != null) {
            hash.password = password
        }
    }
}
