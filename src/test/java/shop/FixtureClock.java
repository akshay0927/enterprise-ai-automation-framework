package shop;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

final class FixtureClock extends Clock {
    private final AtomicReference<Instant> current;
    private final ZoneId zone;

    FixtureClock() {
        this(new AtomicReference<>(Instant.EPOCH), ZoneOffset.UTC);
    }

    private FixtureClock(AtomicReference<Instant> current, ZoneId zone) {
        this.current = current;
        this.zone = zone;
    }

    void advance(Duration duration) {
        current.updateAndGet(instant -> instant.plus(duration));
    }

    @Override public Instant instant() { return current.get(); }
    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId zone) { return new FixtureClock(current, zone); }
}
