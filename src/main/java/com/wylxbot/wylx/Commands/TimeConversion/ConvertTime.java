package com.wylxbot.wylx.Commands.TimeConversion;

import com.wylxbot.wylx.Core.Events.SilentEvents.SilentEvent;
import com.wylxbot.wylx.Database.Pojos.DBUser;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertTime extends SilentEvent {

    private static final String TWELVE_HOUR_REGEX = "(^| )((1[0-2]|0?[1-9])(:[0-5][0-9])?) *[aApP][mM]";
    private static final Pattern TWELVE_HOUR_PATTERN = Pattern.compile(TWELVE_HOUR_REGEX);

    public ConvertTime(){
        super("Automagically detects messages containing a time and converts them to timezones for server users");
    }

    @Override
    public boolean check(MessageReceivedEvent event, String prefix) {
        return TWELVE_HOUR_PATTERN.matcher(event.getMessage().getContentRaw()).find();
    }

    // TODO: Only convert to the timezones that people in the server have
    @Override
    public void runEvent(MessageReceivedEvent event, String prefix) {
        DBUser dbUser = Wylx.getInstance().getDb().getUser(event.getAuthor().getId());
        Timezone userTimezone = Timezone.getTimezone(dbUser.timezone);

        // Cant do conversions if we don't know the user's timezone
        if(userTimezone == null) {
            // Don't prompt a user more than once (spam)
            if(dbUser.timezonePrompted) return;
            // Set it so the user doesn't get prompted multiple times
            dbUser.timezonePrompted = true;
            Wylx.getInstance().getDb().setUser(event.getAuthor().getId(), dbUser);

            event.getMessage().reply("Please set your timezone using " + prefix + "settimezone <timezone>").queue();
            return;
        }

        // Isolate just the time from the rest of the string
        Matcher timeMatcher = TWELVE_HOUR_PATTERN.matcher(event.getMessage().getContentRaw());
        timeMatcher.find();
        String timeString = timeMatcher.group().toLowerCase();
        Time originalTime = new Time(timeString, userTimezone);

        String convertedMessage = "";
        for(Timezone timezone : Timezone.values()){
            convertedMessage += timezone.abrv + ": " + originalTime.convertTo(timezone).toString() + ", ";
        }
        convertedMessage = convertedMessage.substring(0, convertedMessage.length() - 2);

        event.getChannel().sendMessage(convertedMessage).queue();
    }
}
