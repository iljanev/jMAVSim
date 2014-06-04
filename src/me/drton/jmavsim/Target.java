package me.drton.jmavsim;

import com.sun.j3d.utils.geometry.Sphere;
import me.drton.jmavlib.geo.GlobalPositionProjector;
import me.drton.jmavlib.geo.LatLonAlt;
import sun.security.provider.SystemSigner;

import javax.vecmath.Vector3d;
import java.io.FileNotFoundException;

/**
 * User: ton Date: 01.02.14 Time: 22:12
 */
public class Target extends KinematicObject {
    private long startTime = 0;
    private GlobalPositionProjector gpsProjector = new GlobalPositionProjector();
    private boolean shouldMessEP = true;

    public Target(World world, double size) throws FileNotFoundException {
        super(world);
        Sphere sphere = new Sphere((float) size);
        transformGroup.addChild(sphere);
        gpsProjector.init(world.getGlobalReference());
        startTime = System.currentTimeMillis();
    }

    public GPSPosition getGlobalPosition() {
        Vector3d pos = getPosition();
        LatLonAlt latLonAlt = gpsProjector.reproject(new double[]{pos.x, pos.y, pos.z});
        GPSPosition gps = new GPSPosition();
        gps.position = latLonAlt;
        long delta = System.currentTimeMillis() - startTime;
        if (shouldMessEP) {
            long firstLimit = 200000;
            long secondLimit = firstLimit + 30000;
            long thirdLimit = secondLimit + 30000;
            long fourthLimit = thirdLimit + 2 * 30000;
            long fifthLimit = fourthLimit + 30000;
            if (delta < firstLimit) {
                gps.eph = 1.0;
                gps.epv = 1.0;
            } else if (delta >= firstLimit && delta < secondLimit) {
                gps.eph = 5.0;
                gps.epv = 5.0;
            } else if (delta >= secondLimit && delta < thirdLimit) {
                gps.eph = 10.0;
                gps.epv = 10.0;
            } else if (delta >= thirdLimit && delta < fourthLimit) {
                gps.eph = 20.0;
                gps.epv = 20.0;
            } else if (delta >= fourthLimit && delta < fifthLimit) {
                gps.eph = 10.0;
                gps.epv = 10.0;
            } else {
                gps.eph = 1.0;
                gps.epv = 1.0;
            }
        } else {
            gps.eph = 1.0;
            gps.epv = 1.0;
        }
        gps.velocity = getVelocity();
        return gps;
    }
}
