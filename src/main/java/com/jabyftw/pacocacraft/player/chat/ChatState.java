package com.jabyftw.pacocacraft.player.chat;

/**
 * Copyright (C) 2015  Rafael Sartori for PacocaCraft Plugin
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Email address: rafael.sartori96@gmail.com
 */
public enum ChatState {
    ONLY_SERVER_MESSAGES,
    ONLY_CHAT_MESSAGES, // Server Messages included, just don't receive /msg
    EVERY_MESSAGE;

    public boolean accepts(MessageType messageType) {
        switch(messageType) {
            case SERVER_MESSAGE:
                return true; // always enabled
            case CHAT_MESSAGE:
                return this == EVERY_MESSAGE || this == ONLY_CHAT_MESSAGES;
            case PRIVATE_MESSAGE:
                return this == EVERY_MESSAGE;
        }
        return false;
    }

    public enum MessageType {
        SERVER_MESSAGE,
        CHAT_MESSAGE,
        PRIVATE_MESSAGE
    }
}
