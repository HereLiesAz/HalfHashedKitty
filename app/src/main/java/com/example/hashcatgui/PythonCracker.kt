package com.example.hashcatgui

import com.chaquo.python.Python

class PythonCracker {

    fun dictionaryAttack(hash: String, wordlistPath: String, hashAlgorithm: String): String? {
        val python = Python.getInstance()
        val crackerModule = python.getModule("cracker")
        val result = crackerModule.callAttr("dictionary_attack", hash, wordlistPath, hashAlgorithm)
        return result.toString()
    }
}
