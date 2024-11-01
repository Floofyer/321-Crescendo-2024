/* (C) Robolancers 2024 */
package org.robolancers321.commands.ChoreoAutos;

import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import org.robolancers321.commands.AutoCommands.PathAndIntake;
import org.robolancers321.commands.AutoCommands.PathAndMate;
import org.robolancers321.commands.AutoCommands.PathAndShoot;
import org.robolancers321.commands.ScoreSpeakerFixedAuto;
import org.robolancers321.commands.ScoreSpeakerFromDistance;
import org.robolancers321.subsystems.drivetrain.Drivetrain;
import org.robolancers321.subsystems.intake.Retractor;
import org.robolancers321.subsystems.intake.Sucker;
import org.robolancers321.subsystems.launcher.Flywheel;
import org.robolancers321.subsystems.launcher.Indexer;
import org.robolancers321.subsystems.launcher.Pivot;

public class Auto4NBSkip extends SequentialCommandGroup {
  private Drivetrain drivetrain;
  private Retractor retractor;
  private Sucker sucker;
  private Pivot pivot;
  private Indexer indexer;
  private Flywheel flywheel;

  public Auto4NBSkip() {
    this.drivetrain = Drivetrain.getInstance();
    this.retractor = Retractor.getInstance();
    this.sucker = Sucker.getInstance();
    this.pivot = Pivot.getInstance();
    this.indexer = Indexer.getInstance();
    this.flywheel = Flywheel.getInstance();

    this.addCommands(
        // TODO: test this
        Drivetrain.getInstance().zeroToPath(PathPlannerPath.fromChoreoTrajectory("4NB-Skip.1")),
        new ScoreSpeakerFixedAuto(),
        new PathAndIntake(PathPlannerPath.fromChoreoTrajectory("4NB-Skip.1")),
        new PathAndMate(PathPlannerPath.fromChoreoTrajectory("4NB-Skip.2")),
        new ScoreSpeakerFromDistance(),
        new PathAndIntake(PathPlannerPath.fromChoreoTrajectory("4NB-Skip.3")),
        new PathAndMate(PathPlannerPath.fromChoreoTrajectory("4NB-Skip.4")),
        new ScoreSpeakerFromDistance(),
        new PathAndShoot(PathPlannerPath.fromChoreoTrajectory("4NB-Skip.5")));
  }
}
