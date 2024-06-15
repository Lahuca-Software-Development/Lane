package com.lahuca.lane.message;

import java.util.Locale;
import java.util.Map;

/**
 * @author _Neko1
 * @date 18.03.2024
 **/

public class MapLaneMessage implements LaneMessage {

    private final Map<String, Map<String, String>> messages;

    public MapLaneMessage(Map<String, Map<String, String>> messages) {
        this.messages = messages;
    }

    @Override
    public String retrieveMessage(String messageId, Locale locale) {
        Map<String, String> data = messages.get(locale.toLanguageTag());
        return data != null ? data.get(messageId) : null;
    }
}
