import bc.Direction;
import bc.GameController;
import bc.Unit;
import bc.VecUnit;

public class EarthPlayer extends PlanetPlayer {
    public EarthPlayer(GameController gc) {
        super(gc);
    }

    @Override
    public void processTurn() {
        // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
        VecUnit units = this.gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);

            // Most methods on gc take unit IDs, instead of the unit objects themselves.
            if (this.gc.isMoveReady(unit.id()) && this.gc.canMove(unit.id(), Direction.Southeast)) {
                this.gc.moveRobot(unit.id(), Direction.Southeast);
            }
        }
    }
}
