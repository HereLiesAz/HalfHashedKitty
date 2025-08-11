package com.example.hashcatgui

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream

@RunWith(MockitoJUnitRunner::class)
class HashTypeIdentifierTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var hashTypeIdentifier: HashTypeIdentifier

    @Before
    fun setup() {
        val modes = "0 MD5\n100 SHA1\n1400 SHA2-256\n1700 SHA2-512"
        val inputStream = ByteArrayInputStream(modes.toByteArray())
        `when`(mockContext.resources.openRawResource(R.raw.modes)).thenReturn(inputStream)
        hashTypeIdentifier = HashTypeIdentifier(mockContext)
    }

    @Test
    fun `identify should return correct hash type for MD5`() {
        val hash = "d41d8cd98f00b204e9800998ecf8427e"
        val result = hashTypeIdentifier.identify(hash)
        assertEquals(1, result.size)
        assertEquals("MD5", result[0].name)
    }
}
