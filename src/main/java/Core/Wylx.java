package Core;

import Core.Processing.MessageProcessing;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

public class Wylx {
	private static Wylx wylx;

	private final JDA jda;

	private final boolean isRelease;

	public static void main(String[] args) throws LoginException, InterruptedException {
		wylx = new Wylx();
	}

	public static Wylx getInstance() {
		return wylx;
	}

	private Wylx() throws InterruptedException, LoginException {
		MessageProcessing messageProcessor = new MessageProcessing();
		Dotenv env = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		isRelease = Boolean.parseBoolean(env.get("RELEASE"));

		jda = JDABuilder.createDefault(env.get("DISCORD_TOKEN"))
				.setActivity(Activity.of(Activity.ActivityType.PLAYING, "with half a ship"))
				.addEventListeners(messageProcessor)
				.enableIntents(GatewayIntent.GUILD_MEMBERS)
				.build();

		jda.awaitReady();
	}

	public boolean isRelease(){
		return isRelease;
	}

	public long getBotID(){
		return jda.getSelfUser().getIdLong();
	}

	public AudioManager getGuildAudioManager(long guildID) {
		var guild = jda.getGuildById(guildID);
		if (guild == null) return null;
		return guild.getAudioManager();
	}

	public Member getMemberInGuild(long guildID, long userID) {
		var guild = jda.getGuildById(guildID);
		var user = jda.getUserById(userID);
		if (guild == null || user == null) return null;
		return guild.getMember(user);
	}

	public TextChannel getTextChannel(long channelID) {
		return jda.getTextChannelById(channelID);
	}

	@SuppressWarnings("ConstantConditions")
	public boolean userInVoiceChannel(long guildID, long channelID, long userID) {
		var member = getMemberInGuild(guildID, userID);
		if (member == null) return false;
		var voiceState = member.getVoiceState();
		return voiceState != null &&
				voiceState.inAudioChannel() &&
				voiceState.getChannel().getIdLong() == channelID;
	}
}
