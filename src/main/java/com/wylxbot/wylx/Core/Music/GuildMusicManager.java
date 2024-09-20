package com.wylxbot.wylx.Core.Music;

import com.wylxbot.wylx.Core.Util.Helper;
import com.wylxbot.wylx.Database.Pojos.DBServer;
import com.wylxbot.wylx.Wylx;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GuildMusicManager extends AudioEventAdapter {
    private boolean loop = false;
    private Timer discTimer;
    private TrackContext lastCtx;
    private final String guildID;
    private final AudioPlayer player;
    private final MusicPlaylist playlist = new MusicPlaylist();

    private static final Logger logger = LoggerFactory.getLogger(GuildMusicManager.class);

    public GuildMusicManager(String guildID, AudioPlayerManager manager) {
        Wylx wylx = Wylx.getInstance();
        this.guildID = guildID;
        player = manager.createPlayer();
        player.addListener(this);

        // Use last saved volume
        DBServer serverEntry = wylx.getDb().getServer(guildID);
        player.setVolume(serverEntry.musicVolume);

        AudioManager audioManager = wylx.getGuildAudioManager(guildID);
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
    }

    public void queuePlaylist(AudioPlaylist newPlaylist) {
        List<AudioTrack> tracks = newPlaylist.getTracks();
        TrackContext ctx = (TrackContext) tracks.get(0).getUserData();
        MessageChannel textChannel = Wylx.getInstance().getTextChannel(ctx.channelID());

        cancelTimer();
        if (MusicUtils.joinVoice(ctx)) {
            return;
        }

        textChannel.sendMessage(String.format("Playlist found: Added %d Songs", tracks.size()))
                .delay(Duration.ofSeconds(60))
                .flatMap(Message::delete)
                .queue();

        // Attempt to play song, this will return false if another song is playing already
        if (player.startTrack(tracks.get(0), true)) {
            tracks.remove(0);
        }

        playlist.queuePlaylist(tracks);
    }

    public void queue(AudioTrack newTrack) {
        TrackContext ctx = (TrackContext) newTrack.getUserData();
        MessageChannel textChannel = Wylx.getInstance().getTextChannel(ctx.channelID());

        cancelTimer();
        if (MusicUtils.joinVoice(ctx)) {
            return;
        }

        // Attempt to play song, this will return false if another song is playing already
        if (player.startTrack(newTrack, true))
            return;

        // Only send if queued instead of played right away
        MessageEmbed embed = MusicUtils.createPlayingEmbed(newTrack, "Queueing **%s**", false);
        textChannel.sendMessageEmbeds(embed)
                .delay(Duration.ofSeconds(60))
                .flatMap(Message::delete)
                .queue();

        playlist.queue(newTrack);
    }

    private void cancelTimer() {
        if (discTimer != null) {
            discTimer.cancel();
            discTimer = null;
        }
    }

    // Called when skipped or last song ended
    private void playNextTrack() {
        if (player.startTrack(playlist.getNextTrack(), false))
            return;

        // Nothing else to play!
        if (lastCtx != null) {
            MessageChannel channel = Wylx.getInstance().getTextChannel(lastCtx.channelID());
            if (channel.canTalk()) {
                channel.sendMessage("Playlist ended. Use the $play command to add more music")
                        .delay(Duration.ofMinutes(1))
                        .flatMap(Message::delete)
                        .queue();
            }
        }

        // Disconnect after a minute of not playing
        discTimer = new Timer();
        discTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                AudioManager audioManager = Wylx.getInstance().getGuildAudioManager(guildID);
                audioManager.closeAudioConnection();
            }
        }, 60000);
    }

    public boolean isNotPlaying() {
        return player.getPlayingTrack() == null;
    }

    public void pause(boolean pause) {
        player.setPaused(pause);
    }

    public Object[] getQueue() {
        return playlist.getQueue();
    }

    public AudioTrack getCurrentTrack() {
        return player.getPlayingTrack();
    }

    public void skip() {
        // TODO: Announce skip

        playNextTrack();
    }

    public void clearQueue() {
        playlist.clear();
        playNextTrack();
    }

    public void stop() {
        loop(false);
        playlist.clear();
        player.stopTrack();

        AudioManager audioManager = Wylx.getInstance().getGuildAudioManager(guildID);
        audioManager.closeAudioConnection();
    }

    public void loop(boolean enable) {
        loop = enable;
    }

    public boolean isLooping() {
        return loop;
    }

    public void setVolume(int vol) {
        player.setVolume(vol);
    }

    public int getVolume() {
        return player.getVolume();
    }

    public void seek(MusicSeek seek) {
        if (!seek.relative()) {
            player.getPlayingTrack().setPosition(seek.dur().toMillis());
            return;
        }

        Duration curLoc = Duration.ofMillis(player.getPlayingTrack().getPosition());
        Duration newLoc = curLoc.plus(seek.dur());
        player.getPlayingTrack().setPosition(newLoc.toMillis());
    }

    // Events from Lavaplayer

    @Override
    public void onPlayerPause(AudioPlayer player) {
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Wylx wylx = Wylx.getInstance();
        lastCtx = (TrackContext) track.getUserData();
        MessageChannel channel = wylx.getTextChannel(lastCtx.channelID());

        track.setPosition(lastCtx.startMillis());

        if (channel == null ||
            !channel.canTalk()) return;

        MessageEmbed embed = MusicUtils.createPlayingEmbed(track, "Playing **%s**", false);
        Helper.selfDestructingMsg(channel.sendMessageEmbeds(embed), Duration.ofMinutes(1));
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (loop) {
                player.startTrack(track.makeClone(), false);
            } else {
                playNextTrack();
            }
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        TrackContext ctx = (TrackContext) track.getUserData();
        MessageChannel textChannel = Wylx.getInstance().getTextChannel(ctx.channelID());
        textChannel.sendMessage("Error: " + exception.getMessage()).queue();
        logger.error("Track ended: {}", exception.getMessage());
    }
}
