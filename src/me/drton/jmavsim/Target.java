package me.drton.jmavsim;

import com.sun.j3d.utils.geometry.Sphere;
import me.drton.jmavlib.geo.GlobalPositionProjector;
import me.drton.jmavlib.geo.LatLonAlt;

import javax.vecmath.Vector3d;
import java.io.FileNotFoundException;

/**
 * User: ton Date: 01.02.14 Time: 22:12
 */
public class Target extends KinematicObject {
    private GlobalPositionProjector gpsProjector = new GlobalPositionProjector();
    private boolean isMoving = false;
    private double xForce = 0;
    private double yForce = 0;

    public Target(World world, double size) throws FileNotFoundException {
        super(world);
        Sphere sphere = new Sphere((float) size);
        transformGroup.addChild(sphere);
        gpsProjector.init(world.getGlobalReference());
    }

    public Target(World world, String modelFile) throws FileNotFoundException {
        super(world);
        modelFromFile(modelFile);
        gpsProjector.init(world.getGlobalReference());
    }

    public GlobalPositionVelocity getGlobalPosition() {
        Vector3d pos = getPosition();
        LatLonAlt latLonAlt = gpsProjector.reproject(new double[]{pos.x, pos.y, pos.z});
        GlobalPositionVelocity gps = new GlobalPositionVelocity();
        gps.position = latLonAlt;
        gps.eph = 1.0;
        gps.epv = 1.0;
        gps.velocity = getVelocity();
        return gps;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean isMoving) {
        this.isMoving = isMoving;
    }

    public double getXForce() {
        return xForce;
    }

    public void setXForce(double xForce) {
        this.xForce = xForce;
    }

    public double getYForce() {
        return yForce;
    }

    public void setYForce(double yForce) {
        this.yForce = yForce;
    }
}
