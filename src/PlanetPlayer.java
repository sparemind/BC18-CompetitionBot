import bc.*;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public abstract class PlanetPlayer {
    // Initial time in the time pool in milliseconds (10 seconds)
    static final int INITIAL_TIME = 10 * 1000;
    // Number of milliseconds added to the time pool each turn
    static final int TIME_INCREMENT = 50;
    static final Direction[] DIRECTIONS = Direction.values();

    // Represents impassable terrain in the game map.
    static final int IMPASSABLE = -1;
    // What team this player is on
    protected final Team MY_TEAM;
    // What team the enemy is
    protected final Team ENEMY_TEAM;
    // This player's game controller
    protected GameController gc;
    // Number of milliseconds remaining in the time pool
    protected long timePool;
    // Timestamp of the turn start in milliseconds
    protected long turnStartTimestampMillis;
    // Map of all locations on the planet, represented as the amount of
    // karbonite at each location. -1 signifies impassable terrain.
    protected int[][] map;
    protected Map<Point, Direction[][]> navMaps;
    // Key: UnitID, Value: Unit
    protected Map<Integer, Unit> allUnits;
    // Key: UnitType, Value: Set of all my units of that type
    protected Map<UnitType, Set<Integer>> myUnits;

    /**
     * Creates a new player.
     *
     * @param gc     The game controller for this player.
     * @param planet The planet this player is on.
     */
    public PlanetPlayer(GameController gc, Planet planet) {
        this.turnStartTimestampMillis = System.currentTimeMillis();
        this.gc = gc;
        this.MY_TEAM = gc.team();
        if (this.MY_TEAM == Team.Blue) {
            this.ENEMY_TEAM = Team.Red;
        } else {
            this.ENEMY_TEAM = Team.Blue;
        }
        this.timePool = INITIAL_TIME;

        // Create karbonite map
        PlanetMap pm = gc.startingMap(planet);
        this.map = new int[(int) pm.getHeight()][(int) pm.getWidth()];
        this.navMaps = new HashMap<>();
        for (int y = 0; y < this.map.length; y++) {
            for (int x = 0; x < this.map[y].length; x++) {
                MapLocation loc = new MapLocation(planet, x, y);
                if (pm.isPassableTerrainAt(loc) == 1) {
                    this.map[y][x] = (int) pm.initialKarboniteAt(loc);
                } else {
                    this.map[y][x] = IMPASSABLE;
                }
            }
        }

        // Create navigation maps
        for (int y = 0; y < this.map.length; y++) {
            for (int x = 0; x < this.map[y].length; x++) {
                // Skip making a navmap if this location is impassable
                // if (this.map[y][x] == IMPASSABLE) {
                //     continue;
                // }

                MapLocation start = new MapLocation(planet, x, y);
                Queue<MapLocation> openSet = new LinkedList<>();
                Direction[][] navMap = new Direction[this.map.length][this.map[y].length];
                for (Direction d : DIRECTIONS) {
                    MapLocation adj = start.add(d);

                    int adjX = adj.getX();
                    int adjY = adj.getY();
                    if (adjX < 0 || adjY < 0 || adjX >= this.map[y].length || adjY >= this.map.length || this.map[adjY][adjX] == IMPASSABLE) {
                        continue;
                    }

                    openSet.add(adj);
                    navMap[adjY][adjX] = bc.bcDirectionOpposite(d);
                }
                while (!openSet.isEmpty()) {
                    MapLocation next = openSet.remove();
                    for (Direction d : DIRECTIONS) {
                        MapLocation adj = next.add(d);
                        int adjX = adj.getX();
                        int adjY = adj.getY();
                        if (isOOB(adjX, adjY) || this.map[adjY][adjX] == IMPASSABLE) {
                            continue;
                        }
                        if (navMap[adjY][adjX] == null) {
                            openSet.add(adj);
                            navMap[adjY][adjX] = bc.bcDirectionOpposite(d);
                        }
                    }
                }
                this.navMaps.put(new Point(x, y), navMap);
            }
        }

        // Set up unit lookup map
        this.allUnits = new HashMap<>();

        // Set up units map
        this.myUnits = new HashMap<>();
        for (UnitType type : UnitType.values()) {
            this.myUnits.put(type, new HashSet<>());
        }

        this.timePool = getTimeLeft();
    }

    /**
     * Processes any actions that must happen before a turn.
     */
    public void processPreTurn() {
        this.timePool += TIME_INCREMENT;
        this.turnStartTimestampMillis = System.currentTimeMillis();

        // Update unit maps
        for (UnitType type : this.myUnits.keySet()) {
            this.myUnits.get(type).clear();
        }
        VecUnit units = this.gc.units();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (unit.team() == this.MY_TEAM) {
                this.myUnits.get(unit.unitType()).add(unit.id());
            }
            this.allUnits.put(unit.id(), unit);
        }
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
        System.out.println("Ending round " + this.gc.round() + " with " + this.timePool + "ms remaining.");
    }

    /**
     * Returns an estimate the amount of time left in the time pool.
     *
     * @return The number of milliseconds left in this player's time pool,
     * rounded down.
     */
    public long getTimeLeft() {
        // Subtract an extra millisecond to "round down" and give a safer estimate
        return this.timePool - (System.currentTimeMillis() - this.turnStartTimestampMillis) - 1;
    }

    /**
     * Returns whether a given location is out of bounds.
     *
     * @param x The x-coordinate of the location to check.
     * @param y The y-coordinate of the location to check.
     * @return True if the location is out of bounds, false otherwise.
     */
    protected boolean isOOB(int x, int y) {
        return x < 0 || y < 0 || x >= this.map[0].length || y >= this.map.length;
    }
}
