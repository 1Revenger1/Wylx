package com.wylxbot.wylx.Commands.BotUtil;

import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ThreadedCommand;
import com.wylxbot.wylx.Core.Util.EnvUtils;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UpdateCommand extends ThreadedCommand {
    private static final String MAIN_BRANCH = "main";
    private static final String GIT_ERROR = "An error occurred while pulling from Git\n";
    private static final String GIT_SUCCESS = "Pulled from git successfully\n";
    private static final String START_ERROR_OUTPUT = "Error Out:\n```";
    private static final String START_STDOUT_OUTPUT = "Stdout:\n```";
    private static final String END_OUTPUT = "\n```";
    private static final int DISCORD_MAX_LEN = 2000;
    private static final int GIT_OUTPUT_MAX_LEN = DISCORD_MAX_LEN
            - START_ERROR_OUTPUT.length()
            - START_STDOUT_OUTPUT.length()
            - (2 * END_OUTPUT.length());

    UpdateCommand() {
        super ("update", CommandPermission.BOT_ADMIN, "Update and build");
    }

    @Override
    public void runCommandThread(CommandContext ctx) {
        MessageReceivedEvent event = ctx.event();
        String[] args = ctx.args();

        if (args.length == 2 && Wylx.getInstance().getWylxConfig().release) {
            event.getChannel().sendMessage("Unable to use different branch on RELEASE").queue();
            return;
        }

        String branch = args.length == 2 ? args[1] : MAIN_BRANCH;
        if (!updateGit(event, branch)) return;
        runGradlewBuild(event);
        runGradlewInstall(event);
    }

    private boolean updateGit(MessageReceivedEvent event, String branch) {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"git", "pull", "origin", branch};

        try {
            Process proc = rt.exec(commands);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader errInput = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            int exitCode = proc.waitFor();

            // Read output from git pull
            StringBuilder stdOuputBuilder = new StringBuilder();
            while (stdInput.ready()) {
                stdOuputBuilder.append(stdInput.readLine()).append("\n");
            }
            String stdOutput = stdOuputBuilder.toString();

            StringBuilder stdErrorBuilder = new StringBuilder();
            while (errInput.ready()) {
                stdErrorBuilder.append(errInput.readLine()).append("\n");
            }
            String stdError = stdErrorBuilder.toString();

            // Build message for discord
            StringBuilder msgString = new StringBuilder(exitCode == 0 ? GIT_SUCCESS : GIT_ERROR);

            if (stdOutput.length() + stdError.length() < (GIT_OUTPUT_MAX_LEN - msgString.length())) {
                // Send a message with git output if it'll fit
                if (stdError.length() > 0)
                    msgString.append(START_ERROR_OUTPUT).append(stdErrorBuilder).append(END_OUTPUT);
                if (stdOutput.length() > 0)
                    msgString.append(START_STDOUT_OUTPUT).append(stdOuputBuilder).append(END_OUTPUT);

                event.getChannel().sendMessage(msgString).queue();
            } else {
                // Otherwise send files containing git output
                FileUpload stdoutFile = FileUpload.fromData(stdOuputBuilder.toString().getBytes(), "Stdout.txt");
                FileUpload stderrFile = FileUpload.fromData(stdErrorBuilder.toString().getBytes(), "Stderr.txt");
                event.getChannel().sendFiles(stdoutFile, stderrFile)
                        .setContent(msgString.toString()).queue();

            }

            stdInput.close();
            errInput.close();
            return proc.exitValue() == 0;
        } catch (Exception e) {
            event.getChannel().sendMessage("Exception while running git\n" + e.getMessage()).queue();
            return false;
        }
    }

    private void runGradlewBuild(MessageReceivedEvent event) {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"./gradlew", "build"};

        try {
            Process proc = rt.exec(commands);

            if (proc.waitFor() != 0) {
                event.getChannel().sendMessage("An error occurred while building").queue();
            } else {
                event.getChannel().sendMessage("Gradlew built Wylx").queue();
            }

        } catch (Exception e) {
            event.getChannel().sendMessage("Exception while running gradlew\n" + e.getMessage()).queue();
        }
    }

    private void runGradlewInstall(MessageReceivedEvent event) {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"./gradlew", "installdist"};

        try {
            Process proc = rt.exec(commands);

            if (proc.waitFor() != 0) {
                event.getChannel().sendMessage("An error occurred while running installDist").queue();
            } else {
                event.getChannel().sendMessage("Gradlew finished running installDist - Ready for restart!").queue();
            }

        } catch (Exception e) {

            event.getChannel().sendMessage("Exception while running gradlew\n" + e.getMessage()).queue();
        }
    }
}
