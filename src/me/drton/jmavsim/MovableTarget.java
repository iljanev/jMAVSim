package me.drton.jmavsim;

import javax.vecmath.Vector3d;
import java.io.FileNotFoundException;

/**
 * User: ton Date: 04.05.14 Time: 23:25
 */
public class MovableTarget extends Target {


    Vector3d movementVector = new Vector3d(0, 0, 0);

    public MovableTarget(World world, double size) throws FileNotFoundException {
        super(world, size);
    }

    public MovableTarget(World world, String modelFile) throws FileNotFoundException {
        super(world, modelFile);
    }

    public void stop(){
        setMoving(false);
    }

    public void start(){
        setMoving(true);
    }

    public void setX(double x){
        movementVector.setX(x);
    }
    public double getX(){
        return movementVector.getX();
    }

    public void setY(double y){
        movementVector.setY(y);
    }
    public double getY(){
        return movementVector.getY();
    }


    @Override
    public void update(long t) {
        if (isMoving()) {
            position.scaleAdd(1, movementVector, position);
            //velocity.set(10.0, 10.0, 0.0);
        }

        super.update(t);
    }
}
