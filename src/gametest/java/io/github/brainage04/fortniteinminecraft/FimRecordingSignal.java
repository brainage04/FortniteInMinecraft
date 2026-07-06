package io.github.brainage04.fortniteinminecraft;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class FimRecordingSignal {
    private static final String START_SIGNAL_ENV = "FIM_CLIENT_GAMETEST_RECORDING_START_SIGNAL";
    private static final String READY_SIGNAL_ENV = "FIM_CLIENT_GAMETEST_RECORDING_READY_SIGNAL";
    private static final int READY_TIMEOUT_TICKS = 200;

    static void signalReadyToRecord(ClientGameTestContext context) {
        String startSignal = System.getenv(START_SIGNAL_ENV);
        if (startSignal == null || startSignal.isBlank()) {
            return;
        }

        writeSignal(startSignal);

        String readySignal = System.getenv(READY_SIGNAL_ENV);
        if (readySignal == null || readySignal.isBlank()) {
            return;
        }

        context.waitFor(_ -> Files.exists(Path.of(readySignal)), READY_TIMEOUT_TICKS);
    }

    private static void writeSignal(String signalPath) {
        Path path = Path.of(signalPath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, Long.toString(System.currentTimeMillis()));
        } catch (IOException exception) {
            throw new AssertionError("Expected to write client GameTest recording signal: " + path, exception);
        }
    }
}
