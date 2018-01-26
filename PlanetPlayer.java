import bc.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class PlanetPlayer {
    static final Direction[] DIRECTIONS = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest};
    static Map<Direction, Point> DIR_VECTORS = new HashMap<>();

    static {
        DIR_VECTORS.put(Direction.North, new Point(0, 1));
        DIR_VECTORS.put(Direction.Northeast, new Point(1, 1));
        DIR_VECTORS.put(Direction.East, new Point(1, 0));
        DIR_VECTORS.put(Direction.Southeast, new Point(1, -1));
        DIR_VECTORS.put(Direction.South, new Point(0, -1));
        DIR_VECTORS.put(Direction.Southwest, new Point(-1, -1));
        DIR_VECTORS.put(Direction.West, new Point(-1, 0));
        DIR_VECTORS.put(Direction.Northwest, new Point(-1, 1));
    }

    // Represents impassable terrain in the game map.
    static final int IMPASSABLE = -1;
    // What team this player is on
    protected final Team MY_TEAM;
    // What team the enemy is
    protected final Team ENEMY_TEAM;
    // This player's game controller
    protected GameController gc;
    // The planet this player is on
    protected Planet planet;
    // The navigator for this player
    protected Navigator navigator;
    // Map of all locations on the planet, represented as the amount of
    // karbonite at each location. -1 signifies impassable terrain.
    protected int[][] karboniteMap;
    protected boolean[][] passableMap;
    protected boolean[][] factoryLocationMap;
    protected int mapWidth;
    protected int mapHeight;
    protected Set<Point> impassablePoints;
    // Key: UnitID, Value: Unit
    protected Map<Integer, Unit> allUnits;
    // Key: UnitType, Value: Set of all my units of that type
    protected Map<UnitType, Set<Integer>> myUnits;
    // Key: UnitType, Value: Set of all my units of that type
    protected Map<UnitType, Set<Integer>> oppUnits;
    protected List<MapLocation> attackPoints;
    protected MapLocation rallyPoint;
    protected MapLocation base;
    protected Map<Integer, Integer> stalemateTime;

    /**
     * Creates a new player.
     *
     * @param gc     The game controller for this player.
     * @param planet The planet this player is on.
     */
    public PlanetPlayer(GameController gc, Planet planet) {
        this.gc = gc;
        this.planet = planet;
        this.MY_TEAM = gc.team();
        if (this.MY_TEAM == Team.Blue) {
            this.ENEMY_TEAM = Team.Red;
        } else {
            this.ENEMY_TEAM = Team.Blue;
        }

        // Create planet maps
        PlanetMap pm = gc.startingMap(planet);
        this.mapWidth = (int) pm.getWidth();
        this.mapHeight = (int) pm.getHeight();
        this.impassablePoints = new HashSet<>();
        this.karboniteMap = new int[this.mapHeight][this.mapWidth];
        this.passableMap = new boolean[this.mapHeight][this.mapWidth];
        this.attackPoints = new ArrayList<>();
        this.rallyPoint = null;
        this.base = null;
        this.factoryLocationMap = new boolean[this.mapHeight][this.mapWidth];
        this.stalemateTime = new HashMap<>();
        for (int y = 0; y < this.karboniteMap.length; y++) {
            for (int x = 0; x < this.karboniteMap[y].length; x++) {
                MapLocation loc = new MapLocation(planet, x, y);
                this.karboniteMap[y][x] = (int) pm.initialKarboniteAt(loc);
                if (pm.isPassableTerrainAt(loc) == 1) {
                    this.passableMap[y][x] = true;
                } else {
                    this.passableMap[y][x] = false;
                }
            }
        }

        for (int y = 0; y < this.karboniteMap.length; y++) {
            for (int x = 0; x < this.karboniteMap[y].length; x++) {
                if (!this.passableMap[y][x]) {
                    continue;
                }
                this.factoryLocationMap[y][x] = true;
            }
        }

        // Create navigation maps
        // for (int y = 0; y < this.map.length; y++) {
        //     for (int x = 0; x < this.map[y].length; x++) {
        //         // Skip making a navmap if this location is impassable
        //         if (this.map[y][x] == IMPASSABLE) {
        //             continue;
        //         }
        //
        //         MapLocation start = new MapLocation(planet, x, y);
        //         Queue<MapLocation> openSet = new LinkedList<>();
        //         Direction[][] navMap = new Direction[this.map.length][this.map[y].length];
        //         for (Direction d : DIRECTIONS) {
        //             MapLocation adj = start.add(d);
        //
        //             int adjX = adj.getX();
        //             int adjY = adj.getY();
        //             if (adjX < 0 || adjY < 0 || adjX >= this.map[y].length || adjY >= this.map.length || this.map[adjY][adjX] == IMPASSABLE) {
        //                 continue;
        //             }
        //
        //             openSet.add(adj);
        //             navMap[adjY][adjX] = bc.bcDirectionOpposite(d);
        //         }
        //         while (!openSet.isEmpty()) {
        //             MapLocation next = openSet.remove();
        //             for (Direction d : DIRECTIONS) {
        //                 MapLocation adj = next.add(d);
        //                 int adjX = adj.getX();
        //                 int adjY = adj.getY();
        //                 if (isOOB(adjX, adjY) || this.map[adjY][adjX] == IMPASSABLE) {
        //                     continue;
        //                 }
        //                 if (navMap[adjY][adjX] == null) {
        //                     openSet.add(adj);
        //                     navMap[adjY][adjX] = bc.bcDirectionOpposite(d);
        //                 }
        //             }
        //         }
        //         this.navMaps.put(new Point(x, y), navMap);
        //     }
        // }

        // Set up unit lookup map
        this.allUnits = new HashMap<>();

        // Set up units map
        this.myUnits = new HashMap<>();
        this.oppUnits = new HashMap<>();
        for (UnitType type : UnitType.values()) {
            this.myUnits.put(type, new HashSet<>());
            this.oppUnits.put(type, new HashSet<>());
        }

        this.navigator = new Navigator(gc, this.passableMap);

        System.out.println("Ending initialization with " + gc.getTimeLeftMs() + "ms remaining.");
    }

    /**
     * TODO
     */
    protected void nextRallyPoint() {
        if (this.base == null) {
            return;
        }

        if (this.attackPoints.isEmpty()) {
            for (int u : this.allUnits.keySet()) {
                Unit unit = this.gc.unit(u);
                if (unit.team() == this.ENEMY_TEAM) {
                    Location loc = unit.location();
                    if (loc.isOnMap()) {
                        MapLocation mapLoc = loc.mapLocation();
                        this.attackPoints.add(mapLoc);
                        this.rallyPoint = mapLoc;
                        break;
                    }
                }
            }
        } else {
            long closestDistance = Integer.MAX_VALUE;

            for (MapLocation candidate : this.attackPoints) {
                long dist = this.base.distanceSquaredTo(candidate);
                if (dist < closestDistance) {
                    closestDistance = dist;
                    this.rallyPoint = candidate;
                }
            }
        }
    }

    /**
     * Processes any actions that must happen before a turn.
     */
    public void processPreTurn() {
        // Update unit maps
        for (UnitType type : this.myUnits.keySet()) {
            this.myUnits.get(type).clear();
            this.oppUnits.get(type).clear();
        }
        this.allUnits.clear();
        this.impassablePoints.clear();

        VecUnit units = this.gc.units();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            Location unitLoc = unit.location();
            if (unitLoc.isOnMap()) {
                MapLocation unitMapLoc = unitLoc.mapLocation();
                this.impassablePoints.add(new Point(unitMapLoc.getX(), unitMapLoc.getY()));

                if (unit.team() == this.MY_TEAM) {
                    for (int j = 0; j < this.attackPoints.size(); j++) {
                        MapLocation m = this.attackPoints.get(j);
                        if (m.getX() == unitMapLoc.getX() && m.getY() == unitMapLoc.getY()) {
                            this.attackPoints.remove(j);
                            nextRallyPoint();
                            break;
                        }
                    }
                }
            }
            if (unit.team() == this.MY_TEAM) {
                this.myUnits.get(unit.unitType()).add(unit.id());
            } else {
                this.oppUnits.get(unit.unitType()).add(unit.id());
            }
            this.allUnits.put(unit.id(), unit);
        }

        if (this.rallyPoint == null) {
            nextRallyPoint();
        }
    }

    /**
     * Returns whether a unit of the given ID exists.
     *
     * @param unitID The ID of the unit to check.
     * @return True if a unit of the given ID exists, false otherwise.
     */

    public boolean unitExists(int unitID) {
        return this.allUnits.containsKey(unitID);
    }

    /**
     * Processes a single turn.
     */
    public abstract void processTurn();

    /**
     * Processes any actions that must happen at the end of a turn.
     */
    public void processPostTurn() {
        System.out.println("Ending round " + this.gc.round() + " with " + this.gc.getTimeLeftMs() + "ms remaining.");
    }

    /**
     * Returns whether a given location is out of bounds.
     *
     * @param x The x-coordinate of the location to check.
     * @param y The y-coordinate of the location to check.
     * @return True if the location is out of bounds, false otherwise.
     */
    protected boolean isOOB(int x, int y) {
        return x < 0 || y < 0 || x >= this.mapWidth || y >= this.mapHeight;
    }

    /**
     * Attempts to move a unit to a given target.
     *
     * @param unitID The unit to move.
     * @param target The target to move the given unit to.
     */
    protected void move(int unitID, MapLocation target) {
        Unit unit = this.allUnits.get(unitID);
        if (!unit.location().isOnMap()) {
            return;
        }
        Direction toMove = this.navigator.navigate(unitID, unit.location().mapLocation(), target);
        // MapLocation loc = unit.location().mapLocation();
        // Direction toMove = this.navigator.pathfind(new Point(loc.getX(), loc.getY()), new Point(target.getX(), target.getY()), this.passableMap, this.impassablePoints);
        this.navigator.tryMove(unitID, toMove);
    }


    /**
     * Attempts to move a unit to a given target using a potentially more
     * expensive navigator that accounts for unit locations.
     *
     * @param unitID The unit to move.
     * @param target The target to move the given unit to.
     */
    protected void goodMove(int unitID, MapLocation target) {
        Unit unit = this.allUnits.get(unitID);
        if (!unit.location().isOnMap()) {
            return;
        }
        if (!this.stalemateTime.containsKey(unitID)) {
            this.stalemateTime.put(unitID, 0);
        }
        MapLocation unitLoc = unit.location().mapLocation();

        Direction toMove = this.navigator.navigate(unitID, unit.location().mapLocation(), target);
        if (!this.gc.canMove(unitID, toMove)) {
            this.stalemateTime.put(unit.id(), this.stalemateTime.get(unit.id()) + 1);
        } else {
            this.stalemateTime.put(unitID, 0);
        }
        if (this.stalemateTime.get(unitID) > 5 && this.gc.getTimeLeftMs() > 10000) { // TODO
            toMove = this.navigator.pathfind(new Point(unitLoc.getX(), unitLoc.getY()), new Point(target.getX(), target.getY()), this.passableMap, this.impassablePoints);
        }

        this.navigator.tryMove(unitID, toMove);
    }
}
