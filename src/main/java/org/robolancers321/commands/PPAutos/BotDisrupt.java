/* (C) Robolancers 2024 */
package org.robolancers321.commands.PPAutos;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import org.robolancers321.commands.ScoreSpeakerFixedAuto;
import org.robolancers321.subsystems.drivetrain.Drivetrain;

public class BotDisrupt extends SequentialCommandGroup {
  private Drivetrain drivetrain;

  public BotDisrupt() {
    this.drivetrain = Drivetrain.getInstance();

    this.addCommands(
      Drivetrain.getInstance().zeroToPath(PathPlannerPath.fromPathFile("BotDisrupt")),
        new ScoreSpeakerFixedAuto(),
        AutoBuilder.followPath(PathPlannerPath.fromPathFile("BotDisrupt")));
  }
}
