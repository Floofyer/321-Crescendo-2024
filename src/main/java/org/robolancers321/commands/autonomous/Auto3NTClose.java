/* (C) Robolancers 2024 */
package org.robolancers321.commands.autonomous;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import org.robolancers321.commands.PathAndShoot;
import org.robolancers321.commands.ScoreSpeakerFixedAuto;
import org.robolancers321.subsystems.drivetrain.Drivetrain;
import org.robolancers321.subsystems.intake.Retractor;
import org.robolancers321.subsystems.intake.Sucker;
import org.robolancers321.subsystems.launcher.Flywheel;
import org.robolancers321.subsystems.launcher.Indexer;
import org.robolancers321.subsystems.launcher.Pivot;

public class Auto3NTClose extends SequentialCommandGroup {
  private Drivetrain drivetrain;
  private Retractor retractor;
  private Sucker sucker;
  private Pivot pivot;
  private Indexer indexer;
  private Flywheel flywheel;

  public Auto3NTClose() {
    this.drivetrain = Drivetrain.getInstance();
    this.retractor = Retractor.getInstance();
    this.sucker = Sucker.getInstance();
    this.pivot = Pivot.getInstance();
    this.indexer = Indexer.getInstance();
    this.flywheel = Flywheel.getInstance();

    this.addCommands(
        // TODO: test this
        new InstantCommand(
            () -> this.drivetrain.setYaw(this.drivetrain.getPose().getRotation().getDegrees())),
        new ScoreSpeakerFixedAuto(),
        new PathAndShoot("3NTClose.1"),
        new PathAndShoot("3NTClose.2"),
        new PathAndShoot("3NTClose.3"));
  }
}
