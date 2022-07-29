package us.hebi.sass;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * @author Florian Enner
 * @since 30 Jul 2022
 */
@Mojo(name = "watch")
@Execute(phase = LifecyclePhase.GENERATE_RESOURCES)
public class WatchMojo extends RunMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Enabling watch mode");
        args.add("--watch");
        super.execute();
    }

}
