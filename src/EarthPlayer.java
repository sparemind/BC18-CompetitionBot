import bc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EarthPlayer extends PlanetPlayer {
    // "Radius" of square
    private static final int DEPOSIT_SCAN_RADIUS = 1;
    // In squared units
    private static final int POD_SCAN_RADIUS = 16;

    private int[][] depositMap;
    // Key: UnitID, Value: Pod of UnitIDs
    private Map<Integer, Set<Integer>> podsMap;
    // All worker pods
    private List<Set<Integer>> pods;
    private Map<Set<Integer>, Order> podOrders;
    private Navigator navigator;

    public EarthPlayer(GameController gc, Planet planet) {
        super(gc, planet);

        initializeNavigator();
        // findKarboniteDeposits();
        // makePods();
        // assignInitialPods();
    }

    private void initializeNavigator() {
        boolean[][] passable = new boolean[this.mapHeight][this.mapWidth];
        for (int y = 0; y < this.mapHeight; y++) {
            for (int x = 0; x < this.mapWidth; x++) {
                passable[y][x] = this.map[y][x] != IMPASSABLE;
            }
        }
        this.navigator = new Navigator(this.gc, passable);

        // this.navigator.precomputeNavMaps(this.gc.planet());
    }

    private void findKarboniteDeposits() {
        this.depositMap = new int[this.mapHeight][this.mapWidth];
        int maxDepositValue = 0; // TODO

        for (int y = 0; y < this.mapHeight; y++) {
            for (int x = 0; x < this.mapWidth; x++) {
                // Don't calculate deposits for impassable locations
                if (this.map[y][x] == IMPASSABLE) {
                    continue;
                }
                int value = getDepositValue(x, y, DEPOSIT_SCAN_RADIUS);
                this.depositMap[y][x] = value;
                maxDepositValue = Math.max(maxDepositValue, value);
            }
        }
    }

    private int getDepositValue(int x, int y, int radius) {
        int value = 0;

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int yPos = y + i;
                int xPos = x + j;
                if (isOOB(xPos, yPos)) {
                    continue;
                }
                // Don't add impassable locations to the total
                if (this.map[yPos][xPos] == IMPASSABLE) {
                    continue;
                }
                value += this.map[yPos][xPos];
            }
        }

        return value;
    }

    private void makePods() {
        this.podsMap = new HashMap<>();
        this.pods = new ArrayList<>();

        Set<Integer> processed = new HashSet<>();

        // Process every unit
        VecUnit myUnits = this.gc.myUnits();
        for (int i = 0; i < myUnits.size(); i++) {
            HashSet<Integer> pod = new HashSet<>();
            addToPod(myUnits.get(i), pod, processed);
            if (!pod.isEmpty()) {
                this.pods.add(pod);
            }
        }

        System.out.println(this.pods.size() + " pods.");
        for (Set<Integer> pod : this.pods) {
            System.out.println(pod);
        }
        System.out.println();
    }

    private void addToPod(Unit unit, Set<Integer> pod, Set<Integer> processed) {
        if (processed.contains(unit.id())) {
            return;
        }

        processed.add(unit.id());
        pod.add(unit.id());
        this.podsMap.put(unit.id(), pod);

        VecUnit nearbyUnits = this.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), POD_SCAN_RADIUS, this.MY_TEAM);
        for (int i = 0; i < nearbyUnits.size(); i++) {
            addToPod(nearbyUnits.get(i), pod, processed);
        }
    }

    private void assignInitialPods() {
        this.podOrders = new IdentityHashMap<>();

        if (this.pods.size() == 1) {
            this.podOrders.put(this.pods.get(0), Order.BUILD);
        } else {
            Set<Integer> bestPod = null;
            int bestPodValue = -1;
            for (Set<Integer> pod : this.pods) {
                this.podOrders.put(pod, Order.BUILD);

                int meanX = 0;
                int meanY = 0;
                for (int unit : pod) {
                    meanX += this.gc.unit(unit).location().mapLocation().getX();
                    meanY += this.gc.unit(unit).location().mapLocation().getY();
                }
                meanX /= pod.size();
                meanY /= pod.size();

                int value = getDepositValue(meanX, meanY, 5); // TODO
                if (value > bestPodValue) {
                    bestPodValue = value;
                    bestPod = pod;
                }
            }
            this.podOrders.put(bestPod, Order.MINE);
        }
    }

    @Override
    public void processTurn() {


        int i = 0;
        MapLocation[] targets = {new MapLocation(Planet.Earth, 18, 4), new MapLocation(Planet.Earth, 18, 15)};
        for (int worker : this.myUnits.get(UnitType.Worker)) {
            Unit workerUnit = this.allUnits.get(worker);
            MapLocation loc = workerUnit.location().mapLocation();

            MapLocation target = targets[i];

            Direction toMove = this.navigator.navigate(worker, loc, target);
            if (this.gc.isMoveReady(worker) && this.gc.canMove(worker, toMove)) {
                this.gc.moveRobot(worker, toMove);
            }

            i++;

            // VecUnit allUnits = this.gc.units();
            // for (int i = 0; i < allUnits.size(); i++) {
            //     Location l = allUnits.get(i).location();
            //     if (l.isOnMap() && !l.isInGarrison()) {
            //         MapLocation ml = l.mapLocation();
            //         navMap[ml.getY()][ml.getX()] = false;
            //     }
            // }
            // // for (boolean[] b : navMap) {
            // //     for (boolean bb : b) {
            // //         System.out.print(bb ? "." : "#");
            // //     }
            // // }
            // // System.out.println();
            //
            // Direction toMove = nav.pathfind(new Point(loc.getX(), loc.getY()), new Point(18, 10), navMap);
            // if (toMove != null && this.gc.isMoveReady(worker) && this.gc.canMove(worker, toMove)) {
            //     this.gc.moveRobot(worker, toMove);
            // }
        }

        // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
        // VecUnit units = this.gc.myUnits();
        // for (int i = 0; i < units.size(); i++) {
        //     Unit unit = units.get(i);
        //
        //     Direction[][] navMap = this.navMaps.get(new Point(10, 10));
        //     MapLocation loc = unit.location().mapLocation();
        //     int unitX = loc.getX();
        //     int unitY = loc.getY();
        //     Direction nextDir = navMap[unitY][unitX];
        //     if (this.gc.isMoveReady(unit.id()) && this.gc.canMove(unit.id(), nextDir)) {
        //         this.gc.moveRobot(unit.id(), nextDir);
        //     }
        //
        // }


        /*
        for (int worker : this.myUnits.get(UnitType.Worker)) {
            // if (this.gc.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory)) {
            Unit workerUnit = this.allUnits.get(worker);
            if (workerUnit.location().isInGarrison()) {
                continue;
            }
            MapLocation loc = workerUnit.location().mapLocation();

            for (int factory : this.myUnits.get(UnitType.Factory)) {
                if (this.gc.canBuild(worker, factory)) {
                    this.gc.build(worker, factory);
                }
            }

            for (Direction d : DIRECTIONS) {
                MapLocation adj = loc.add(d);
                if (!isOOB(adj.getX(), adj.getY()) && this.map[adj.getY()][adj.getX()] == 0 && this.gc.canBlueprint(worker, UnitType.Factory, d)) {
                    this.gc.blueprint(worker, UnitType.Factory, d);
                    break;
                }
                if (!isOOB(adj.getX(), adj.getY()) && this.gc.canHarvest(worker, d)) {
                    this.gc.harvest(worker, d);
                    break;
                }
            }
            // }
        }

        for (int ranger : this.myUnits.get(UnitType.Ranger)) {
            Unit rangerUnit = this.allUnits.get(ranger);
            if (rangerUnit.location().isInGarrison()) {
                continue;
            }
            MapLocation loc = rangerUnit.location().mapLocation();

            VecUnit rawTargets = this.gc.senseNearbyUnitsByType(loc, rangerUnit.attackRange(), UnitType.Factory);
            List<Unit> targets = new ArrayList<>();
            for (int i = 0; i < rawTargets.size(); i++) {
                if (rawTargets.get(i).team() == this.ENEMY_TEAM) {
                    targets.add(rawTargets.get(i));
                }
            }
            if (targets.isEmpty()) {
                rawTargets = this.gc.senseNearbyUnitsByTeam(loc, rangerUnit.attackRange(), this.ENEMY_TEAM);
                for (int i = 0; i < rawTargets.size(); i++) {
                    targets.add(rawTargets.get(i));
                }
            }
            Unit closestTarget = null;
            long closestDistance = Long.MAX_VALUE;
            for (int i = 0; i < targets.size(); i++) {
                Unit target = targets.get(i);
                MapLocation targetLoc = target.location().mapLocation();
                long distance = loc.distanceSquaredTo(targetLoc);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = target;
                }
            }
            if (closestTarget != null) {
                if (this.gc.canAttack(ranger, closestTarget.id()) && rangerUnit.attackHeat() < 10) {
                    this.gc.attack(ranger, closestTarget.id());
                }
            }
        }

        for (int factory : this.myUnits.get(UnitType.Factory)) {
            Unit factoryUnit = this.allUnits.get(factory);
            MapLocation loc = factoryUnit.location().mapLocation();

            boolean freeSurroundings = false;
            for (Direction d : DIRECTIONS) {
                if (this.gc.canUnload(factory, d)) {
                    this.gc.unload(factory, d);
                }
                MapLocation adj = loc.add(d);
                if (isOOB(adj.getX(), adj.getY())) {
                    continue;
                }
                if (this.gc.isOccupiable(adj) == 1) {
                    freeSurroundings = true;
                    break;
                }
            }
            // Don't make stuff if you can't unload it
            if (!freeSurroundings) {
                continue;
            }
            if (Math.random() < 0.5) {
                if (this.gc.canProduceRobot(factory, UnitType.Worker)) {
                    this.gc.produceRobot(factory, UnitType.Worker);
                }
            } else {
                if (this.gc.canProduceRobot(factory, UnitType.Ranger)) {
                    this.gc.produceRobot(factory, UnitType.Ranger);
                }
            }
        }

        */
    }
}
