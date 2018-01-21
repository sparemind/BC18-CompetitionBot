import bc.*;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class Navigator {
    static final Direction[] DIRECTIONS = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest};
    static final Map<Direction, Integer> DIR_VAL = new HashMap<>();
    private static final double SQRT2 = Math.sqrt(2.0);
    private static final double A_STAR_WEIGHT = 1.0;
    private static final int LOOKAHEAD_DISTANCE = 3;
    private static final Direction[] D_DIRS = {Direction.Northeast, Direction.Southeast, Direction.Southwest, Direction.Northwest, Direction.North, Direction.East, Direction.South, Direction.West};
    private static final Map<Direction, Direction> DIR_HORZ_MIRROR = new HashMap<>();
    private static final Map<Direction, Direction> DIR_VERT_MIRROR = new HashMap<>();
    private static final int[] DIR_ROT_ORDER = {-1, 1, -2, 2};

    static {
        DIR_HORZ_MIRROR.put(Direction.North, Direction.North);
        DIR_HORZ_MIRROR.put(Direction.Northeast, Direction.Northwest);
        DIR_HORZ_MIRROR.put(Direction.East, Direction.West);
        DIR_HORZ_MIRROR.put(Direction.Southeast, Direction.Southwest);
        DIR_HORZ_MIRROR.put(Direction.South, Direction.South);
        DIR_HORZ_MIRROR.put(Direction.Southwest, Direction.Southeast);
        DIR_HORZ_MIRROR.put(Direction.West, Direction.East);
        DIR_HORZ_MIRROR.put(Direction.Northwest, Direction.Northeast);

        DIR_VERT_MIRROR.put(Direction.North, Direction.South);
        DIR_VERT_MIRROR.put(Direction.Northeast, Direction.Southeast);
        DIR_VERT_MIRROR.put(Direction.East, Direction.East);
        DIR_VERT_MIRROR.put(Direction.Southeast, Direction.Northeast);
        DIR_VERT_MIRROR.put(Direction.South, Direction.North);
        DIR_VERT_MIRROR.put(Direction.Southwest, Direction.Northwest);
        DIR_VERT_MIRROR.put(Direction.West, Direction.West);
        DIR_VERT_MIRROR.put(Direction.Northwest, Direction.Southwest);

        DIR_VAL.put(Direction.North, 0);
        DIR_VAL.put(Direction.Northeast, 1);
        DIR_VAL.put(Direction.East, 2);
        DIR_VAL.put(Direction.Southeast, 3);
        DIR_VAL.put(Direction.South, 4);
        DIR_VAL.put(Direction.Southwest, 5);
        DIR_VAL.put(Direction.West, 6);
        DIR_VAL.put(Direction.Northwest, 7);
    }

    private GameController gc;
    private Map<Point, Direction[][]> navMaps;
    private Map<Point, int[][]> navMapDists;
    private boolean[][] passable;
    private int mapWidth;
    private int mapHeight;
    // private Symmetry symmetry;
    private boolean isVerticallySymmetric;
    private boolean isHorizontallySymmetric;
    private boolean isRotatedSymmetric;

    public Navigator(GameController gc, boolean[][] passable) {
        this.gc = gc;
        this.navMaps = new HashMap<>();
        this.navMapDists = new HashMap<>();
        this.passable = passable;
        this.mapHeight = this.passable.length;
        this.mapWidth = this.passable[0].length;

        findSymmetry();
    }

    private void findSymmetry() {
        this.isVerticallySymmetric = true;
        this.isHorizontallySymmetric = true;
        this.isRotatedSymmetric = true;
        for (int y = 0; y < this.mapHeight; y++) {
            for (int x = 0; x < this.mapWidth; x++) {
                int mirrorX = this.mapWidth - 1 - x;
                int mirrorY = this.mapHeight - 1 - y;
                if (this.isVerticallySymmetric && this.passable[y][x] != this.passable[mirrorY][x]) {
                    this.isVerticallySymmetric = false;
                }
                if (this.isHorizontallySymmetric && this.passable[y][x] != this.passable[y][mirrorX]) {
                    this.isHorizontallySymmetric = false;
                }
                if (this.isRotatedSymmetric && this.passable[y][x] != this.passable[mirrorY][mirrorX]) {
                    this.isRotatedSymmetric = false;
                }
            }
        }

        // if (isVerticallySymmetric) {
        //     this.symmetry = Navigator.Symmetry.VERTICAL;
        // } else if (isHorizontallySymmetric) {
        //     this.symmetry = Navigator.Symmetry.HORIZONTAL;
        // } else {
        //     this.symmetry = Navigator.Symmetry.ROTATED;
        // }
        System.out.println("Symmetry: " + this.isVerticallySymmetric + ":" + this.isHorizontallySymmetric + ":" + this.isRotatedSymmetric);
    }

    // public void precomputeNavMaps(Planet planet) {
    //     // findSymmetry();
    //
    //     // Create nav maps
    //     for (int y = 0; y < this.mapHeight / 2; y++) {
    //         for (int x = 0; x < this.mapWidth; x++) {
    //             // Don't create navigation for an impassable location
    //             if (!this.passable[y][x]) {
    //                 continue;
    //             }
    //
    //             MapLocation target = new MapLocation(planet, x, y);
    //             Queue<MapLocation> openSet = new LinkedList<>();
    //             Direction[][] navMap = new Direction[this.mapHeight][this.mapWidth];
    //             Direction[][] symNavMap = new Direction[this.mapHeight][this.mapWidth];
    //
    //             openSet.add(target);
    //             while (!openSet.isEmpty()) {
    //                 MapLocation next = openSet.remove();
    //                 for (Direction d : D_DIRS) {
    //                     MapLocation adj = next.add(d);
    //                     int adjX = adj.getX();
    //                     int adjY = adj.getY();
    //                     if (isOOB(adjX, adjY) || !this.passable[adjY][adjX]) {
    //                         continue;
    //                     }
    //                     if (navMap[adjY][adjX] == null) {
    //                         openSet.add(adj);
    //
    //                         Direction navDir = bc.bcDirectionOpposite(d);
    //                         navMap[adjY][adjX] = navDir;
    //
    //                         switch (this.symmetry) {
    //                         case VERTICAL:
    //     symNavMap[this.mapHeight - 1 - adjY][adjX] = DIR_VERT_MIRROR.get(navDir);
    //     navMapDist[this.mapHeight - 1 - adjY][adjX] = navMapDist[next.getY()][next.getX()] + 1;
    //                             break;
    //                         case HORIZONTAL:
    //     symNavMap[y][this.mapWidth - 1 - adjX] = DIR_HORZ_MIRROR.get(navDir);
    //                             break;
    //                         case ROTATED:
    //     symNavMap[this.mapHeight - 1 - adjY][this.mapWidth - 1 - adjX] = d;
    //                             break;
    // }
    //                     }
    //                 }
    //             }
    //             this.navMaps.put(new Point(x, y), navMap);
    //             switch (this.symmetry) {
    //                 case VERTICAL:
    //                     this.navMaps.put(new Point(x, this.mapHeight - 1 - y), symNavMap);
    //                     break;
    //                 case HORIZONTAL:
    //                     this.navMaps.put(new Point(this.mapWidth - 1 - x, y), symNavMap);
    //                     break;
    //                 case ROTATED:
    //                     this.navMaps.put(new Point(this.mapWidth - 1 - x, this.mapHeight - 1 - y), symNavMap);
    //                     break;
    //             }
    //         }
    //     }
    // }

    private boolean isOOB(int x, int y) {
        return x < 0 || y < 0 || x >= this.mapWidth || y >= this.mapHeight;
    }

    public void doNavigate(Set<Unit> units, MapLocation target) {
        Map<Integer, Direction> directions = navigate(units, target);
        for (int unit : directions.keySet()) {
            if (this.gc.isMoveReady(unit) && this.gc.canMove(unit, directions.get(unit))) {
                this.gc.moveRobot(unit, directions.get(unit));
            }
        }
    }

    public Map<Integer, Direction> navigate(Set<Unit> units, MapLocation target) {
        Point targetPoint = new Point(target.getX(), target.getY());
        ensureNavMap(target, targetPoint);
        Map<Integer, Direction> directions = new HashMap<>();

        Queue<Unit> orderedUnits = new PriorityQueue<>((u1, u2) -> {
            MapLocation u1Loc = u1.location().mapLocation();
            MapLocation u2Loc = u2.location().mapLocation();

            int u1Dist = this.navMapDists.get(targetPoint)[u1Loc.getY()][u1Loc.getX()];
            int u2Dist = this.navMapDists.get(targetPoint)[u2Loc.getY()][u2Loc.getX()];

            return Integer.compare(u2Dist, u1Dist);
        });
        for (Unit unit : units) {
            orderedUnits.add(unit);
        }

        for (Unit unit : orderedUnits) {
            directions.put(unit.id(), navigate(unit.id(), unit.location().mapLocation(), target));
        }

        return directions;
    }

    private void ensureNavMap(MapLocation target, Point targetPoint) {
        if (!this.navMaps.containsKey(targetPoint)) {
            createNavMap(target);
        }
    }

    public boolean tryMove(int unit, Direction direction) {
        if (this.gc.isMoveReady(unit) && this.gc.canMove(unit, direction)) {
            this.gc.moveRobot(unit, direction);
            return true;
        }
        return false;
    }

    public Direction navigate(int unit, MapLocation start, MapLocation target) {
        Point targetPoint = new Point(target.getX(), target.getY());
        ensureNavMap(target, targetPoint);
        Direction nextDir = this.navMaps.get(targetPoint)[start.getY()][start.getX()];
        if (nextDir == null || nextDir == Direction.Center) {
            return Direction.Center;
        }

        // Move in this direction if possible
        if (this.gc.canMove(unit, nextDir)) {
            return nextDir;
        }

        MapLocation lookaheadPoint = start;
        for (int i = 0; i < LOOKAHEAD_DISTANCE; i++) {
            lookaheadPoint = lookaheadPoint.add(this.navMaps.get(targetPoint)[lookaheadPoint.getY()][lookaheadPoint.getX()]);
        }

        // If not possible, try turning slightly left or right and see if a spot
        // is open that is closer to the target.
        Direction bestAdjustedDir = Direction.Center;
        // long bestAdjustedDirDist = Integer.MAX_VALUE; // Use this for back and forth motion instead
        long bestAdjustedDirDist = start.distanceSquaredTo(lookaheadPoint);
        for (int i = 0; i < DIR_ROT_ORDER.length; i++) {
            int index = (DIR_VAL.get(nextDir) + DIR_ROT_ORDER[i] + DIRECTIONS.length) % DIRECTIONS.length;
            Direction adjustedDir = DIRECTIONS[index];
            if (!this.gc.canMove(unit, adjustedDir)) {
                continue;
            }

            MapLocation newPos = start.add(adjustedDir);
            long distanceToTarget = newPos.distanceSquaredTo(lookaheadPoint);
            if (distanceToTarget < bestAdjustedDirDist) {
                bestAdjustedDir = adjustedDir;
                bestAdjustedDirDist = distanceToTarget;
            }
        }

        return bestAdjustedDir;
    }

    /**
     * Creates a navigation map for a target map location and the location's
     * symmetrical pair.
     *
     * @param target The location to create a navigation map for.
     */
    private void createNavMap(MapLocation target) {
        int targetX = target.getX();
        int targetY = target.getY();
        Point targetPoint = new Point(targetX, targetY);

        Queue<MapLocation> openSet = new LinkedList<>();
        Direction[][] navMap = new Direction[this.mapHeight][this.mapWidth];
        Direction[][] vertSymNavMap = new Direction[this.mapHeight][this.mapWidth];
        Direction[][] horzSymNavMap = new Direction[this.mapHeight][this.mapWidth];
        Direction[][] rotSymNavMap = new Direction[this.mapHeight][this.mapWidth];
        // int[][] navMapDist = new int[this.mapHeight][this.mapWidth];
        // int[][] symNavMapDist = new int[this.mapHeight][this.mapWidth];

        Point vertSymPoint = new Point(targetX, this.mapHeight - 1 - targetY);
        Point horzSymPoint = new Point(this.mapWidth - 1 - targetX, targetY);
        Point rotSymPoint = new Point(this.mapWidth - 1 - targetX, this.mapHeight - 1 - targetY);

        // Point symmetricPoint = null;
        // if (this.isVerticallySymmetric) {
        //     symmetricPoint = new Point(targetX, this.mapHeight - 1 - targetY);
        // }
        // if (this.isHorizontallySymmetric) {
        //     symmetricPoint = new Point(this.mapWidth - 1 - targetX, targetY);
        // }
        // if (this.isRotatedSymmetric) {
        //     symmetricPoint = new Point(this.mapWidth - 1 - targetX, this.mapHeight - 1 - targetY);
        // }
        // switch (this.symmetry) {
        //     case VERTICAL:
        //         symmetricPoint = new Point(targetX, this.mapHeight - 1 - targetY);
        //         break;
        //     case HORIZONTAL:
        //         symmetricPoint = new Point(this.mapWidth - 1 - targetX, targetY);
        //         break;
        //     case ROTATED:
        //         symmetricPoint = new Point(this.mapWidth - 1 - targetX, this.mapHeight - 1 - targetY);
        //         break;
        // }

        navMap[targetY][targetX] = Direction.Center;
        // navMapDist[targetY][targetX] = 0;

        vertSymNavMap[vertSymPoint.y][vertSymPoint.x] = Direction.Center;
        horzSymNavMap[horzSymPoint.y][horzSymPoint.x] = Direction.Center;
        rotSymNavMap[rotSymPoint.y][rotSymPoint.x] = Direction.Center;
        // symNavMapDist[symmetricPoint.y][symmetricPoint.x] = 0;

        openSet.add(target);
        while (!openSet.isEmpty()) {
            MapLocation next = openSet.remove();
            for (Direction d : D_DIRS) {
                MapLocation adj = next.add(d);
                int adjX = adj.getX();
                int adjY = adj.getY();
                if (isOOB(adjX, adjY) || !this.passable[adjY][adjX]) {
                    continue;
                }
                if (navMap[adjY][adjX] == null) {
                    openSet.add(adj);

                    Direction navDir = bc.bcDirectionOpposite(d);
                    navMap[adjY][adjX] = navDir;
                    // navMapDist[adjY][adjX] = navMapDist[next.getY()][next.getX()] + 1;

                    if (this.isVerticallySymmetric) {
                        vertSymNavMap[this.mapHeight - 1 - adjY][adjX] = DIR_VERT_MIRROR.get(navDir);
                        // symNavMapDist[this.mapHeight - 1 - adjY][adjX] = navMapDist[adjY][adjX];
                    }
                    if (this.isHorizontallySymmetric) {
                        horzSymNavMap[adjY][this.mapWidth - 1 - adjX] = DIR_HORZ_MIRROR.get(navDir);
                        // symNavMapDist[adjY][this.mapWidth - 1 - adjX] = navMapDist[adjY][adjX];
                    }
                    if (this.isRotatedSymmetric) {
                        rotSymNavMap[this.mapHeight - 1 - adjY][this.mapWidth - 1 - adjX] = d;
                        // symNavMapDist[this.mapHeight - 1 - adjY][this.mapWidth - 1 - adjX] = navMapDist[adjY][adjX];
                    }
                    // switch (this.symmetry) {
                    //     case VERTICAL:
                    //         symNavMap[this.mapHeight - 1 - adjY][adjX] = DIR_VERT_MIRROR.get(navDir);
                    //         symNavMapDist[this.mapHeight - 1 - adjY][adjX] = navMapDist[adjY][adjX];
                    //         break;
                    //     case HORIZONTAL:
                    //         symNavMap[adjY][this.mapWidth - 1 - adjX] = DIR_HORZ_MIRROR.get(navDir);
                    //         symNavMapDist[adjY][this.mapWidth - 1 - adjX] = navMapDist[adjY][adjX];
                    //         break;
                    //     case ROTATED:
                    //         symNavMap[this.mapHeight - 1 - adjY][this.mapWidth - 1 - adjX] = d;
                    //         symNavMapDist[this.mapHeight - 1 - adjY][this.mapWidth - 1 - adjX] = navMapDist[adjY][adjX];
                    //         break;
                    // }
                }
            }
        }
        this.navMaps.put(targetPoint, navMap);
        // this.navMapDists.put(targetPoint, navMapDist);

        if (this.isVerticallySymmetric) {
            this.navMaps.put(vertSymPoint, vertSymNavMap);
        }
        if (this.isHorizontallySymmetric) {
            this.navMaps.put(horzSymPoint, horzSymNavMap);
        }
        if (this.isRotatedSymmetric) {
            this.navMaps.put(rotSymPoint, rotSymNavMap);
        }
        // this.navMapDists.put(symmetricPoint, symNavMapDist);
    }

    public Direction pathfind(Point start, Point target, boolean[][] map) {
        // TODO
        map[target.y][target.x] = true;

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        HashMap<Point, Node> nodes = new HashMap<>();

        // Initialize with first node
        Node startNode = new Node(start);
        startNode.gScore = 0;
        startNode.fScore = calcHeuristic(start, target);
        openSet.add(startNode);
        nodes.put(startNode.point, startNode);

        while (!openSet.isEmpty()) {
            // Get node with lowest fScore
            Node current = openSet.remove();

            if (current.point.equals(target)) {
                return calculateSolutionPath(current);
            }

            closedSet.add(current);

            Node[] neighbors = new Node[DIRECTIONS.length];
            for (int i = 0; i < DIRECTIONS.length; i++) {
                Direction dir = DIRECTIONS[i];
                Point dirVector = PlanetPlayer.DIR_VECTORS.get(dir);
                Node adjNode = new Node(new Point(current.point.x + dirVector.x, current.point.y + dirVector.y));
                adjNode.fromParent = dir;
                neighbors[i] = adjNode;
            }
            // Node up = new Node(new Point(current.point.x, current.point.y - 1));
            // Node down = new Node(new Point(current.point.x, current.point.y + 1));
            // Node right = new Node(new Point(current.point.x + 1, current.point.y));
            // Node left = new Node(new Point(current.point.x - 1, current.point.y));
            // Node[] neighbors = {up, right, down, left};

            for (Node neighbor : neighbors) {
                // Ignore already evaluated nodes and ones that aren't traversable
                if (closedSet.contains(neighbor) || neighbor.point.x < 0 || neighbor.point.y < 0 || neighbor.point.x >= map[0].length || neighbor.point.y >= map.length || !map[neighbor.point.y][neighbor.point.x]) {
                    continue;
                }

                // Discover a new Node
                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                    nodes.put(neighbor.point, neighbor);
                }

                // Get the node reference for this point, if one already exists
                if (nodes.containsKey(neighbor.point)) {
                    neighbor = nodes.get(neighbor.point);
                }
                double tentativeGScore = calcTentativeGScore(current, neighbor);
                if (tentativeGScore < neighbor.gScore) {
                    // This is a better path
                    neighbor.cameFrom = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = neighbor.gScore + calcHeuristic(neighbor.point, target);

                    // Update this node's position in the priority queue
                    // Note: O(n) for remove()
                    if (openSet.contains(neighbor)) {
                        openSet.remove(neighbor);
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds the direction that should be moved in from the start of the path
     * to get to the given target node.
     *
     * @param target The end node of the path.
     * @return The direction to move from the start to get to the target.
     */
    private Direction calculateSolutionPath(Node target) {
        Direction dir = null;
        while (target != null && target.fromParent != null) {
            dir = target.fromParent;
            target = target.cameFrom;
        }
        return dir;
    }

    /**
     * Returns a tentative gScore (real cost to reach this node) for the given
     * neighbor of a given node.
     *
     * @param current  The node currently being evaluated.
     * @param neighbor The neighbor of the current node being evaluated.
     * @return The tentative gScore of the neighboring node.
     */
    private double calcTentativeGScore(Node current, Node neighbor) {
        double dist = bc.bcDirectionIsDiagonal(neighbor.fromParent) ? SQRT2 : 1;
        return current.gScore + dist;
    }

    /**
     * Returns the octile distance between the given points.
     *
     * @param p1 First point of the point pair to calculate the heuristic of.
     * @param p2 Second point of the point pair to calculate the heuristic of.
     * @return The octile distance between the two points.
     */
    private double calcHeuristic(Point p1, Point p2) {
        int dx = Math.abs(p1.x - p2.x);
        int dy = Math.abs(p1.y - p2.y);
        return A_STAR_WEIGHT * ((dx + dy) + (SQRT2 - 2) * Math.min(dx, dy));
    }

    public enum Symmetry {
        VERTICAL, HORIZONTAL, ROTATED
    }

    private static class Node implements Comparable<Node> {
        public final Point point; // Position of this node
        public double gScore; // The real cost to reach this node
        public double fScore; // The total cost to reach this node (including heuristic cost)
        public Node cameFrom; // Node on the path back to the start
        public Direction fromParent; // Direction to get here from parent

        public Node(Point point) {
            this.point = point;
            this.gScore = Double.MAX_VALUE;
            this.fScore = Double.MAX_VALUE;
            this.cameFrom = null;
            this.fromParent = null;
        }

        @Override
        public int hashCode() {
            return this.point.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Node other = (Node) o;
            return this.point.equals(other.point);
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }
}
