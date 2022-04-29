package soccer.model;

import java.util.Objects;
import java.util.UUID;

public class PlayerId implements Comparable<PlayerId> {

    private UUID _uuid;

    public PlayerId() {
        this(null);
    }

    public PlayerId(UUID uuid) {

        setUUID(uuid);
    }

    public UUID getUUID() {
        return _uuid;
    }

    public void setUUID(UUID uuid) {
        _uuid = uuid;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(getUUID());
    }

    @Override
    public boolean equals(Object object) {

        if (this == object)
            return true;
        if (!(object instanceof PlayerId))
            return false;
        PlayerId that = (PlayerId) object;

        return Objects.equals(this.getUUID(), that.getUUID());
    }

    @Override
    public int compareTo(PlayerId playerId) {

        UUID value1 = getUUID();
        UUID value2 = playerId.getUUID();

        if (value1 == null) {
            if (value2 == null) {
                return 0;
            }
            return 1;
        } else if (value2 == null) {
            return -1;
        }

        return Objects.compare(value1, value2, UUID::compareTo);
    }

    public String toText() {

        UUID uuid = getUUID();
        if (uuid == null) {
            return "NO ID";
        }

        return uuid.toString();
    }

    @Override
    public String toString() {
        return toText();
    }

    public static PlayerId with(String text) {

        return new PlayerId(UUID.fromString(text));
    }

    public static PlayerId random() {

        return new PlayerId(UUID.randomUUID());
    }
}