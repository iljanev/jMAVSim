package me.drton.jmavsim;

import me.drton.jmavlib.geo.LatLonAlt;
import me.drton.jmavsim.vehicle.AbstractMulticopter;
import me.drton.jmavsim.vehicle.Quadcopter;
import org.mavlink.messages.IMAVLinkMessageID;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
        mainWindow = new MainWindow(uiCommandListener, xChangeListener, yChangeListener);
        mainWindow.show();

        // Create world
        world = new World();
        //world.setGlobalReference(new LatLonAlt(55.753395, 37.625427, 0.0));
        world.setGlobalReference(new LatLonAlt(56.9654307, 24.1298169, 0.0));


        // Create MAVLink connections
        MAVLinkConnection connHIL = new MAVLinkConnection(world);
        world.addObject(connHIL);
        MAVLinkConnection connCommon = new MAVLinkConnection(world);
        // Don't spam ground station with HIL messages
        connCommon.addSkipMessage(IMAVLinkMessageID.MAVLINK_MSG_ID_HIL_CONTROLS);
        connCommon.addSkipMessage(IMAVLinkMessageID.MAVLINK_MSG_ID_HIL_SENSOR);
        connCommon.addSkipMessage(IMAVLinkMessageID.MAVLINK_MSG_ID_HIL_GPS);
        world.addObject(connCommon);

        // Create ports
        // Serial port: connection to autopilot
        SerialMAVLinkPort serialMAVLinkPort = new SerialMAVLinkPort();
        connCommon.addNode(serialMAVLinkPort);
        connHIL.addNode(serialMAVLinkPort);
        // UDP port: connection to ground station
        UDPMavLinkPort udpMavLinkPort = new UDPMavLinkPort();
        connCommon.addNode(udpMavLinkPort);

        // Create environment
        SimpleEnvironment simpleEnvironment = new SimpleEnvironment(world);
        Vector3d magField = new Vector3d(0.2f, 0.0f, 0.5f);
        Matrix3d magDecl = new Matrix3d();
        magDecl.rotZ(11.0 / 180.0 * Math.PI);
        magDecl.transform(magField);
        simpleEnvironment.setMagField(magField);
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
        vehicle.setSensors(sensors);
        vehicle.setDragMove(0.02);
        //v.setDragRotate(0.1);

        // Create MAVLink HIL system
        // SysId should be the same as autopilot, ComponentId should be different!
        connHIL.addNode(new MAVLinkHILSystem(1, 51, vehicle));
        world.addObject(vehicle);

        // Create target
//        target = new SimpleTarget(world, "models/biped.obj");
//
//        long t = System.currentTimeMillis();
//        ((SimpleTarget)target).setTrajectory(new Vector3d(5.0, 0.0, 0), new Vector3d(5.0, 100.0, 0), t + 20000, t + 50000);
//        connCommon.addNode(new MAVLinkTargetSystem(2, 1, target));
//        world.addObject(target);


//        target = new MovableTarget(world, "models/bmw.obj");
//        target.position = new Vector3d(5.0, 0.0, 0);
//        connCommon.addNode(new MAVLinkTargetSystem(2, 1, target));
//        world.addObject(target);

        target = new LogPlayerTarget(world, "models/biped.obj");

        try {
            ((LogPlayerTarget)target).openLog("logs/03.bin");
        } catch (Exception ex){
            System.err.println(ex.getMessage());
        }
        long t = System.currentTimeMillis();
        ((LogPlayerTarget)target).setTimeStart(t + 60000);
        connCommon.addNode(new MAVLinkTargetSystem(2, 1, target));
        world.addObject(target);

        // Create visualizer
        visualizer = new Visualizer(world, mainWindow.canvas3D);
        // Put camera on vehicle (FPV)
        /*
        visualizer.setViewerPositionObject(vehicle);   // Without gimbal
         */
        // Put camera on vehicle with gimbal
        // Create camera gimbal
        CameraGimbal2D gimbal = new CameraGimbal2D(world);
        gimbal.setBaseObject(vehicle);
        gimbal.setPitchChannel(4);
        gimbal.setPitchScale(1.57); // +/- 90deg
        world.addObject(gimbal);
        visualizer.setViewerPositionObject(gimbal);      // With gimbal
        visualizer.setViewerTargetObject(target);
        // Put camera on static point and point to vehicle
        /*
        visualizer.setViewerPosition(new Vector3d(-5.0, 0.0, -1.7));
        visualizer.setViewerTargetObject(vehicle);
        visualizer.setAutoRotate(true);
        */
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

    private float moveForce = 0.05f;
    //target.setMoving(true);
    public ActionListener uiCommandListener = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            String cmd = actionEvent.getActionCommand();
            if (target instanceof MovableTarget){
                MovableTarget movableTarget = (MovableTarget) target;
                if (cmd.equals("start")) {
                    target.setMoving(true);
                } else if (cmd.equals("stop")) {
                    movableTarget.setX(0);
                    movableTarget.setY(0);
                    target.setMoving(false);
                } else if (cmd.equals("X")) {
                    movableTarget.setX(movableTarget.getX() + moveForce);
                } else if (cmd.equals("-X")) {
                    movableTarget.setX(movableTarget.getX() - moveForce);
                } else if (cmd.equals("Y")) {
                    movableTarget.setY(movableTarget.getY() + moveForce);
                } else if (cmd.equals("-Y")) {
                    movableTarget.setY(movableTarget.getY() - moveForce);
                }
            }
        }
    };

    public ChangeListener xChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            if (target instanceof MovableTarget) {
                MovableTarget movableTarget = (MovableTarget) target;
                if (changeEvent != null && changeEvent.getSource() instanceof JSlider) {
                    JSlider xSlider = (JSlider) changeEvent.getSource();
                    ;
                    movableTarget.setX(xSlider.getValue() / 100.0f);
                }
            }
        }
    };
    public ChangeListener yChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            if (target instanceof MovableTarget) {
                MovableTarget movableTarget = (MovableTarget) target;
                if (changeEvent != null && changeEvent.getSource() instanceof JSlider) {
                    JSlider xSlider = (JSlider) changeEvent.getSource();
                    ;
                    movableTarget.setY(xSlider.getValue() / 100.0f);
                }
            }
        }
    };

    private void changeSliderValue(){

    }

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
