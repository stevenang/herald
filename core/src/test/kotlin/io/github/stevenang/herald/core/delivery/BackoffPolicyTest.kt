package io.github.stevenang.herald.core.delivery

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import kotlin.random.Random

internal class BackoffPolicyTest {

    @Test
    fun `throws when baseDelayMills is not greater than 0`() {
        val exception = assertThrows<IllegalArgumentException> {
            BackoffPolicy(0L, 1000L, 3)
        }
        assertEquals("baseDelayMillis must be greater than 0", exception.message)
    }

    @Test
    fun `throws when maxDelayMillis must be greater than or equal to baseDelayMillis`() {
        val exception = assertThrows<IllegalArgumentException> {
            BackoffPolicy(1000L, 500L, 3)
        }
        assertEquals("maxDelayMillis must be greater than or equal to baseDelayMillis", exception.message)
    }

    @Test
    fun `throws when maxAttempts must be greater than 0`() {
        val exception = assertThrows<IllegalArgumentException> {
            BackoffPolicy(1000L, 5000L, 0)
        }
        assertEquals("maxAttempts must be greater than 0", exception.message)
    }

    @Test
    fun `test constructor with valid parameters`() {
        val policy = BackoffPolicy(1000L, 5000L, 3)
        assertEquals(1000L, policy.baseDelayMillis)
        assertEquals(5000L, policy.maxDelayMillis)
        assertEquals(3, policy.maxAttempts)
    }

    @Test
    fun `test nextDelay with exponential growth`() {
        val policy = BackoffPolicy(1000L, 5000L, 3)
        assertTrue(policy.nextDelay(1).inWholeMilliseconds in 500L until 1000L)
        assertTrue(policy.nextDelay(2).inWholeMilliseconds in 1000L until 2000L)
        assertTrue(policy.nextDelay(3).inWholeMilliseconds in 2000L until 4000L)
        assertTrue(policy.nextDelay(4).inWholeMilliseconds in 2500L until 5000L)
    }

    @Test
    fun `test nextDelay with attempt = 0`() {
        val policy = BackoffPolicy(1000L, 5000L, 3)
        val exception = assertThrows<IllegalArgumentException> {
            policy.nextDelay(0)
        }
        assertEquals("attempt must be greater than 0", exception.message)
    }

    @Test
    fun `test isExhausted with various attempts`() {
        val policy = BackoffPolicy(1000L, 5000L, 3)
        assertTrue(policy.isExhausted(3))
        assertTrue(policy.isExhausted(4))
        assertTrue(!policy.isExhausted(2))
    }

    @Test
    fun `test same seed produce the same delay`() {
        val policy = BackoffPolicy(1000L, 5000L, 3)
        val delay1 = policy.nextDelay(2, Random(42))
        val delay2 = policy.nextDelay(2, Random(42))
        assertEquals(delay1, delay2)
    }
}