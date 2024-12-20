/* (C) Robolancers 2024 */
package org.robolancers321;

import static org.robolancers321.util.MathUtils.epsilonEquals;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.simulation.AddressableLEDSim;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import org.robolancers321.Constants.FlywheelConstants;
import org.robolancers321.Constants.PivotConstants;
import org.robolancers321.Constants.RetractorConstants;
import org.robolancers321.Constants.RetractorConstants.RetractorSetpoint;
import org.robolancers321.commands.AutoPickupNote;
import org.robolancers321.commands.EmergencyCancel;
import org.robolancers321.commands.FeederShot;
import org.robolancers321.commands.IntakeNote;
import org.robolancers321.commands.IntakeNoteManual;
import org.robolancers321.commands.Mate;
import org.robolancers321.commands.OuttakeNote;
import org.robolancers321.commands.PPAutos.BotDisrupt;
import org.robolancers321.commands.PPAutos.BotDisruptWithPickup;
import org.robolancers321.commands.PPAutos.BotTaxi;
import org.robolancers321.commands.PPAutos.FourBottom;
import org.robolancers321.commands.PPAutos.FourMid;
import org.robolancers321.commands.PPAutos.FourTop;
import org.robolancers321.commands.PPAutos.FourTopAlt;
import org.robolancers321.commands.PPAutos.ScoreAndSit;
import org.robolancers321.commands.PPAutos.ThreeBotCenter;
import org.robolancers321.commands.PPAutos.ThreeBotCenterAlt;
import org.robolancers321.commands.PPAutos.ThreeTopCenter;
import org.robolancers321.commands.PPAutos.TopDisrupt;
import org.robolancers321.commands.PPAutos.TopTaxi;
import org.robolancers321.commands.ScoreAmp;
import org.robolancers321.commands.ScoreAmpIntake;
import org.robolancers321.commands.ScoreSpeakerFixedTeleop;
import org.robolancers321.commands.ScoreSpeakerFromDistance;
import org.robolancers321.commands.Shift;
import org.robolancers321.subsystems.Climber;
import org.robolancers321.subsystems.LED.LED;
import org.robolancers321.subsystems.LED.LED.Section;
import org.robolancers321.subsystems.drivetrain.Drivetrain;
import org.robolancers321.subsystems.intake.Retractor;
import org.robolancers321.subsystems.intake.Sucker;
import org.robolancers321.subsystems.launcher.AimTable;
import org.robolancers321.subsystems.launcher.Flywheel;
import org.robolancers321.subsystems.launcher.Indexer;
import org.robolancers321.subsystems.launcher.Pivot;

public class RobotContainer {
  private Drivetrain drivetrain;
  private Retractor retractor;
  private Sucker sucker;
  private Pivot pivot;
  private Indexer indexer;
  private Flywheel flywheel;
  private Climber climber;
  private XboxController driverController;
  private XboxController manipulatorController;
  private SendableChooser<Command> autoChooser;
  private LED led;
  private AddressableLEDSim ledSim;
  private boolean climbing;

  public RobotContainer() {
    this.drivetrain = Drivetrain.getInstance();
    this.retractor = Retractor.getInstance();
    this.sucker = Sucker.getInstance();
    this.pivot = Pivot.getInstance();
    this.indexer = Indexer.getInstance();
    this.flywheel = Flywheel.getInstance();
    this.climber = Climber.getInstance();

    this.driverController = new XboxController(0);
    this.manipulatorController = new XboxController(1);

    this.configureNamedCommands();

    this.autoChooser = new SendableChooser<Command>();

    this.led = new LED();
    this.ledSim = new AddressableLEDSim(led.ledStrip);

    this.configureEvents();
    this.configureLEDs();
    this.configureDefaultCommands();
    this.configureDriverController_old(); // TODO: test new controls
    this.configureManipulatorController();
    this.configureAuto();
  }

  private void configureEvents() {
    RobotModeTriggers.autonomous().onTrue(disableClimbingMode());
    RobotModeTriggers.teleop().onTrue(disableClimbingMode());

    CommandScheduler.getInstance()
        .onCommandInitialize(
            command -> SmartDashboard.putString("Curr Command", command.getName()));
  }

  private void configureLEDs() {
    // default, meteor red (not climbing)
    LED.registerSignal(0, () -> !climbing, LED.meteorRain(0.02, LED.kDrivingMeteor));

    // meteor blue (climbing)
    // TODO: more technically correct solution is probably propagating robot mode into signal sets?
    LED.registerSignal(15, () -> climbing, LED.meteorRain(0.02, LED.kClimbingMeteor));

    // intakeDown, solid orange
    LED.registerSignal(
        2,
        () -> epsilonEquals(this.retractor.getPositionDeg(), RetractorSetpoint.kIntake.angle, 30),
        LED.solid(Section.FULL, new Color(255, 80, 0)));

    // note in sucker, solid white
    LED.registerSignal(
        3, this.sucker::noteDetected, LED.solid(Section.FULL, new Color(255, 255, 255)));

    // flywheel is revving, solid yellow
    LED.registerSignal(
        4,
        () ->
            (!this.flywheel.isRevved()
                && this.flywheel.getGoalRPM()
                    > FlywheelConstants.FlywheelSetpoint.kAcceptHandoff.rpm),
        LED.solid(Section.FULL, new Color(255, 160, 0)));

    // flywheel is revved, solid green
    LED.registerSignal(
        5,
        () ->
            (this.flywheel.isRevved()
                && this.flywheel.getGoalRPM()
                    >= FlywheelConstants.FlywheelSetpoint.kAcceptHandoff.rpm),
        LED.solid(Section.FULL, new Color(0, 255, 0)));

    LED.registerSignal(
        6,
        () -> this.retractor.getGoal() == RetractorConstants.RetractorSetpoint.kAmp.angle,
        LED.solid(Section.FULL, new Color(20, 0, 50)));
  }

  private void configureDefaultCommands() {
    this.drivetrain.setDefaultCommand( // this.drivetrain.tuneModules());
        this.drivetrain.teleopDrive(driverController, true));
    // this.drivetrain.tuneModulesCommand());

    this.sucker.setDefaultCommand(this.sucker.off());
    this.indexer.setDefaultCommand(this.indexer.off());

    this.flywheel.setDefaultCommand(
        this.flywheel.revSpeakerFromRPM(
            () -> {
              if (this.indexer.entranceBeamNotBroken()) return 0.0;

              if (this.drivetrain.getDistanceToSpeaker() < 4.0)
                return Math.min(
                    0.85 * AimTable.interpolateFlywheelRPM(this.drivetrain.getDistanceToSpeaker()),
                    2000);

              return 0.0;
            }));

    this.retractor.setDefaultCommand(
        // this.retractor.tuneControllers()
        this.retractor.moveToSpeaker());

    this.pivot.setDefaultCommand(
        this.pivot.aimAtSpeaker(
            () -> {
              if (this.indexer.entranceBeamNotBroken())
                return PivotConstants.PivotSetpoint.kRetracted.angle;

              if (this.drivetrain.getDistanceToSpeaker() < 4.0)
                return AimTable.interpolatePivotAngle(this.drivetrain.getDistanceToSpeaker());

              return PivotConstants.PivotSetpoint.kRetracted.angle;
            }));
  }

  /*
   * Press Bumpers Together: zero gyro
   *
   * Hold Right Bumper: toggle slow mode
   * Hold Left Bumper: intake
   *
   * Hold Right Trigger: deploy intake
   * Release Right Trigger: retract intake
   *
   * Hold Left Trigger: deploy outtake
   * Release Left Trigger: retract intake
   *
   * Press A Button: snap to amp angle
   * Press B Button: auto pickup note
   * Press X Button: emergency cancel
   * Press Y Button: enter/exit climb mode
   *
   */
  private void configureDriverController_old() {
    // TODO: i think we keep this just in case for teleop
    new Trigger(
            () -> this.driverController.getLeftBumper() && this.driverController.getRightBumper())
        .onTrue(this.drivetrain.zeroYawCommand());

    // TODO: technically we can just pull the bumper state off of the controller inside teleop
    // command but maybe this offers more control programmatically
    // new Trigger(this.driverController::getRightBumper)
    //     .whileTrue(new InstantCommand(() -> this.drivetrain.slowMode = true));
    // new Trigger(this.driverController::getRightBumper)
    //     .whileFalse(new InstantCommand(() -> this.drivetrain.slowMode = false));

    new Trigger(this.driverController::getLeftBumper).whileTrue(this.sucker.in());
    new Trigger(this.driverController::getLeftBumper).whileFalse(this.sucker.off());

    new Trigger(() -> this.driverController.getRightTriggerAxis() > 0.8)
        .whileTrue(new IntakeNoteManual().unless(() -> climbing));

    // for auto handoff, putting it on a button instead is safer so fender shot is still viable
    // new Trigger(() -> this.driverController.getRightTriggerAxis() > 0.8)
    //     .onFalse((new Mate().andThen(new Shift())).onlyIf(this.sucker::noteDetected));

    new Trigger(() -> this.driverController.getLeftTriggerAxis() > 0.8)
        .whileTrue(new OuttakeNote().unless(() -> climbing));

    new Trigger(this.driverController::getAButton).whileTrue(drivetrain.sysIdAngleMotorCommand());
    // .whileTrue(this.drivetrain.alignToAmp().unless(() -> climbing));

    new Trigger(this.driverController::getBButton)
        .whileTrue(new AutoPickupNote().unless(() -> climbing));

    new Trigger(this.driverController::getXButton)
        .onTrue(new EmergencyCancel().unless(() -> climbing));

    new Trigger(this.driverController::getYButton).onTrue(toggleClimbingMode());
  }

  private void configureDriverController() {
    new Trigger(
            () -> this.driverController.getLeftBumper() && this.driverController.getRightBumper())
        .onTrue(this.drivetrain.zeroYawCommand());

    new Trigger(() -> this.driverController.getLeftTriggerAxis() > 0.5)
        .whileTrue(new ScoreSpeakerFixedTeleop().unless(() -> climbing))
        .onFalse(
            new SequentialCommandGroup(
                    this.retractor.moveToSpeaker(),
                    new ParallelDeadlineGroup(
                        (new WaitUntilCommand(this.indexer::exitBeamBroken)
                                .andThen(new WaitUntilCommand(this.indexer::exitBeamNotBroken))
                                .andThen(new WaitCommand(0.1)))
                            .withTimeout(1.0),
                        this.indexer.outtake(),
                        this.sucker.out(),
                        Commands.idle(this.pivot, this.flywheel)))
                .unless(() -> climbing || this.manipulatorController.getLeftTriggerAxis() > 0.5));

    new Trigger(this.driverController::getRightBumper)
        .onTrue(new Mate().andThen(new Shift()).unless(() -> climbing));

    new Trigger(() -> this.driverController.getRightTriggerAxis() > 0.5)
        .whileTrue(new IntakeNoteManual().unless(() -> climbing));

    new Trigger(this.driverController::getLeftBumper)
        .whileTrue(new OuttakeNote().unless(() -> climbing));

    new Trigger(this.driverController::getXButton).onTrue(toggleClimbingMode());
  }

  /*
   * Press B Button: handoff
   *
   * Hold A Button: rev for score amp
   * Release A Button: eject for score amp
   *
   * Press Y Button: score speaker from distance
   *
   * Hold X Button: rev for fixed speaker shot
   * Release X Button: eject for fixed speaker shot
   *
   * Left Joystick Up: move shooter to amp
   * Left Joystick Down: move shooter to retracted
   *
   * Left Trigger hold: rev feeder
   * Left Trigger release: shoot feeder
   *
   * CLIMB MODE
   *
   * Right Joystick Up: move both climbers up
   * Right Joystick down: move both climbers down
   * Right Bumper: right climber up
   * Right Trigger: right climber down
   * Left Bumper: left climber up
   * Left Trigger: left climber down
   */

  private void configureManipulatorController() {
    new Trigger(() -> this.manipulatorController.getLeftBumper())
        .and(() -> !climbing)
        .onTrue(new ScoreAmpIntake().unless(() -> climbing));

    new Trigger(
            () ->
                this.manipulatorController.getLeftTriggerAxis() > 0.5
                    && this.manipulatorController.getRightTriggerAxis() > 0.5)
        .whileTrue(this.enableClimbingMode())
        .whileFalse(disableClimbingMode());

    new Trigger(() -> Math.abs(this.manipulatorController.getRightY()) > 0.2)
        .whileTrue(
            climber
                .run(
                    () -> {
                      climber.setRightPower(this.manipulatorController.getRightY());
                    })
                .finallyDo(
                    () -> {
                      climber.setRightPower(0);
                    })
                .onlyIf(() -> climbing));

    new Trigger(() -> Math.abs(this.manipulatorController.getLeftY()) > 0.2)
        .whileTrue(
            climber
                .run(
                    () -> {
                      climber.setLeftPower(this.manipulatorController.getLeftY());
                    })
                .finallyDo(
                    () -> {
                      climber.setLeftPower(0);
                    })
                .onlyIf(() -> climbing));

    new Trigger(this.manipulatorController::getRightBumper)
        .and(() -> !climbing)
        .whileTrue(new ScoreSpeakerFixedTeleop().unless(() -> climbing))
        .onFalse(
            new SequentialCommandGroup(
                    this.retractor.moveToSpeaker(),
                    new ParallelDeadlineGroup(
                        (new WaitUntilCommand(this.indexer::exitBeamBroken)
                                .andThen(new WaitUntilCommand(this.indexer::exitBeamNotBroken))
                                .andThen(new WaitCommand(0.1)))
                            .withTimeout(1.0),
                        this.indexer.outtake(),
                        this.sucker.out(),
                        Commands.idle(this.pivot, this.flywheel)))
                .unless(() -> climbing || this.manipulatorController.getLeftTriggerAxis() > 0.5));
  }

  private void configureManipulatorController_old() {
    new Trigger(this.manipulatorController::getBButton)
        .onTrue((new Mate().andThen(new Shift()).unless(() -> climbing)));

    // .onlyIf(this.sucker::noteDetected)

    new Trigger(this.manipulatorController::getAButton)
        .whileTrue(new ScoreAmp().unless(() -> climbing));
    new Trigger(this.manipulatorController::getAButton)
        .onFalse(
            this.indexer
                .outtake()
                .raceWith(Commands.idle(this.pivot, this.flywheel))
                .unless(() -> climbing));

    new Trigger(this.manipulatorController::getYButton)
        .onTrue(new ScoreSpeakerFromDistance().unless(() -> climbing));

    new Trigger(this.manipulatorController::getXButton)
        .and(() -> !climbing)
        .whileTrue(new ScoreSpeakerFixedTeleop().unless(() -> climbing));
    new Trigger(this.manipulatorController::getXButton)
        .and(() -> !climbing)
        .onFalse(
            new SequentialCommandGroup(
                    this.retractor.moveToSpeaker(),
                    new ParallelDeadlineGroup(
                        (new WaitUntilCommand(this.indexer::exitBeamBroken)
                                .andThen(new WaitUntilCommand(this.indexer::exitBeamNotBroken))
                                .andThen(new WaitCommand(0.1)))
                            .withTimeout(1.0),
                        this.indexer.outtake(),
                        this.sucker.out(),
                        Commands.idle(this.pivot, this.flywheel)))
                .unless(() -> climbing || this.manipulatorController.getLeftTriggerAxis() > 0.5));

    new Trigger(this.manipulatorController::getLeftBumper).onTrue(toggleClimbingMode());

    // new Trigger(() -> this.manipulatorController.getLeftBumper())
    //     .and(() -> !climbing)
    //     .onTrue(new AutoScoreTrap());

    new Trigger(() -> this.manipulatorController.getPOV() == 0)
        .and(() -> !climbing)
        .whileTrue(new FeederShot().unless(() -> climbing));
    new Trigger(() -> this.manipulatorController.getPOV() == 0)
        .and(() -> !climbing)
        .onFalse(
            new SequentialCommandGroup(
                    this.retractor.moveToSpeaker(),
                    new ParallelDeadlineGroup(
                        (new WaitUntilCommand(this.indexer::exitBeamBroken)
                                .andThen(new WaitUntilCommand(this.indexer::exitBeamNotBroken))
                                .andThen(new WaitCommand(0.1)))
                            .withTimeout(1.0),
                        this.indexer.outtake(),
                        this.sucker.out(),
                        Commands.idle(this.pivot, this.flywheel)))
                .unless(() -> climbing || this.manipulatorController.getLeftTriggerAxis() > 0.5));

    new Trigger(() -> this.manipulatorController.getRightTriggerAxis() > 0.5)
        .and(() -> !climbing)
        .onTrue(new ScoreAmpIntake().unless(() -> climbing));

    // new Trigger(() -> this.manipulatorController.getRightTriggerAxis() > 0.5)
    //     .and(() -> !climbing)
    //     .onFalse(
    //         new ParallelDeadlineGroup(
    //                 new WaitCommand(0.4), this.sucker.ampShot(), Commands.idle(this.retractor))
    //             .unless(() -> climbing));

    new Trigger(() -> this.manipulatorController.getLeftY() < -0.8)
        .onTrue(this.pivot.aimAtAmp().unless(() -> climbing));
    new Trigger(() -> this.manipulatorController.getLeftY() > 0.8)
        .onTrue(this.pivot.moveToRetracted().unless(() -> climbing));

    new Trigger(() -> -this.manipulatorController.getRightY() > 0.2)
        .whileTrue(
            climber
                .run(
                    () -> {
                      climber.setLeftPower(-this.manipulatorController.getRightY());
                      climber.setRightPower(-this.manipulatorController.getRightY());
                    })
                .finallyDo(
                    () -> {
                      climber.setLeftPower(0);
                      climber.setRightPower(0);
                    })
                .onlyIf(() -> climbing));

    new Trigger(() -> -this.manipulatorController.getRightY() < -0.2)
        .whileTrue(
            climber
                .run(
                    () -> {
                      climber.setLeftPower(-this.manipulatorController.getRightY());
                      climber.setRightPower(-this.manipulatorController.getRightY());
                    })
                .finallyDo(
                    () -> {
                      climber.setLeftPower(0);
                      climber.setRightPower(0);
                    })
                .onlyIf(() -> climbing));

    // new Trigger(this.manipulatorController::getLeftBumper)
    //     .and(() -> climbing)
    //     .whileTrue(
    //         climber
    //             .run(() -> climber.setLeftPower(0.2))
    //             .finallyDo(() -> climber.setLeftPower(0))
    //             .onlyIf(() -> climbing));

    // new Trigger(this.manipulatorController::getRightBumper)
    //     .whileTrue(
    //         climber
    //             .run(() -> climber.setRightPower(0.2))
    //             .finallyDo(() -> climber.setRightPower(0))
    //             .onlyIf(() -> climbing));

    new Trigger(() -> this.manipulatorController.getLeftTriggerAxis() > 0.5)
        .whileTrue(
            climber
                .run(() -> climber.setLeftPower(-0.2))
                .finallyDo(() -> climber.setLeftPower(0))
                .onlyIf(() -> climbing));

    new Trigger(() -> this.manipulatorController.getRightTriggerAxis() > 0.5)
        .whileTrue(
            climber
                .run(() -> climber.setRightPower(-0.2))
                .finallyDo(() -> climber.setRightPower(0))
                .onlyIf(() -> climbing));

    // new Trigger(this.manipulatorController::getXButton).and(() -> climbing).onTrue(new
    // ScoreTrap());
  }

  private void configureAuto() {
    /*
    Sweep - pickup front 3 notes with curves
    Sweep straight - pickup front 3 notes with straight line
    Close - pickup front note first
    Skip - pickup center note first then go back for front note
     */

    this.autoChooser.addOption(
        "Do Nothing", Commands.none()
        // new InstantCommand(
        // () -> this.drivetrain.zeroYaw(this.drivetrain.getPose().getRotation().getDegrees()))
        );
    this.autoChooser.setDefaultOption("Score And Sit", new ScoreAndSit());
    this.autoChooser.addOption(
        "Tune",
        drivetrain
            .zeroToPath(PathPlannerPath.fromPathFile("Bruh"))
            .andThen(AutoBuilder.followPath(PathPlannerPath.fromPathFile("Bruh"))));

    // this.autoChooser.addOption(
    //     "TESTING DONT USE",
    //     new InstantCommand(
    //             () ->
    // this.drivetrain.setYaw(this.drivetrain.getPose().getRotation().getDegrees()))
    //         .andThen(new PathAndRetract(PathPlannerPath.fromPathFile("Bruh"))));

    // this.autoChooser.addOption("4NT Sweep", new Auto4NTSweep());
    // this.autoChooser.addOption("4NT Close", new Auto4NTClose());
    // this.autoChooser.addOption("3NT Close", new Auto3NTClose());

    // this.autoChooser.addOption("4NM Sweep", new Auto4NMSweep());
    // this.autoChooser.addOption("3NM Close", new Auto3NMClose());
    // this.autoChooser.addOption("4NM Sweep Fender", new Auto4NMSweepFender());
    // this.autoChooser.addOption("4NM Sweep Fender Straight", new Auto4NMSweepFenderStraight());
    // this.autoChooser.addOption(
    //     "4NM Sweep Fender Straight Auto Pickup", new Auto4NMSweepFenderStraightAutoPickup());

    // this.autoChooser.addOption("4NB Sweep", new Auto4NBSweep());
    // this.autoChooser.addOption("4NB Skip", new Auto4NBSkip());
    // this.autoChooser.addOption("3NB Sweep Straight", new Auto4NBSweepStraight());
    // this.autoChooser.addOption("3NB Close", new Auto3NBClose());

    // pathplanner
    this.autoChooser.addOption("4 piece mid", new FourMid());
    this.autoChooser.addOption("AMP score & taxi", new TopTaxi());
    this.autoChooser.addOption("SOURCE Score & taxi", new BotTaxi());

    this.autoChooser.addOption("AMP 3 piece", new FourTop());
    this.autoChooser.addOption("AMP 3 piece 2", new FourTopAlt());
    this.autoChooser.addOption("AMP straight to center", new ThreeTopCenter());

    this.autoChooser.addOption("SOURCE 3 piece", new FourBottom());
    this.autoChooser.addOption("SOURCE straight to center", new ThreeBotCenter());
    this.autoChooser.addOption("SOURCE straight to center 2", new ThreeBotCenterAlt());

    this.autoChooser.addOption("AMP CHAOS >:)", new TopDisrupt());
    this.autoChooser.addOption("SOURCE chaos >:)", new BotDisrupt());
    this.autoChooser.addOption("SOURCE chaos with pickup >:)", new BotDisruptWithPickup());

    // this.autoChooser.addOption("2 piece mid", new Close3M());

    SmartDashboard.putData(autoChooser);
  }

  private void configureNamedCommands() {
    NamedCommands.registerCommand("IntakeNote", new IntakeNote());
  }

  private Command toggleClimbingMode() {
    return Commands.either(disableClimbingMode(), enableClimbingMode(), () -> climbing);
  }

  private Command enableClimbingMode() {
    return Commands.runOnce(
        () -> {
          this.climbing = true;
          this.pivot.setDefaultCommand(this.pivot.moveToRetracted());
          // this.pivot.setDefaultCommand(this.pivot.aimAtTrap());
        });
  }

  private Command disableClimbingMode() {
    return Commands.runOnce(
        () -> {
          this.climbing = false;
          this.pivot.setDefaultCommand(
              this.pivot.aimAtSpeaker(
                  () -> {
                    if (this.indexer.entranceBeamNotBroken())
                      return PivotConstants.PivotSetpoint.kRetracted.angle;

                    if (this.drivetrain.getDistanceToSpeaker() < 4.0)
                      return AimTable.interpolatePivotAngle(this.drivetrain.getDistanceToSpeaker());

                    return PivotConstants.PivotSetpoint.kRetracted.angle;
                  }));
        });
  }

  public Command getAutonomousCommand() {
    // return new Close4T();
    // return AutoBuilder.buildAuto("Tuning");

    return this.autoChooser.getSelected();
  }
}
