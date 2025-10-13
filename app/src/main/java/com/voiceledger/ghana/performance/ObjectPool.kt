package com.voiceledger.ghana.performance

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Generic object pool for reusing expensive objects
 * Helps reduce garbage collection pressure and memory allocations
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
    private val validator: (T) -> Boolean = { true },
    private val maxSize: Int = 10
) {
    private val pool = ConcurrentLinkedQueue<T>()
    private val currentSize = AtomicInteger(0)
    private val mutex = Mutex()

    /**
     * Acquire an object from the pool or create a new one
     */
    suspend fun acquire(): T {
        return mutex.withLock {
            val obj = pool.poll()
            if (obj != null && validator(obj)) {
                currentSize.decrementAndGet()
                obj
            } else {
                factory()
            }
        }
    }

    /**
     * Return an object to the pool
     */
    suspend fun release(obj: T) {
        mutex.withLock {
            if (currentSize.get() < maxSize && validator(obj)) {
                reset(obj)
                pool.offer(obj)
                currentSize.incrementAndGet()
            }
        }
    }

    /**
     * Clear all objects from the pool
     */
    suspend fun clear() {
        mutex.withLock {
            pool.clear()
            currentSize.set(0)
        }
    }

    /**
     * Get current pool size
     */
    fun size(): Int = currentSize.get()
}

/**
 * Audio buffer pool for reusing byte arrays
 */
class AudioBufferPool(
    private val bufferSize: Int = 4096,
    maxPoolSize: Int = 20
) {
    private val pool = ObjectPool<ByteArray>(
        factory = { ByteArray(bufferSize) },
        reset = { buffer -> buffer.fill(0) },
        validator = { buffer -> buffer.size == bufferSize },
        maxSize = maxPoolSize
    )

    suspend fun acquire(): ByteArray = pool.acquire()
    suspend fun release(buffer: ByteArray) = pool.release(buffer)
    suspend fun clear() = pool.clear()
    fun size(): Int = pool.size()
}

/**
 * Float array pool for audio processing
 */
class FloatArrayPool(
    private val arraySize: Int = 4096,
    maxPoolSize: Int = 15
) {
    private val pool = ObjectPool<FloatArray>(
        factory = { FloatArray(arraySize) },
        reset = { array -> array.fill(0f) },
        validator = { array -> array.size == arraySize },
        maxSize = maxPoolSize
    )

    suspend fun acquire(): FloatArray = pool.acquire()
    suspend fun release(array: FloatArray) = pool.release(array)
    suspend fun clear() = pool.clear()
    fun size(): Int = pool.size()
}

/**
 * StringBuilder pool for string operations
 */
class StringBuilderPool(
    private val initialCapacity: Int = 256,
    maxPoolSize: Int = 10
) {
    private val pool = ObjectPool<StringBuilder>(
        factory = { StringBuilder(initialCapacity) },
        reset = { sb -> sb.clear() },
        validator = { sb -> sb.capacity >= initialCapacity },
        maxSize = maxPoolSize
    )

    suspend fun acquire(): StringBuilder = pool.acquire()
    suspend fun release(sb: StringBuilder) = pool.release(sb)
    suspend fun clear() = pool.clear()
    fun size(): Int = pool.size()
}

/**
 * Pool manager for coordinating multiple object pools
 */
class PoolManager {
    private val pools = mutableMapOf<String, ObjectPool<*>>()
    private val mutex = Mutex()

    /**
     * Register a pool with a name
     */
    suspend fun <T> registerPool(name: String, pool: ObjectPool<T>) {
        mutex.withLock {
            pools[name] = pool
        }
    }

    /**
     * Clear all pools
     */
    suspend fun clearAll() {
        mutex.withLock {
            pools.values.forEach { pool ->
                @Suppress("UNCHECKED_CAST")
                (pool as ObjectPool<Any>).clear()
            }
        }
    }

    /**
     * Get pool statistics
     */
    suspend fun getStatistics(): Map<String, Int> {
        return mutex.withLock {
            pools.mapValues { (_, pool) -> pool.size() }
        }
    }
}

/**
 * Scoped object pool usage
 */
suspend inline fun <T, R> ObjectPool<T>.use(block: (T) -> R): R {
    val obj = acquire()
    try {
        return block(obj)
    } finally {
        release(obj)
    }
}