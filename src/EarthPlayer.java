import bc.*;

import java.util.ArrayList;
import java.util.List;

public class EarthPlayer extends PlanetPlayer {
    public EarthPlayer(GameController gc, Planet planet) {
        super(gc, planet);
    }

    @Override
    public void processTurn() {


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
    }
}
