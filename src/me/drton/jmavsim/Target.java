package me.drton.jmavsim;

import com.sun.j3d.utils.geometry.Sphere;
import me.drton.jmavlib.geo.GlobalPositionProjector;
import me.drton.jmavlib.geo.LatLonAlt;

import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.io.FileNotFoundException;

/**
 * User: ton Date: 01.02.14 Time: 22:12
 */
public class Target extends KinematicObject {
    private GlobalPositionProjector gpsProjector = new GlobalPositionProjector();
    private boolean isMoving = false;

    public Target(World world, double size) throws FileNotFoundException {
        super(world);
        Sphere sphere = new Sphere((float) size);
        Color color = Color.RED;

        Appearance ap = sphere.getAppearance();
        Material mat = ap.getMaterial();
        mat.setDiffuseColor(new Color3f(color));

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

}
