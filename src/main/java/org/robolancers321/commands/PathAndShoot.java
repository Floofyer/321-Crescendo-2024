/* (C) Robolancers 2024 */
package org.robolancers321.commands;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelRaceGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import org.robolancers321.subsystems.intake.Retractor;
import org.robolancers321.subsystems.intake.Sucker;

public class PathAndShoot extends SequentialCommandGroup {
  private Sucker sucker;
  private Retractor retractor;

  public PathAndShoot(String pathName) {
    this.sucker = Sucker.getInstance();
    this.retractor = Retractor.getInstance();

    this.addCommands(
        new ParallelRaceGroup(
            AutoBuilder.followPath(PathPlannerPath.fromChoreoTrajectory(pathName)),
            new IntakeNote()),
        new ConditionalCommand(
            new SequentialCommandGroup(new Mate(), new ScoreSpeakerFromDistance()),
            new InstantCommand(
                () -> {
                  retractor.moveToMating();
                }),
            this.sucker::noteDetected));
  }
}
