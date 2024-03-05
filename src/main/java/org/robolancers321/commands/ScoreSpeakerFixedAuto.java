/* (C) Robolancers 2024 */
package org.robolancers321.commands;

import edu.wpi.first.wpilibj2.command.ParallelRaceGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import org.robolancers321.subsystems.intake.Retractor;
import org.robolancers321.subsystems.intake.Sucker;
import org.robolancers321.subsystems.launcher.Flywheel;
import org.robolancers321.subsystems.launcher.Indexer;
import org.robolancers321.subsystems.launcher.Pivot;

public class ScoreSpeakerFixedAuto extends SequentialCommandGroup {
  private Retractor retractor;
  private Pivot pivot;
  private Indexer indexer;
  private Flywheel flywheel;
  private Sucker sucker;

  // assumes arms are at mating with note inside sucker
  public ScoreSpeakerFixedAuto() {
    this.retractor = Retractor.getInstance();
    this.pivot = Pivot.getInstance();
    this.flywheel = Flywheel.getInstance();
    this.indexer = Indexer.getInstance();
    this.sucker = Sucker.getInstance();

    this.addCommands(
        this.flywheel.revSpeaker(),
        new ParallelRaceGroup(
            this.indexer.acceptHandoff().andThen(this.indexer.outtake()), this.sucker.out()),
        this.flywheel.off());
  }
}
