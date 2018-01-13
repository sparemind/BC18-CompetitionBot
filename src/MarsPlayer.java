import bc.GameController;
import bc.Planet;

public class MarsPlayer extends PlanetPlayer {
    public MarsPlayer(GameController gc, Planet planet) {
        super(gc, planet);
    }

    @Override
    public void processTurn() {
        System.out.println("TODO: MarsPlayer not yet implemented.");
    }
}
