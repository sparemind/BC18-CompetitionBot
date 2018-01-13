import bc.Direction;
import bc.GameController;

public abstract class PlanetPlayer {
    // Initial time in the time pool in milliseconds (10 seconds)
    static final int INITIAL_TIME = 10 * 1000;
    // Number of milliseconds added to the time pool each turn
    static final int TIME_INCREMENT = 50;
    static final Direction[] DIRECTIONS = Direction.values();

    protected GameController gc;
    // Number of milliseconds remaining in the time pool
    protected long timePool;
    // Timestamp of the turn start in milliseconds
    protected long turnStartTimestamp;

    /**
     * Creates a new player.
     *
     * @param gc The game controller for this player.
     */
    public PlanetPlayer(GameController gc) {
        this.gc = gc;
        this.timePool = INITIAL_TIME;
    }

    /**
     * Processes any actions that must happen before a turn.
     */
    public void processPreTurn() {
        this.timePool += TIME_INCREMENT;
        this.turnStartTimestamp = System.currentTimeMillis();
    }

    /**
     * Processes a single turn.
     */
    public abstract void processTurn();

    /**
     * Processes any actions that must happen at the end of a turn.
     */
    public void processPostTurn() {
        this.timePool = getTimeLeft();
    }

    /**
     * Returns a conservative estimate the amount of time left in the time pool.
     *
     * @return The number of milliseconds left in this player's time pool.
     */
    public long getTimeLeft() {
        // Subtract an extra millisecond to "round up" and give a safer estimate
        return this.timePool - (System.currentTimeMillis() - this.turnStartTimestamp) - 1;
    }
}
