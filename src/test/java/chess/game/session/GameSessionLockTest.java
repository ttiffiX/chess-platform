package chess.game.session;

import chess.feature.game.model.TimeControl;
import chess.feature.game.session.GameSession;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionLockTest {

    @Test
    void shouldSerializeConcurrentOperationsOnSameSession() throws Exception {
        GameSession session = new GameSession(
                TimeControl.untimed(),
                "white",
                "black"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch firstInsideLock = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        AtomicInteger activeOperations = new AtomicInteger();
        AtomicInteger maxConcurrentOperations = new AtomicInteger();

        try {
            Future<?> first = executor.submit(() ->
                    session.runLocked(() -> {
                        int active = activeOperations.incrementAndGet();
                        maxConcurrentOperations.accumulateAndGet(active, Math::max);

                        firstInsideLock.countDown();
                        awaitLatch(releaseFirst);

                        activeOperations.decrementAndGet();
                    })
            );

            assertTrue(
                    firstInsideLock.await(2, TimeUnit.SECONDS),
                    "First thread did not acquire the lock"
            );

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();

                session.runLocked(() -> {
                    int active = activeOperations.incrementAndGet();
                    maxConcurrentOperations.accumulateAndGet(active, Math::max);

                    activeOperations.decrementAndGet();
                });
            });

            assertTrue(
                    secondStarted.await(2, TimeUnit.SECONDS),
                    "Second thread did not start"
            );

            // Thread thứ nhất vẫn đang giữ lock.
            assertFalse(
                    second.isDone(),
                    "Second operation should wait for the first lock holder"
            );

            releaseFirst.countDown();

            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);

            assertEquals(
                    1,
                    maxConcurrentOperations.get(),
                    "Two operations entered the same session simultaneously"
            );

        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReleaseLockWhenOperationThrowsException() {
        GameSession session = new GameSession(
                TimeControl.untimed(),
                "white",
                "black"
        );

        assertThrows(
                IllegalStateException.class,
                () -> session.locked(() -> {
                    throw new IllegalStateException("Test exception");
                })
        );

        // Nếu finally không unlock thì lần gọi này sẽ bị treo.
        assertEquals(
                "success",
                session.locked(() -> "success")
        );
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}