/* (C) Robolancers 2024 */
package org.robolancers321.commands.PPAutos;

import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import java.util.List;
import org.robolancers321.commands.AutoCommands.PathAndIntake;
import org.robolancers321.commands.AutoCommands.PathAndRetract;
import org.robolancers321.commands.ScoreSpeakerFixedAuto;
import org.robolancers321.subsystems.drivetrain.Drivetrain;
import org.robolancers321.subsystems.intake.Retractor;
import org.robolancers321.subsystems.intake.Sucker;
import org.robolancers321.subsystems.launcher.Flywheel;
import org.robolancers321.subsystems.launcher.Indexer;
import org.robolancers321.subsystems.launcher.Pivot;

public class ThreeTopCenter extends SequentialCommandGroup {
  private Drivetrain drivetrain;
  private Retractor retractor;
  private Sucker sucker;
  private Pivot pivot;
  private Indexer indexer;
  private Flywheel flywheel;

  public ThreeTopCenter() {
    this.drivetrain = Drivetrain.getInstance();
    this.retractor = Retractor.getInstance();
    this.sucker = Sucker.getInstance();
    this.pivot = Pivot.getInstance();
    this.indexer = Indexer.getInstance();
    this.flywheel = Flywheel.getInstance();

    List<PathPlannerPath> pathGroup = PathPlannerAuto.getPathGroupFromAutoFile("3TopCenter");

    // Pose2d startingPose = pathGroup.get(0).getPreviewStartingHolonomicPose();

    this.addCommands(

        // new InstantCommand(() -> this.drivetrain.resetPose(startingPose)),

        // AutoBuilder.buildAuto("3TopCenter")
        Drivetrain.getInstance().zeroToPath(pathGroup.get(0)),
        new ScoreSpeakerFixedAuto(),
        new PathAndIntake(pathGroup.get(0)),
        new PathAndRetract(pathGroup.get(1)),
        new ScoreSpeakerFixedAuto(),
        new PathAndIntake(pathGroup.get(2)),
        new PathAndRetract(pathGroup.get(3)),
        new ScoreSpeakerFixedAuto());
  }
}
