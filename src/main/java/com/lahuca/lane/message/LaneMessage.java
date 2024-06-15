package com.lahuca.lane.message;

import java.util.Locale;

/**
 * @author _Neko1
 * @date 18.03.2024
 **/

public interface LaneMessage {

    String retrieveMessage(String messageId, Locale locale);

}