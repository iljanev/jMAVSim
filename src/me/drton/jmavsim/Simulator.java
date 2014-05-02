package me.drton.jmavsim;

import me.drton.jmavsim.vehicle.AbstractMulticopter;
import me.drton.jmavsim.vehicle.Quadcopter;
import org.mavlink.messages.IMAVLinkMessageID;

import javax.swing.*;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * User: ton Date: 26.11.13 Time: 12:33
 */
public class Simulator {
    private World world;
    private Visualizer visualizer;
    private int sleepInterval = 10;
    private int visualizerSleepInterval = 20;
    private long nextRun = 0;
    Target target;
    MainWindow mainWindow;

    public Simulator() throws IOException, InterruptedException {
        //Create main window
        mainWindow = new MainWindow(uiCommandListener);
        mainWindow.show();

        // Create world
        world = new World();
        // Create MAVLink connections
        MAVLinkConnection connHIL = new MAVLinkConnection(world);
        world.addObject(connHIL);
        MAVLinkConnection connCommon = new MAVLinkConnection(world);
        connCommon.addSkipMessage(IMAVLinkMessageID.MAVLINK_MSG_ID_HIL_CONTROLS);
        connCommon.addSkipMessage(IMAVLinkMessageID.MAVLINK_MSG_ID_HIL_SENSOR);
        connCommon.addSkipMessage(IMAVLinkMessageID.MAVLINK_MSG_ID_HIL_GPS);
        world.addObject(connCommon);
        // Create and ports
        SerialMAVLinkPort serialMAVLinkPort = new SerialMAVLinkPort();
        connCommon.addNode(serialMAVLinkPort);
        connHIL.addNode(serialMAVLinkPort);
        UDPMavLinkPort udpMavLinkPort = new UDPMavLinkPort();
        connCommon.addNode(udpMavLinkPort);
        // Create environment
        SimpleEnvironment simpleEnvironment = new SimpleEnvironment(world);
        simpleEnvironment.setMagField(new Vector3d(0.2f, 0.0f, 0.5f));
        //simpleEnvironment.setWind(new Vector3d(0.0, 5.0, 0.0));
        simpleEnvironment.setGroundLevel(0.0f);
        world.addObject(simpleEnvironment);
        // Create vehicle with sensors
        Vector3d gc = new Vector3d(0.0, 0.0, 0.0);  // gravity center
        AbstractMulticopter vehicle = new Quadcopter(world, "models/3dr_arducopter_quad_x.obj", "x", 0.33 / 2, 4.0,
                0.05, 0.005, gc);
        vehicle.setMass(0.8);
        Matrix3d I = new Matrix3d();
        // Moments of inertia
        I.m00 = 0.005;  // X
        I.m11 = 0.005;  // Y
        I.m22 = 0.009;  // Z
        vehicle.setMomentOfInertia(I);
        SimpleSensors sensors = new SimpleSensors();
        sensors.initGPS(55.753395, 37.625427);
        vehicle.setSensors(sensors);
        vehicle.setDragMove(0.02);
        //v.setDragRotate(0.1);
        CameraGimbal2D gimbal = new CameraGimbal2D(world);
        gimbal.setBaseObject(vehicle);
        gimbal.setPitchChannel(4);
        gimbal.setPitchScale(1.9);
        world.addObject(gimbal);
        // SysId should be the same as autopilot, ComponentId should be different!
        connHIL.addNode(new MAVLinkHILSystem(1, 51, vehicle));
        world.addObject(vehicle);
        target = new Target(world, 0.3);
        target.setMass(90.0);
        target.initGPS(55.753395, 37.625427);
        target.getPosition().set(5, 0, -5);
        connCommon.addNode(new MAVLinkTargetSystem(2, 1, target));
        world.addObject(target);
        // Create visualizer
        visualizer = new Visualizer(world, mainWindow.canvas3D);
        visualizer.setViewerTarget(target);
        visualizer.setViewerPosition(gimbal);
        //visualizer.setViewerPosition(vehicle);
        visualizer.setAutoRotate(false);
        //visualizer.setAutoRotate(true);
        // Open ports
        serialMAVLinkPort.open("COM5", 230400, 8, 1, 0);
        serialMAVLinkPort.sendRaw("\nsh /etc/init.d/rc.usb\n".getBytes());
        udpMavLinkPort.open(new InetSocketAddress(14555));

        // Run
        try {
            run();
        } catch (InterruptedException e) {
            System.out.println("Exit");
        }
        // Close ports
        serialMAVLinkPort.close();
        udpMavLinkPort.close();
    }

    private double moveForce = 40;
    //target.setMoving(true);
    public ActionListener uiCommandListener = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            String cmd = actionEvent.getActionCommand();
            if (cmd.equals("start")) {
                target.setMoving(true);
            } else if (cmd.equals("stop")) {
                target.setMoving(false);
                target.setXForce(0);
                target.setYForce(0);
            } else if (cmd.equals("X")) {
                target.setXForce(moveForce);
            } else if (cmd.equals("-X")) {
                target.setXForce(-moveForce);
            } else if (cmd.equals("Y")) {
                target.setYForce(moveForce);
            } else if (cmd.equals("-Y")) {
                target.setYForce(-moveForce);
            }
        }
    };

    public void run() throws IOException, InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    visualizer.update();
                    try {
                        Thread.sleep(visualizerSleepInterval);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
        nextRun = System.currentTimeMillis() + sleepInterval;
        while (true) {
            long t = System.currentTimeMillis();
            world.update(t);
            long timeLeft = Math.max(sleepInterval / 4, nextRun - System.currentTimeMillis());
            nextRun = Math.max(t + sleepInterval / 4, nextRun + sleepInterval);
            Thread.sleep(timeLeft);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        new Simulator();
    }
}
