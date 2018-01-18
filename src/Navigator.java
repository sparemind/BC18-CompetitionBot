import bc.*;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class Navigator {
    private static final double SQRT2 = Math.sqrt(2.0);

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

            Node[] neighbors = new Node[PlanetPlayer.DIRECTIONS.length];
            for (int i = 0; i < PlanetPlayer.DIRECTIONS.length; i++) {
                Direction dir = PlanetPlayer.DIRECTIONS[i];
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
        return (dx + dy) + (SQRT2 - 2) * Math.min(dx, dy);
    }
}
