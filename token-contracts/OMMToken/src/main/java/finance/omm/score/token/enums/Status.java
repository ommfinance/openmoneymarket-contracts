package finance.omm.score.token.enums;

public enum Status {
    STAKED(1),
    UNSTAKING(2),
    UNSTAKING_PERIOD(3);

    private final int key;

    Status(int key) {
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }
}
