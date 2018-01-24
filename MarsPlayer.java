import bc.*;

public class MarsPlayer extends PlanetPlayer {
    public MarsPlayer(GameController gc, Planet planet) {
        super(gc, planet);

        for (int x = 0; x < this.mapWidth; x++) {
            for (int y = 0; y < this.mapHeight; y++) {
                if (this.passableMap[y][x]/* && Math.random() < 0.2*/) {
                    gc.writeTeamArray(0, x);
                    gc.writeTeamArray(1, y);
                    return; // TODO
                }
            }
        }
    }

    @Override
    public void processTurn() {
        for (int rocket : this.myUnits.get(UnitType.Rocket)) {
            Location rocketLoc = this.gc.unit(rocket).location();
            if (rocketLoc.isOnPlanet(this.planet)) {
                for (Direction d : DIRECTIONS) {
                    if (this.gc.canUnload(rocket, d)) {
                        this.gc.unload(rocket, d);
                    }
                }
            }
        }

        for (int worker : this.myUnits.get(UnitType.Worker)) {
            Unit unit = this.gc.unit(worker);
            if (!unit.location().isOnMap()) {
                continue;
            }

            Direction toMove = DIRECTIONS[(int) (Math.random() * DIRECTIONS.length)];
            if (this.gc.isMoveReady(worker) && this.gc.canMove(worker, toMove)) {
                this.gc.moveRobot(worker, toMove);
            }

            for (Direction d : DIRECTIONS) {
                if (this.gc.canReplicate(worker, d)) {
                    this.gc.replicate(worker, d);
                }
            }
        }
    }
}
